/**
 * Adaptador do cliente HTTP central.
 *
 * O projeto ainda carrega `frontend/api.js` como script global para preservar
 * compatibilidade com o HTML estático. Este módulo oferece uma fronteira de
 * importação para novos códigos, testes e futuras extrações graduais.
 */
export function getApiClient() {
  if (!window.AedApi) {
    throw new Error('AedApi não foi carregado. Verifique se api.js vem antes dos módulos.');
  }
  return window.AedApi;
}

export const apiDomains = Object.freeze({
  auth: ['login', 'register', 'logout', 'me', 'requireAuth', 'refreshAccessToken'],
  learning: ['getTopicCatalog', 'getProgress', 'recordVisit', 'awardXp'],
  practice: ['getExercises', 'submitExercise', 'generateExercise', 'submitGeneratedExercise'],
  simulators: ['recordSimulationEvent', 'getSimulatorMissions', 'submitSimulatorMission'],
  intelligence: ['getRecommendations', 'getAnalyticsOverview', 'getTopicAnalytics', 'getTrailAnalytics'],
  code: ['getCodeChallenges', 'runCode', 'getCodeSubmissions', 'getLatestCodeSubmission', 'getBestCodeSubmission'],
});
