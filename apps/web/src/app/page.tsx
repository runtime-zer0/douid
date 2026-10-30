import { CinematicScroll } from "@/features/cinematic";
import { loadCinematicTable } from "@/features/cinematic/server";

export default function HomePage() {
  const table = loadCinematicTable();

  return (
    <main className="flex flex-1 flex-col">
      <CinematicScroll table={table} />
    </main>
  );
}
