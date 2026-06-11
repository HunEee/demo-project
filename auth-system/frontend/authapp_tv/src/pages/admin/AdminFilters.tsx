import { Filter, RotateCcw, Search } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

export type AdminFilterValues = Record<string, string>;

export type AdminFilterField = {
  name: string;
  label: string;
  type?: "text" | "date" | "number" | "select";
  placeholder?: string;
  options?: Array<{ label: string; value: string }>;
};

type AdminFiltersProps<T extends AdminFilterValues> = {
  fields: AdminFilterField[];
  values: T;
  onChange: (name: string, value: string) => void;
  onSubmit: () => void;
  onReset: () => void;
  hint?: string;
};

export default function AdminFilters<T extends AdminFilterValues>({
  fields,
  values,
  onChange,
  onSubmit,
  onReset,
  hint = "서버 필터 적용",
}: AdminFiltersProps<T>) {
  return (
    <Card className="rounded-lg">
      <CardContent className="p-4">
        <form
          className="space-y-4"
          onSubmit={(event) => {
            event.preventDefault();
            onSubmit();
          }}
        >
          <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
            {fields.map((field) => (
              <div key={field.name} className="space-y-1.5">
                <Label htmlFor={`admin-filter-${field.name}`} className="text-xs text-muted-foreground">
                  {field.label}
                </Label>
                {field.type === "select" ? (
                  <select
                    id={`admin-filter-${field.name}`}
                    className="h-9 w-full rounded-lg border border-input bg-background px-2.5 text-sm outline-none transition-colors focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
                    value={values[field.name] ?? ""}
                    onChange={(event) => onChange(field.name, event.target.value)}
                  >
                    {(field.options ?? []).map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                ) : (
                  <Input
                    id={`admin-filter-${field.name}`}
                    className="h-9"
                    type={field.type ?? "text"}
                    min={field.type === "number" ? 0 : undefined}
                    value={values[field.name] ?? ""}
                    placeholder={field.placeholder}
                    onChange={(event) => onChange(field.name, event.target.value)}
                  />
                )}
              </div>
            ))}
          </div>

          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <span className="inline-flex items-center gap-1 text-sm text-muted-foreground">
              <Filter className="h-3.5 w-3.5" />
              {hint}
            </span>

            <div className="flex justify-end gap-2">
              <Button type="submit" className="h-9">
                <Search className="h-4 w-4" />
                검색
              </Button>
              <Button type="button" variant="outline" className="h-9" onClick={onReset}>
                <RotateCcw className="h-4 w-4" />
                초기화
              </Button>
            </div>
          </div>
        </form>
      </CardContent>
    </Card>
  );
}
