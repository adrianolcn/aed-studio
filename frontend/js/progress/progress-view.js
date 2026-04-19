export function progressPercent(done, total) {
  if (!total) return 0;
  return Math.max(0, Math.min(100, Math.round((done / total) * 100)));
}

export function formatXp(value) {
  return Number(value || 0).toLocaleString('pt-BR');
}
