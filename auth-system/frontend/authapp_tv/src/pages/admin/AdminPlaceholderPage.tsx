import AdminPageShell from "@/pages/admin/AdminPageShell";

type AdminPlaceholderPageProps = {
  title: string;
  category: string;
};

export default function AdminPlaceholderPage({ title, category }: AdminPlaceholderPageProps) {
  return (
    <AdminPageShell title={title} description={`${category} 메뉴의 기능 화면입니다.`}>
      <section className="flex min-h-[340px] items-center justify-center rounded-lg border border-dashed bg-muted/20 px-6 text-center">
        <div>
          <p className="text-sm font-medium text-foreground">준비 중</p>
          <p className="mt-2 text-sm text-muted-foreground">이 화면은 메뉴 구조를 먼저 잡아둔 빈 화면입니다.</p>
        </div>
      </section>
    </AdminPageShell>
  );
}
