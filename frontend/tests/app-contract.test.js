const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');

const root = path.join(__dirname, '..');
const appHtml = fs.readFileSync(path.join(root, 'aed-studio.html'), 'utf8');
const loginHtml = fs.readFileSync(path.join(root, 'login.html'), 'utf8');
const apiJs = fs.readFileSync(path.join(root, 'api.js'), 'utf8');
const moduleIndexJs = fs.readFileSync(path.join(root, 'js', 'index.js'), 'utf8');

test('tela principal carrega o cliente central e exige autenticação', () => {
  assert.match(appHtml, /<script src="api\.js"><\/script>/);
  assert.match(appHtml, /AedApi\.requireAuth\(\)/);
  assert.match(appHtml, /document\.addEventListener\('DOMContentLoaded', bootstrapApp\)/);
});

test('progresso, visitas e XP da tela principal usam a API', () => {
  assert.match(appHtml, /AedApi\.getProgress\(\)/);
  assert.match(appHtml, /AedApi\.recordVisit\(pageId\)/);
  assert.match(appHtml, /AedApi\.awardXp\(topicId, label, amount\)/);
  assert.match(appHtml, /serverProgress\.topicStates/);
  assert.match(appHtml, /state === 'LOCKED'/);
  assert.doesNotMatch(appHtml, /localStorage\.setItem\('aed_streak'/);
  assert.doesNotMatch(appHtml, /quiz_auto_'\+Date\.now/);
});

test('dashboard educacional e exercícios são renderizados a partir da API', () => {
  assert.match(appHtml, /id="student-dashboard"/);
  assert.match(appHtml, /id="next-topic-list"/);
  assert.match(appHtml, /function renderDashboard\(\)/);
  assert.match(appHtml, /async function renderExercisePanel\(pageId\)/);
  assert.match(appHtml, /AedApi\.getExercises\(pageId\)/);
  assert.match(appHtml, /AedApi\.submitExercise\(ex\.id, btn\.dataset\.answer, elapsed\)/);
});

test('prática fica centralizada em Exercícios e navegação volta ao topo', () => {
  assert.match(appHtml, /id="page-exercicios"/);
  assert.match(appHtml, /id="practice-topic-select"/);
  assert.match(appHtml, /id="practice-dynamic-host"/);
  assert.match(appHtml, /id="practice-code-host"/);
  assert.match(appHtml, /\.expedition-grid \{ display: grid; grid-template-columns: 1fr 1fr; gap: 18px; margin: 18px 0 32px;/);
  assert.match(appHtml, /class="home-callout"/);
  assert.match(appHtml, /id="asymptotic-tool"/);
  assert.match(appHtml, /id="sorting-arena-tool"/);
  assert.match(appHtml, /rota pedagógica recomendada/);
  assert.match(appHtml, /modelo mental antes da manipulação/);
  assert.match(appHtml, /async function renderPracticeWorkspace\(preferredTopicId\)/);
  assert.match(appHtml, /function analyzeAsymptoticTool\(\)/);
  assert.match(appHtml, /function startSortingArena\(\)/);
  assert.match(appHtml, /function buildArenaFrames\(base, algo\)/);
  assert.match(appHtml, /function scrollMainToTop\(\)/);
  assert.match(appHtml, /function insertLearningPanel\(page, panel\) \{\s*page\.appendChild\(panel\);\s*\}/);
  assert.match(appHtml, /if\(pageId === 'exercicios'\) renderPracticeWorkspace\(\);[\s\S]*else renderSimulatorPanel\(pageId\);/);
  assert.match(appHtml, /content\.scrollTo\(\{ top: 0, left: 0, behavior: 'auto' \}\)/);
  assert.ok(appHtml.indexOf('id="ni-exercicios"') > appHtml.indexOf('id="grp-paradigmas"'));
  assert.ok(appHtml.indexOf('id="asymptotic-tool"') < appHtml.indexOf('id="practice-required-host"'));
});

test('frontend possui camada modular incremental sem migrar de framework', () => {
  assert.match(appHtml, /<script type="module" src="js\/index.js"><\/script>/);
  assert.match(loginHtml, /<script type="module" src="js\/index.js"><\/script>/);
  assert.match(moduleIndexJs, /window\.AedStudioModules/);
  assert.match(moduleIndexJs, /simulatorTypes/);
  assert.match(moduleIndexJs, /sandboxModes/);
});

test('explicações didáticas cobrem assintótica, ordenação e Dijkstra', () => {
  assert.match(appHtml, /ASYMPTOTIC_EXAMPLES/);
  assert.match(appHtml, /O\(n log n\)/);
  assert.match(appHtml, /O\(\(V \+ E\) log V\)/);
  assert.match(appHtml, /Como chegamos em/);
  assert.match(appHtml, /id="dijk-explain"/);
  assert.match(appHtml, /function dijkExplainHtml\(step\)/);
  assert.match(appHtml, /distância até/);
  assert.match(appHtml, /arenaScore\(run\)/);
});

test('fundamentos têm narrativa comparativa e ganchos pedagógicos', () => {
  assert.match(appHtml, /algoritmo não é só receita/);
  assert.match(appHtml, /Heurística:/);
  assert.match(appHtml, /Programa:/);
  assert.match(appHtml, /qual algoritmo é o melhor\?/);
  assert.match(appHtml, /tad, estrutura concreta e classe não são a mesma coisa/);
  assert.match(appHtml, /O TAD é a promessa pública/);
  assert.match(appHtml, /Use array quando:/);
  assert.match(appHtml, /Cartões comparativos: leia lado a lado, não como etapas/);
  assert.match(appHtml, /<div class="insight-icon">💡<\/div>/);
  assert.match(appHtml, /<div class="insight-title">Próximo passo<\/div>/);
  assert.doesNotMatch(appHtml, /Próxima deixa:/);
  assert.doesNotMatch(appHtml, /Deixa para o próximo tópico:/);
});

test('análise de algoritmos explica critérios, comparações e armadilhas', () => {
  assert.match(appHtml, /Analisar algoritmo é prever comportamento antes de rodar/);
  assert.match(appHtml, /A receita mental/);
  assert.match(appHtml, /Busca linear/);
  assert.match(appHtml, /Busca binária/);
  assert.match(appHtml, /Tabela hash/);
  assert.match(appHtml, /Árvore balanceada/);
  assert.match(appHtml, /Teorema Mestre sem mistério/);
  assert.match(appHtml, /armadilhas comuns/);
  assert.match(appHtml, /Cartões comparativos: escolha pelo contexto do problema/);
  assert.match(appHtml, /Checklist de leitura: use como alertas de projeto/);
});

test('estruturas explicam política de acesso, origem e problema resolvido', () => {
  assert.match(appHtml, /ordered-notes/);
  assert.match(appHtml, /Leitura guiada: origem, acesso, ganho e limite/);
  assert.match(appHtml, /Leitura guiada: problema, política, acesso e uso/);
  assert.match(appHtml, /Por que arrays foram criados\?/);
  assert.match(appHtml, /Política de acesso:<\/strong> acesso aleatório por índice/);
  assert.match(appHtml, /Como o dado é alcançado:<\/strong> por endereço base mais deslocamento/);
  assert.match(appHtml, /Política de acesso:<\/strong> acesso sequencial por nós/);
  assert.match(appHtml, /Por que listas ligadas foram criadas\?/);
  assert.match(appHtml, /Política de acesso:<\/strong> acesso restrito ao topo/);
  assert.match(appHtml, /Política de acesso:<\/strong> insere atrás, remove na frente/);
  assert.match(appHtml, /Política de acesso:<\/strong> comparação hierárquica/);
  assert.match(appHtml, /Política de acesso:<\/strong> acesso prioritário à raiz/);
  assert.match(appHtml, /Política de acesso:<\/strong> acesso por chave/);
  assert.match(appHtml, /Política de acesso:<\/strong> acesso por adjacência/);
});

test('simuladores, recomendações, analytics e exercícios dinâmicos estão integrados à API', () => {
  assert.match(appHtml, /SIMULATOR_CONFIGS/);
  assert.match(appHtml, /type:'ARRAY'/);
  assert.match(appHtml, /type:'STACK'/);
  assert.match(appHtml, /type:'QUEUE'/);
  assert.match(appHtml, /type:'LINKED_LIST'/);
  assert.match(appHtml, /type:'BST'/);
  assert.match(appHtml, /type:'HASH_TABLE'/);
  assert.match(appHtml, /type:'GRAPH'/);
  assert.match(appHtml, /AedApi\.recordSimulationEvent/);
  assert.match(appHtml, /AedApi\.getSimulatorMissions/);
  assert.match(appHtml, /AedApi\.submitSimulatorMission/);
  assert.match(appHtml, /AedApi\.getRecommendations\(\)/);
  assert.match(appHtml, /AedApi\.getAnalyticsOverview\(\)/);
  assert.match(appHtml, /AedApi\.getXpHistory\(\)/);
  assert.match(appHtml, /AedApi\.generateExercise\(pageId, difficulty\)/);
  assert.match(appHtml, /AedApi\.submitGeneratedExercise\(exercise\.id, btn\.dataset\.answer, elapsed\)/);
  assert.match(appHtml, /AedApi\.getCodeChallenges\(pageId\)/);
  assert.match(appHtml, /AedApi\.runCode\(challenge\.id/);
  assert.match(appHtml, /AedApi\.getCodeSubmissions/);
  assert.match(appHtml, /histórico de submissões/);
  assert.match(appHtml, /challenge\.signature/);
  assert.match(appHtml, /result\.submissionId/);
  assert.match(appHtml, /result\.executionTimeMs/);
  assert.match(appHtml, /dica conceitual/);
  assert.match(appHtml, /pseudoSkeleton/);
  assert.match(appHtml, /passedCount/);
  assert.match(appHtml, /riskLevel/);
  assert.match(appHtml, /suggestedActivity/);
  assert.match(appHtml, /didacticFocus/);
  assert.match(appHtml, /id="recommendation-primary"/);
  assert.match(appHtml, /id="analytics-overview"/);
  assert.match(appHtml, /base do explorador/);
  assert.match(appHtml, /mapa de progresso/);
  assert.match(appHtml, /sandbox de código/);
  assert.match(appHtml, /validar missão/);
});

test('login usa AedApi e não usa fetch manual com credentials', () => {
  assert.match(loginHtml, /<script src="api\.js"><\/script>/);
  assert.match(loginHtml, /AedApi\.login\(email, password\)/);
  assert.match(loginHtml, /AedApi\.register\(username, email, password, fullName\)/);
  assert.doesNotMatch(loginHtml, /const API_BASE =/);
  assert.doesNotMatch(loginHtml, /credentials:\s*'include'/);
});

test('interface não expõe status técnico de API para o aluno', () => {
  assert.match(loginHtml, /Ambiente seguro/);
  assert.match(loginHtml, /v2\.0 · AED Studio/);
  assert.match(appHtml, /v2\.0 · jornada do explorador/);
  assert.doesNotMatch(loginHtml, /API verificando|API online|API offline|JWT \+ Sessão/);
  assert.doesNotMatch(appHtml, /API verificando|API online|API offline|API integrada/);
  assert.doesNotMatch(appHtml, /\.shell\.app-loading\s*\{[^}]*opacity:\s*\.(?:3|35|4)/s);
  assert.doesNotMatch(appHtml, /\.shell\.app-loading\s*\{[^}]*pointer-events:\s*none/s);
});

test('health público e base configurável existem no cliente central', () => {
  assert.match(apiJs, /function resolveInitialBase\(\)/);
  assert.match(apiJs, /localStorage\.getItem\(API_BASE_KEY\)/);
  assert.match(apiJs, /async function health\(\)/);
  assert.match(apiJs, /\/api\/health/);
  assert.match(apiJs, /async function getTopicCatalog\(\)/);
  assert.match(apiJs, /\/api\/catalog\/topics/);
  assert.match(apiJs, /async function getExercises\(topicId\)/);
  assert.match(apiJs, /async function submitExercise\(exerciseId, answer/);
  assert.match(apiJs, /async function generateExercise\(topicId, difficulty/);
  assert.match(apiJs, /async function recordSimulationEvent\(topicId, simulatorType/);
  assert.match(apiJs, /async function getRecommendations\(\)/);
  assert.match(apiJs, /async function getAnalyticsOverview\(\)/);
  assert.match(apiJs, /async function getSimulatorMissions\(topicId\)/);
  assert.match(apiJs, /async function submitSimulatorMission\(missionId, stateSnapshot\)/);
  assert.match(apiJs, /async function getXpHistory\(\)/);
  assert.match(apiJs, /async function getCodeChallenges\(topicId\)/);
  assert.match(apiJs, /async function runCode\(challengeId, code\)/);
  assert.match(apiJs, /async function getCodeSubmissions/);
  assert.match(apiJs, /async function getLatestCodeSubmission\(exerciseId\)/);
  assert.match(apiJs, /async function getBestCodeSubmission\(exerciseId\)/);
});
