export function byId(id) {
  return document.getElementById(id);
}

export function setText(id, value) {
  const el = byId(id);
  if (el) el.textContent = value ?? '';
}

export function toggleClass(el, className, enabled) {
  if (el) el.classList.toggle(className, Boolean(enabled));
}
