/**
 * AED·Studio — cliente HTTP central.
 *
 * Fonte única para autenticação JWT, refresh token, health check,
 * catálogo de tópicos e progresso persistido no servidor.
 */
const AedApi = (() => {
  const ACCESS_KEY = 'aed_access_token';
  const REFRESH_KEY = 'aed_refresh_token';
  const USER_KEY = 'aed_user';
  const API_BASE_KEY = 'aed_api_base';

  let apiBase = normalizeBase(resolveInitialBase());
  let refreshPromise = null;
  let lastError = null;

  const storage = {
    getAccess: () => sessionStorage.getItem(ACCESS_KEY),
    getRefresh: () => sessionStorage.getItem(REFRESH_KEY),
    getUser: () => safeJson(sessionStorage.getItem(USER_KEY)),
    setTokens: (access, refresh) => {
      if (access) sessionStorage.setItem(ACCESS_KEY, access);
      if (refresh) sessionStorage.setItem(REFRESH_KEY, refresh);
    },
    setUser: user => sessionStorage.setItem(USER_KEY, JSON.stringify(user)),
    clear: () => {
      sessionStorage.removeItem(ACCESS_KEY);
      sessionStorage.removeItem(REFRESH_KEY);
      sessionStorage.removeItem(USER_KEY);
    },
  };

  function safeJson(raw) {
    try { return raw ? JSON.parse(raw) : null; } catch { return null; }
  }

  function normalizeBase(base) {
    return String(base || '').replace(/\/+$/, '');
  }

  function resolveInitialBase() {
    const win = window || {};
    const params = new URLSearchParams(win.location?.search || '');
    const meta = document.querySelector('meta[name="aed-api-base"]')?.content;
    const configured =
      win.AED_API_BASE ||
      params.get('apiBase') ||
      meta ||
      localStorage.getItem(API_BASE_KEY);
    if (configured) return configured;

    const loc = win.location || {};
    if (loc.protocol === 'file:') return 'http://localhost:8080';
    if (loc.hostname && loc.port && loc.port !== '8080') {
      return `${loc.protocol}//${loc.hostname}:8080`;
    }
    return loc.origin || 'http://localhost:8080';
  }

  function setBase(base, persist = true) {
    apiBase = normalizeBase(base);
    if (persist) localStorage.setItem(API_BASE_KEY, apiBase);
    return apiBase;
  }

  function getBase() {
    return apiBase;
  }

  async function request(path, opts = {}) {
    const {
      auth = true,
      retry = true,
      redirectOnUnauthorized = true,
      timeoutMs = 8000,
      ...fetchOpts
    } = opts;

    const headers = {
      'Content-Type': 'application/json',
      ...(fetchOpts.headers || {}),
    };

    const token = storage.getAccess();
    if (auth && token) headers.Authorization = `Bearer ${token}`;

    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), timeoutMs);
    let res;
    try {
      res = await fetch(apiBase + path, {
        ...fetchOpts,
        headers,
        signal: fetchOpts.signal || controller.signal,
      });
    } catch (err) {
      lastError = {
        status: 0,
        message: err.name === 'AbortError' ? 'Tempo limite excedido ao chamar a API.' : 'Falha de rede ao chamar a API.',
        path,
      };
      return null;
    } finally {
      clearTimeout(timeout);
    }

    if (res.status === 401 && auth && retry) {
      const refreshed = await refreshAccessToken();
      if (refreshed) {
        return request(path, { ...opts, retry: false });
      }
      if (redirectOnUnauthorized) redirectToLogin('expired');
      return null;
    }

    return res;
  }

  async function refreshAccessToken() {
    const refreshToken = storage.getRefresh();
    if (!refreshToken) return false;

    if (!refreshPromise) {
      refreshPromise = (async () => {
        const res = await request('/api/auth/refresh', {
          method: 'POST',
          auth: false,
          retry: false,
          redirectOnUnauthorized: false,
          body: JSON.stringify({ refreshToken }),
        });
        if (!res?.ok) return false;
        const data = await res.json();
        persistAuth(data);
        return true;
      })().finally(() => { refreshPromise = null; });
    }

    return refreshPromise;
  }

  async function parseOrNull(res) {
    if (!res) return null;
    if (res.status === 204) return {};
    const text = await res.text();
    if (!text) return {};
    try { return JSON.parse(text); } catch { return { message: text }; }
  }

  async function json(path, opts = {}) {
    const res = await request(path, opts);
    const data = await parseOrNull(res);
    if (!res?.ok) {
      lastError = {
        status: res?.status || 0,
        message: data?.message || data?.error || 'Erro inesperado na API.',
        path,
      };
      return null;
    }
    lastError = null;
    return data;
  }

  function persistAuth(data) {
    storage.setTokens(data.accessToken, data.refreshToken);
    storage.setUser(data.user);
  }

  function redirectToLogin(reason = 'auth') {
    storage.clear();
    const target = `login.html?reason=${encodeURIComponent(reason)}`;
    if (!window.location.pathname.endsWith('/login.html')) {
      window.location.href = target;
    }
  }

  async function login(email, password) {
    const data = await json('/api/auth/login', {
      method: 'POST',
      auth: false,
      redirectOnUnauthorized: false,
      body: JSON.stringify({ email, password }),
    });
    if (data) persistAuth(data);
    return data;
  }

  async function register(username, email, password, fullName) {
    const data = await json('/api/auth/register', {
      method: 'POST',
      auth: false,
      redirectOnUnauthorized: false,
      body: JSON.stringify({ username, email, password, fullName }),
    });
    if (data) persistAuth(data);
    return data;
  }

  async function logout() {
    try {
      await request('/api/auth/logout', { method: 'POST', retry: false, redirectOnUnauthorized: false });
    } finally {
      storage.clear();
      window.location.href = 'login.html?reason=logout';
    }
  }

  async function me(options = {}) {
    const user = await json('/api/auth/me', options);
    if (user) storage.setUser(user);
    return user;
  }

  async function requireAuth() {
    if (!storage.getAccess()) {
      redirectToLogin('missing-token');
      return null;
    }
    const user = await me({ redirectOnUnauthorized: false });
    if (!user) {
      redirectToLogin('invalid-token');
      return null;
    }
    return user;
  }

  async function health() {
    return json('/api/health', { auth: false, redirectOnUnauthorized: false, timeoutMs: 3000 });
  }

  async function getTopicCatalog() {
    return json('/api/catalog/topics', { auth: false, redirectOnUnauthorized: false });
  }

  async function getProgress() {
    return json('/api/progress');
  }

  async function recordVisit(topicId) {
    return json('/api/progress/visit', {
      method: 'POST',
      body: JSON.stringify({ topicId }),
    });
  }

  async function awardXp(topicId, reason, amount) {
    return json('/api/progress/xp', {
      method: 'POST',
      body: JSON.stringify({ topicId, reason, amount }),
    });
  }

  async function getExercises(topicId) {
    return json(`/api/learning/topics/${encodeURIComponent(topicId)}/exercises`);
  }

  async function submitExercise(exerciseId, answer, timeSpentSeconds = 0) {
    return json('/api/learning/attempts', {
      method: 'POST',
      body: JSON.stringify({ exerciseId, answer, timeSpentSeconds }),
    });
  }

  async function generateExercise(topicId, difficulty = 1) {
    return json('/api/learning/generated-exercises', {
      method: 'POST',
      body: JSON.stringify({ topicId, difficulty }),
    });
  }

  async function getGeneratedExerciseHistory() {
    return json('/api/learning/generated-exercises/history');
  }

  async function submitGeneratedExercise(generatedExerciseId, answer, timeSpentSeconds = 0) {
    return json(`/api/learning/generated-exercises/${encodeURIComponent(generatedExerciseId)}/attempts`, {
      method: 'POST',
      body: JSON.stringify({ answer, timeSpentSeconds }),
    });
  }

  async function recordSimulationEvent(topicId, simulatorType, action, milestone = null, stateSnapshot = null) {
    return json('/api/simulations/events', {
      method: 'POST',
      body: JSON.stringify({ topicId, simulatorType, action, milestone, stateSnapshot }),
    });
  }

  async function getSimulatorMissions(topicId) {
    return json(`/api/simulations/topics/${encodeURIComponent(topicId)}/missions`);
  }

  async function submitSimulatorMission(missionId, stateSnapshot) {
    return json(`/api/simulations/missions/${encodeURIComponent(missionId)}/submit`, {
      method: 'POST',
      body: JSON.stringify({ stateSnapshot }),
    });
  }

  async function getRecommendations() {
    return json('/api/recommendations');
  }

  async function getAnalyticsOverview() {
    return json('/api/analytics/overview');
  }

  async function getTopicAnalytics() {
    return json('/api/analytics/topics');
  }

  async function getTrailAnalytics() {
    return json('/api/analytics/trails');
  }

  async function getXpHistory() {
    return json('/api/analytics/xp-history');
  }

  async function getCodeChallenges(topicId) {
    return json(`/api/code/topics/${encodeURIComponent(topicId)}/challenges`);
  }

  async function runCode(challengeId, code) {
    return json('/api/code/run', {
      method: 'POST',
      body: JSON.stringify({ challengeId, code }),
    });
  }

  function isAuthenticated() {
    return !!storage.getAccess();
  }

  function getCurrentUser() {
    return storage.getUser();
  }

  function getLastError() {
    return lastError;
  }

  return {
    setBase,
    getBase,
    login,
    register,
    logout,
    me,
    requireAuth,
    health,
    getTopicCatalog,
    getProgress,
    recordVisit,
    awardXp,
    getExercises,
    submitExercise,
    generateExercise,
    getGeneratedExerciseHistory,
    submitGeneratedExercise,
    recordSimulationEvent,
    getSimulatorMissions,
    submitSimulatorMission,
    getRecommendations,
    getAnalyticsOverview,
    getTopicAnalytics,
    getTrailAnalytics,
    getXpHistory,
    getCodeChallenges,
    runCode,
    refreshAccessToken,
    isAuthenticated,
    getCurrentUser,
    getLastError,
    clearSession: storage.clear,
  };
})();

window.AedApi = AedApi;
