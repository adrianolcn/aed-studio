export const sandboxModes = Object.freeze({
  local: 'local',
  docker: 'docker',
});

export function isSafeDisplayStatus(status) {
  return ['SUCCESS', 'FAILURE', 'ERROR', 'TIMEOUT', 'COMPILE_ERROR'].includes(String(status || '').toUpperCase());
}
