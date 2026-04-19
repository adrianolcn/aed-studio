export const simulatorTypes = Object.freeze([
  'ARRAY',
  'STACK',
  'QUEUE',
  'LINKED_LIST',
  'BST',
  'HASH_TABLE',
  'GRAPH',
]);

export function supportsSimulator(type) {
  return simulatorTypes.includes(String(type || '').toUpperCase());
}
