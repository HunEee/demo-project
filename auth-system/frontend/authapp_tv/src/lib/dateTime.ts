const pad = (value: number) => String(value).padStart(2, "0");

export const formatLocalDateInputValue = (date = new Date()) => {
  return [
    date.getFullYear(),
    pad(date.getMonth() + 1),
    pad(date.getDate()),
  ].join("-");
};

export const formatSecurityDateTime = (value?: string | null) => {
  if (!value) return "기록 없음";

  const isoMatch = value.match(
    /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})/,
  );

  if (isoMatch) {
    const [, year, month, day, hour, minute, second] = isoMatch;
    return `${year}-${month}-${day} ${hour}:${minute}:${second}`;
  }

  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return value;

  return [
    `${parsed.getFullYear()}-${pad(parsed.getMonth() + 1)}-${pad(parsed.getDate())}`,
    `${pad(parsed.getHours())}:${pad(parsed.getMinutes())}:${pad(parsed.getSeconds())}`,
  ].join(" ");
};

// formatSecurityDateTime("2026-05-24T13:34:44.926486") === "2026-05-24 13:34:44"
// formatLocalDateInputValue(new Date("2026-05-25T00:30:00+09:00")) === "2026-05-25"
