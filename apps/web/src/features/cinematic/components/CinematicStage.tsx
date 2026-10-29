import { Button } from "@/shared/components/ui/button";

export function CinematicStage() {
  return (
    <section className="flex flex-1 flex-col items-center justify-center gap-6 px-6 text-center">
      <h1 className="font-heading text-5xl tracking-tight">DOUID</h1>
      <p className="text-muted-foreground">Brand portfolio is in progress.</p>
      <Button variant="outline" disabled>
        Coming soon
      </Button>
    </section>
  );
}
