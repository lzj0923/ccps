const ISO_DATE_PREFIX = /^(\d{4})-(\d{2})-(\d{2})/;

/** Display-only date format. API and native date inputs continue to use ISO yyyy-MM-dd. */
export function formatDate(value, fallback = '—') {
  if (!value) return fallback;
  const match = String(value).match(ISO_DATE_PREFIX);
  if (match) return `${match[3]}/${match[2]}/${match[1]}`;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return fallback;
  return `${String(date.getDate()).padStart(2, '0')}/${String(date.getMonth() + 1).padStart(2, '0')}/${date.getFullYear()}`;
}

export function formatDateTime(value, fallback = '—') {
  if (!value) return fallback;
  const date = formatDate(value, fallback);
  const time = String(value).match(/[T ](\d{2}:\d{2})(?::\d{2})?/);
  return time ? `${date} ${time[1]}` : date;
}

export function todayIsoDate() {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}
