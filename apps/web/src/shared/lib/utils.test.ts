import { describe, expect, it } from "vitest";

import { cn } from "./utils";

describe("cn", () => {
  it("조건부 클래스를 합친다", () => {
    expect(cn("px-2", false && "hidden", undefined, "text-sm")).toBe(
      "px-2 text-sm",
    );
  });

  it("충돌하는 Tailwind 클래스는 뒤에 온 값을 남긴다", () => {
    expect(cn("px-2 py-1", "px-4")).toBe("py-1 px-4");
  });
});
