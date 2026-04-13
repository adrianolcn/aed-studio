/**
 * AED·Studio — API Client
 *
 * Centraliza todas as chamadas ao back-end.
 * Funcionalidades:
 *   - Adiciona automaticamente Authorization: Bearer <token>
 *   - Intercepta 401 e tenta renovar o access token via refresh
 *   - Redireciona para login se o refresh também expirar
 *   - Sincroniza progresso (visitas, XP) com o back-end
 */

const AedApi = (() => {

  const BASE = 'http://localhost:8080'; // ajuste para o seu servidor

  // ── Token storage ──────────────────────────────────────────────
  // sessionStorage: limpo ao fechar o browser (mais seguro para JWTs)
  const storage = {
    getAccess:   () => sessionStorage.getItem('aed_access_token'),
    getRefresh:  () => sessionStorage.getItem('aed_refresh_token'),
    getUser:     () => JSON.parse(sessionStorage.getItem('aed_user') || 'null'),
    setTokens:   (access, refresh) => {
      sessionStorage.setItem('aed_access_token', access);
      sessionStorage.setItem('aed_refresh_token', refresh);
    },
    setUser:     (u) => sessionStorage.setItem('aed_user', JSON.stringify(u)),
    clear:       () => {
      sessionStorage.removeItem('aed_access_token');
      sessionStorage.removeItem('aed_refresh_token');
      sessionStorage.removeItem('aed_user');
    }
  };

  // ── Core fetch com retry de refresh ───────────────────────────
  let refreshPromise = null; // evita múltiplos refreshes simultâneos

  async function request(path, opts = {}, retry = true) {
    const token = storage.getAccess();
    const headers = {
      'Content-Type': 'application/json',
      ...(token ? { 'Authorization': 'Bearer ' + token } : {}),
      ...opts.headers,
    };

    const res = await fetch(BASE + path, {
      ...opts,
      headers,
      credentials: 'include', // envia cookie de sessão também
    });

    // Se 401 e temos refresh token, tenta renovar uma vez
    if (res.status === 401 && retry) {
      const refreshToken = storage.getRefresh();
      if (!refreshToken) { redirectToLogin(); return; }

      // Serializa tentativas de refresh simultâneas
      if (!refreshPromise) {
        refreshPromise = doRefresh(refreshToken).finally(() => {
          refreshPromise = null;
        });
      }

      const refreshed = await refreshPromise;
      if (!refreshed) { redirectToLogin(); return; }

      // Retry com novo token
      return request(path, opts, false);
    }

    return res;
  }

  async function doRefresh(refreshToken) {
    try {
      const res = await fetch(BASE + '/api/auth/refresh', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken }),
        credentials: 'include',
      });
      if (!res.ok) return false;
      const data = await res.json();
      storage.setTokens(data.accessToken, data.refreshToken);
      storage.setUser(data.user);
      return true;
    } catch {
      return false;
    }
  }

  function redirectToLogin() {
    storage.clear();
    window.location.href = 'login.html';
  }

  // ── API methods ────────────────────────────────────────────────

  async function get(path) {
    const res = await request(path);
    if (!res) return null;
    return res.ok ? res.json() : handleError(res);
  }

  async function post(path, body) {
    const res = await request(path, {
      method: 'POST',
      body: JSON.stringify(body),
    });
    if (!res) return null;
    return res.ok ? res.json() : handleError(res);
  }

  async function handleError(res) {
    const data = await res.json().catch(() => ({}));
    console.warn(`API ${res.status}:`, data.message || 'Erro desconhecido');
    return null;
  }

  // ── Auth ───────────────────────────────────────────────────────

  async function logout() {
    try {
      await request('/api/auth/logout', { method: 'POST' }, false);
      await request('/api/auth/logout-web', { method: 'POST' }, false);
    } finally {
      storage.clear();
      window.location.href = 'login.html';
    }
  }

  async function me() {
    return get('/api/auth/me');
  }

  // ── Progresso ──────────────────────────────────────────────────

  async function getProgress() {
    return get('/api/progress');
  }

  /**
   * Registra visita a um tópico. Fire-and-forget — não bloqueia UI.
   */
  function recordVisit(topicId) {
    post('/api/progress/visit', { topicId })
      .catch(e => console.warn('recordVisit error:', e));
  }

  /**
   * Concede XP. Idempotente — o back-end rejeita duplicatas.
   * Retorna o XP efetivamente concedido (0 se já havia sido ganho).
   */
  async function awardXp(topicId, reason, amount) {
    const data = await post('/api/progress/xp', { topicId, reason, amount });
    return data ? data.awarded : 0;
  }

  // ── Verificação de sessão ──────────────────────────────────────

  /**
   * Verifica se o usuário está autenticado.
   * Deve ser chamado no início do aed-studio.html.
   * Se não autenticado, redireciona para login.
   */
  async function requireAuth() {
    const token = storage.getAccess();
    if (!token) { redirectToLogin(); return null; }

    const user = await me();
    if (!user) { redirectToLogin(); return null; }

    storage.setUser(user);
    return user;
  }

  // ── Utilitários ────────────────────────────────────────────────

  function isAuthenticated() {
    return !!storage.getAccess();
  }

  function getCurrentUser() {
    return storage.getUser();
  }

  // ── Interface pública ──────────────────────────────────────────
  return {
    // Auth
    logout,
    me,
    requireAuth,
    isAuthenticated,
    getCurrentUser,
    // Progresso
    getProgress,
    recordVisit,
    awardXp,
  };

})();

// Disponível globalmente como AedApi
window.AedApi = AedApi;
