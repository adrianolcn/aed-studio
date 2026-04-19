export function recommendationTone(category) {
  const key = String(category || '').toUpperCase();
  if (key.includes('REVIEW')) return 'review';
  if (key.includes('TRAIL')) return 'trail';
  return 'next';
}
