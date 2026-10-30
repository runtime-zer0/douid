import { Button } from "@/shared/components/ui/button";

/** 시네마틱 이후 안내 영역, 건너뛰기 링크의 도착 지점 */
export function CinematicStage() {
  return (
    <section
      id="after-cinematic"
      tabIndex={-1}
      className="flex min-h-svh flex-col items-center justify-center gap-6 px-6 text-center outline-none"
    >
      <h1 className="font-heading text-5xl tracking-tight">DOUID</h1>
      <p className="text-muted-foreground">Brand portfolio is in progress.</p>
      <Button variant="outline" disabled>
        Coming soon
      </Button>
    </section>
  );
}
