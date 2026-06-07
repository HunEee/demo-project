import { formatSecurityDateTime } from "@/lib/dateTime";

const splitDateTime = (value?: string | null) => {
  const formatted = formatSecurityDateTime(value);
  if (!value || formatted === "기록 없음" || !formatted.includes(" ")) {
    return { date: formatted, time: "" };
  }
  const [date, ...timeParts] = formatted.split(" ");
  return { date, time: timeParts.join(" ") };
};

export default function MfaDateTimeCell({ value }: { value?: string | null }) {
  const { date, time } = splitDateTime(value);
  return (
    <div className="text-center tabular-nums">
      <div>{date}</div>
      {time ? <div className="text-xs text-muted-foreground">{time}</div> : null}
    </div>
  );
}
