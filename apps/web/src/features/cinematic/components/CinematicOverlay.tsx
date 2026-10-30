import { Button } from "@/shared/components/ui/button";

export type OverlayState = "hidden" | "loading" | "error";

type CinematicOverlayProps = {
  state: OverlayState;
  onRetry: () => void;
};

export function CinematicOverlay({ state, onRetry }: CinematicOverlayProps) {
  return (
    <>
      <p aria-live="polite" className="absolute inset-x-0 bottom-10 text-center text-sm text-white/70">
        {state === "loading" ? "시네마틱을 불러오는 중" : ""}
      </p>
      {state === "error" && (
        <div
          role="alert"
          className="absolute inset-0 flex flex-col items-center justify-center gap-4 bg-black/70 px-6 text-center text-white"
        >
          <p>시네마틱 영상을 불러오지 못했습니다.</p>
          <div className="flex gap-3">
            <Button onClick={onRetry}>다시 시도</Button>
            <Button asChild variant="outline" className="text-foreground">
              <a href="#after-cinematic">시네마틱 건너뛰기</a>
            </Button>
          </div>
        </div>
      )}
    </>
  );
}
