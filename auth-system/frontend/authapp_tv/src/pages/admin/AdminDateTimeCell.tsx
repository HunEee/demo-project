import { formatSecurityDateTime } from "@/lib/dateTime";

function splitDateTime(value?: string | null) {
  if (!value) return { date: "-", time: "" };
  const formatted = formatSecurityDateTime(value);
  if (!formatted.includes(" ")) return { date: formatted, time: "" };
  const [date, ...timeParts] = formatted.split(" ");
  return { date, time: timeParts.join(" ") };
}

export default function AdminDateTimeCell({ value }: { value?: string | null }) {
  const { date, time } = splitDateTime(value);
  return (
    <div className="text-center tabular-nums">
      <div>{date}</div>
      {time ? <div className="text-xs text-muted-foreground">{time}</div> : null}
    </div>
  );
}
