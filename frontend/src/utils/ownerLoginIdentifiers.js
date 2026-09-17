const PHONE_INPUT_PATTERN = /^[+\d\s().-]+$/;

export function ownerLoginIdentifiers(value, dialCode) {
  const raw = String(value || '').trim();
  if (!raw || raw.includes('@') || !PHONE_INPUT_PATTERN.test(raw)) return [raw];

  const digits = raw.replace(/\D/g, '');
  const dialDigits = String(dialCode || '').replace(/\D/g, '');
  if (!digits) return [raw];

  const international = raw.startsWith('+')
    ? `+${digits}`
    : `+${dialDigits}${digits.replace(/^0+/, '')}`;
  const candidates = new Set([international]);

  const internationalDigits = international.slice(1);
  if (dialDigits && internationalDigits.startsWith(dialDigits)) {
    const national = internationalDigits.slice(dialDigits.length);
    if (national) {
      candidates.add(national);
      if (dialDigits === '60' || dialDigits === '66') candidates.add(`0${national}`);
    }
  }
  if (!raw.startsWith('+')) candidates.add(digits);
  return [...candidates];
}

export async function loginWithIdentifierFallback(loginFn, payload, portal) {
  const identifiers = [...new Set(payload.identifiers || [])];
  let lastError;
  for (let index = 0; index < identifiers.length; index += 1) {
    try {
      return await loginFn({
        identifier: identifiers[index],
        password: payload.password,
        rememberMe: payload.rememberMe
      }, portal);
    } catch (error) {
      lastError = error;
      if (Number(error?.status || 0) !== 401 || index === identifiers.length - 1) throw error;
    }
  }
  throw lastError;
}
