import { SiteFooter, SiteHeader } from "@/features/site";

export default function WorkLayout({ children }: LayoutProps<"/work">) {
  return (
    <>
      <SiteHeader tone="light" />
      <main className="mx-auto flex w-full max-w-6xl flex-1 flex-col px-4 pt-32 pb-24 sm:px-6">{children}</main>
      <SiteFooter />
    </>
  );
}
