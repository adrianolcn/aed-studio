/**
 * Convenções de domínio usadas pelo front.
 * A fonte de verdade continua sendo o back-end; estes valores ajudam a manter
 * nomes e estados consistentes em renderizações locais.
 */
export const topicStates = Object.freeze({
  locked: 'LOCKED',
  available: 'AVAILABLE',
  visited: 'VISITED',
  completed: 'COMPLETED',
});

export const learningFlow = Object.freeze([
  'ler teoria',
  'analisar custo',
  'manipular simulador',
  'resolver exercícios',
  'validar código',
]);

export function isUnlocked(state) {
  return state === topicStates.available || state === topicStates.visited || state === topicStates.completed;
}
