const LOGIN_ERROR_KEYS = new Map([
  ['Invalid username or password', 'login.invalidCredentials'],
  ['Account is disabled', 'login.accountDisabled'],
  ['This account can only sign in through the administrator portal', 'login.adminPortalRequired'],
  ['This account can only sign in through the owner portal', 'login.ownerPortalRequired'],
  ['Account type is not configured correctly', 'login.accountConfiguration'],
  ['Username or email is required', 'login.identifierRequired'],
  ['Password is required', 'login.passwordRequired']
]);

const NETWORK_FAILURE_PATTERN = /Failed to fetch|NetworkError|Load failed|Network request failed|AbortError/i;

export function resolveLoginErrorKey(error) {
  const apiMessage = String(error?.apiMessage || '').trim();
  const message = String(error?.message || '').trim();
  const status = Number(error?.status || 0);

  if (LOGIN_ERROR_KEYS.has(apiMessage)) return LOGIN_ERROR_KEYS.get(apiMessage);
  if (NETWORK_FAILURE_PATTERN.test(apiMessage) || NETWORK_FAILURE_PATTERN.test(message)) return 'login.networkFailed';
  if (status === 401) return 'login.invalidCredentials';
  if (status === 429) return 'login.tooManyAttempts';
  if (status >= 500) return 'login.serviceUnavailable';
  return 'login.failed';
}
