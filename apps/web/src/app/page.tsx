import { CinematicScroll } from "@/features/cinematic";
import { loadCinematicTable } from "@/features/cinematic/server";
import { BrandMark, SiteFooter, SiteHeader } from "@/features/site";

export default function HomePage() {
  const table = loadCinematicTable();

  return (
    <>
      <main className="flex flex-1 flex-col">
        <CinematicScroll table={table} endingMark={<BrandMark decorative className="w-[min(36vw,28rem)]" />} />
      </main>
      {/* 헤더는 fixed라 화면 위치와 무관, 첫 Tab이 시네마틱 건너뛰기 링크가 되도록 DOM에서는 뒤에 둠 */}
      <SiteHeader intro />
      <SiteFooter />
    </>
  );
}
