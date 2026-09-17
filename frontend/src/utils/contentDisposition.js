export function contentDispositionFilename(disposition, fallback) {
  const value = String(disposition || '');
  const utf = value.match(/filename\*=UTF-8''([^;]+)/i);
  const plain = value.match(/filename="?([^";]+)"?/i);
  try { return decodeURIComponent(utf?.[1] || plain?.[1] || fallback); }
  catch { return fallback; }
}
