export function missingRequiredFields(values = {}, requiredFields = {}) {
  return Object.entries(requiredFields)
    .filter(([field]) => !String(values[field] ?? '').trim())
    .map(([field, labelKey]) => ({ field, labelKey }));
}

export function findFirstEmptyRequiredControl(form) {
  if (!form?.querySelectorAll) return null;
  return [...form.querySelectorAll('input, textarea, select')]
    .find(control => {
      if (control.disabled || !control.required) return false;
      const type = String(control.type || '').toLowerCase();
      if (type === 'checkbox' || type === 'radio') return !control.checked;
      return !String(control.value ?? '').trim();
    }) || null;
}

export function findFirstInvalidControl(form) {
  return findFirstEmptyRequiredControl(form);
}

export function revealFirstInvalidControl(form) {
  const control = findFirstEmptyRequiredControl(form);
  if (!control) return null;
  control.scrollIntoView?.({ behavior: 'smooth', block: 'center', inline: 'nearest' });
  control.focus?.({ preventScroll: true });
  return control;
}
