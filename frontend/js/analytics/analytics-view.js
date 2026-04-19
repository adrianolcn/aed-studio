export function accuracyLabel(percent) {
  if (percent >= 85) return 'forte';
  if (percent >= 70) return 'adequado';
  if (percent > 0) return 'em atenção';
  return 'sem dados';
}
