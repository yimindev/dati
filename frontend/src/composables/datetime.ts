export function formatDateTime(rawDate: string, fallback = rawDate) {
  const date = new Date(rawDate);
  if (Number.isNaN(date.getTime())) return fallback;
  return date.toLocaleString();
}