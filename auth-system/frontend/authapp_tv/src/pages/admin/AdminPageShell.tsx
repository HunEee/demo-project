import type { ReactNode } from "react";

type AdminPageShellProps = {
  title: string;
  description: string;
  actions?: ReactNode;
  children: ReactNode;
};

// 관리자 화면의 헤더와 본문을 같은 컨테이너 폭으로 묶어 정렬을 일관되게 유지
export default function AdminPageShell({
  title,
  description,
  actions,
  children,
}: AdminPageShellProps) {
  return (
    <main className="min-h-screen bg-background px-4 py-6 sm:px-6 lg:px-8">
      <div className="mx-auto flex w-full max-w-6xl flex-col gap-6">
        <header className="flex flex-col gap-4 border-b pb-5 md:flex-row md:items-end md:justify-between">
          <div className="min-w-0">
            <h1 className="text-2xl font-semibold tracking-normal text-foreground">{title}</h1>
            <p className="mt-1 max-w-3xl text-sm leading-6 text-muted-foreground">{description}</p>
          </div>
          {actions ? <div className="flex shrink-0 flex-wrap items-center gap-2">{actions}</div> : null}
        </header>
        {children}
      </div>
    </main>
  );
}
