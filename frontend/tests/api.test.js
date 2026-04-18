const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const vm = require('node:vm');

const apiSource = fs.readFileSync(path.join(__dirname, '..', 'api.js'), 'utf8');

function createStorage() {
  const data = new Map();
  return {
    getItem: key => data.has(key) ? data.get(key) : null,
    setItem: (key, value) => data.set(key, String(value)),
    removeItem: key => data.delete(key),
    clear: () => data.clear(),
  };
}

function response(status, body) {
  return {
    status,
    ok: status >= 200 && status < 300,
    text: async () => body == null ? '' : JSON.stringify(body),
    json: async () => body || {},
  };
}

function loadApi({ location, fetchImpl, localStorage = createStorage(), sessionStorage = createStorage() } = {}) {
  const window = {
    location: location || {
      protocol: 'http:',
      hostname: 'localhost',
      port: '5500',
      origin: 'http://localhost:5500',
      pathname: '/frontend/aed-studio.html',
      search: '',
    },
  };
  const context = {
    window,
    document: { querySelector: () => null },
    localStorage,
    sessionStorage,
    fetch: fetchImpl || (async () => response(200, {})),
    AbortController,
    URLSearchParams,
    setTimeout,
    clearTimeout,
    console,
  };
  vm.createContext(context);
  vm.runInContext(apiSource, context);
  return { api: context.window.AedApi, window, localStorage, sessionStorage };
}

test('resolve a base da API a partir do host mobile/local', () => {
  const { api } = loadApi({
    location: {
      protocol: 'http:',
      hostname: '192.168.0.25',
      port: '5500',
      origin: 'http://192.168.0.25:5500',
      pathname: '/frontend/aed-studio.html',
      search: '',
    },
  });
  assert.equal(api.getBase(), 'http://192.168.0.25:8080');
});

test('permite configurar a base da API por query string', () => {
  const { api } = loadApi({
    location: {
      protocol: 'file:',
      hostname: '',
      port: '',
      origin: 'null',
      pathname: '/frontend/aed-studio.html',
      search: '?apiBase=http://10.0.0.8:8080',
    },
  });
  assert.equal(api.getBase(), 'http://10.0.0.8:8080');
});

test('login persiste tokens sem credentials/include', async () => {
  const calls = [];
  const { api, sessionStorage } = loadApi({
    fetchImpl: async (url, opts) => {
      calls.push({ url, opts });
      return response(200, {
        accessToken: 'access-1',
        refreshToken: 'refresh-1',
        user: { email: 'user@aed.test' },
      });
    },
  });

  const data = await api.login('user@aed.test', 'Senha123!');

  assert.equal(data.accessToken, 'access-1');
  assert.equal(sessionStorage.getItem('aed_access_token'), 'access-1');
  assert.equal(calls[0].url, 'http://localhost:8080/api/auth/login');
  assert.equal(calls[0].opts.credentials, undefined);
});

test('health usa endpoint público sem Authorization', async () => {
  const calls = [];
  const { api, sessionStorage } = loadApi({
    fetchImpl: async (url, opts) => {
      calls.push({ url, opts });
      return response(200, { status: 'UP' });
    },
  });
  sessionStorage.setItem('aed_access_token', 'access-1');

  const health = await api.health();

  assert.equal(health.status, 'UP');
  assert.equal(calls[0].url, 'http://localhost:8080/api/health');
  assert.equal(calls[0].opts.headers.Authorization, undefined);
});

test('401 em rota protegida faz refresh e repete a requisição com token novo', async () => {
  const calls = [];
  const { api, sessionStorage } = loadApi({
    fetchImpl: async (url, opts) => {
      calls.push({ url, opts });
      if (url.endsWith('/api/auth/me') && calls.length === 1) return response(401, { message: 'expired' });
      if (url.endsWith('/api/auth/refresh')) {
        return response(200, {
          accessToken: 'access-2',
          refreshToken: 'refresh-2',
          user: { email: 'user@aed.test' },
        });
      }
      return response(200, { email: 'user@aed.test' });
    },
  });
  sessionStorage.setItem('aed_access_token', 'access-1');
  sessionStorage.setItem('aed_refresh_token', 'refresh-1');

  const user = await api.me();

  assert.equal(user.email, 'user@aed.test');
  assert.equal(sessionStorage.getItem('aed_access_token'), 'access-2');
  assert.equal(calls.length, 3);
  assert.equal(calls[2].opts.headers.Authorization, 'Bearer access-2');
});

test('busca exercícios e envia tentativa pelo contrato de aprendizagem ativa', async () => {
  const calls = [];
  const { api, sessionStorage } = loadApi({
    fetchImpl: async (url, opts) => {
      calls.push({ url, opts });
      if (url.endsWith('/api/learning/topics/algoritmos/exercises')) {
        return response(200, [{ id: 'algoritmos-check', topicId: 'algoritmos' }]);
      }
      return response(200, { correct: true, awarded: 35 });
    },
  });
  sessionStorage.setItem('aed_access_token', 'access-1');

  const exercises = await api.getExercises('algoritmos');
  const result = await api.submitExercise('algoritmos-check', 'B', 18);

  assert.equal(exercises[0].id, 'algoritmos-check');
  assert.equal(result.correct, true);
  assert.equal(calls[0].opts.headers.Authorization, 'Bearer access-1');
  assert.equal(calls[1].url, 'http://localhost:8080/api/learning/attempts');
  assert.equal(JSON.parse(calls[1].opts.body).timeSpentSeconds, 18);
});

test('usa contratos avançados para simuladores, recomendações, analytics e exercícios gerados', async () => {
  const calls = [];
  const { api, sessionStorage } = loadApi({
    fetchImpl: async (url, opts) => {
      calls.push({ url, opts });
      if (url.endsWith('/api/learning/generated-exercises')) {
        return response(200, { id: 'gen-1-algoritmos-1', topicId: 'algoritmos' });
      }
      if (url.endsWith('/api/recommendations')) {
        return response(200, { primary: { topicId: 'algoritmos' } });
      }
      if (url.endsWith('/api/analytics/overview')) {
        return response(200, { totalAttempts: 1 });
      }
      if (url.endsWith('/api/analytics/topics')) {
        return response(200, [{ topicId: 'algoritmos' }]);
      }
      if (url.endsWith('/api/analytics/trails')) {
        return response(200, [{ trackId: 'fundamentos' }]);
      }
      if (url.endsWith('/api/learning/generated-exercises/history')) {
        return response(200, [{ id: 'gen-1-algoritmos-1' }]);
      }
      return response(200, { awarded: 8, correct: true });
    },
  });
  sessionStorage.setItem('aed_access_token', 'access-1');

  const generated = await api.generateExercise('algoritmos', 2);
  await api.submitGeneratedExercise(generated.id, 'A', 12);
  await api.recordSimulationEvent('algoritmos', 'ARRAY', 'insert', 'FIRST_RUN', '[3]');
  const recs = await api.getRecommendations();
  const overview = await api.getAnalyticsOverview();
  const topics = await api.getTopicAnalytics();
  const trails = await api.getTrailAnalytics();
  const history = await api.getGeneratedExerciseHistory();

  assert.equal(generated.id, 'gen-1-algoritmos-1');
  assert.equal(recs.primary.topicId, 'algoritmos');
  assert.equal(overview.totalAttempts, 1);
  assert.equal(topics[0].topicId, 'algoritmos');
  assert.equal(trails[0].trackId, 'fundamentos');
  assert.equal(history[0].id, 'gen-1-algoritmos-1');
  assert.equal(calls[0].url, 'http://localhost:8080/api/learning/generated-exercises');
  assert.equal(JSON.parse(calls[0].opts.body).difficulty, 2);
  assert.equal(calls[2].url, 'http://localhost:8080/api/simulations/events');
  assert.equal(JSON.parse(calls[2].opts.body).milestone, 'FIRST_RUN');
});

test('usa contratos de missões, histórico de XP e sandbox de código', async () => {
  const calls = [];
  const { api, sessionStorage } = loadApi({
    fetchImpl: async (url, opts) => {
      calls.push({ url, opts });
      if (url.endsWith('/api/simulations/topics/arrays/missions')) {
        return response(200, [{ id: 'arrays-map-3', completed: false }]);
      }
      if (url.endsWith('/api/simulations/missions/arrays-map-3/submit')) {
        return response(200, { completed: true, awarded: 18 });
      }
      if (url.endsWith('/api/analytics/xp-history')) {
        return response(200, [{ date: '2026-04-18', xp: 30, cumulativeXp: 30 }]);
      }
      if (url.endsWith('/api/code/topics/algoritmos/challenges')) {
        return response(200, [{ id: 'algoritmos-code-sum' }]);
      }
      return response(200, { accepted: true, awarded: 30 });
    },
  });
  sessionStorage.setItem('aed_access_token', 'access-1');

  const missions = await api.getSimulatorMissions('arrays');
  const mission = await api.submitSimulatorMission('arrays-map-3', '{"values":[3,6,9]}');
  const history = await api.getXpHistory();
  const challenges = await api.getCodeChallenges('algoritmos');
  const run = await api.runCode('algoritmos-code-sum', 'return 0;');

  assert.equal(missions[0].id, 'arrays-map-3');
  assert.equal(mission.completed, true);
  assert.equal(history[0].cumulativeXp, 30);
  assert.equal(challenges[0].id, 'algoritmos-code-sum');
  assert.equal(run.accepted, true);
  assert.equal(calls[1].url, 'http://localhost:8080/api/simulations/missions/arrays-map-3/submit');
  assert.equal(JSON.parse(calls[1].opts.body).stateSnapshot, '{"values":[3,6,9]}');
  assert.equal(calls[4].url, 'http://localhost:8080/api/code/run');
});
