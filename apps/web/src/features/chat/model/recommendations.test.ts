import { describe, expect, it } from "vitest";

import { matchRecommendations } from "./recommendations";

const projects = [
  { id: "published-a", title: "A" },
  { id: "published-b", title: "B" },
];

describe("matchRecommendations", () => {
  it("카탈로그에 있는 ID만 입력 순서대로 중복 없이", () => {
    expect(matchRecommendations(["published-b", 404, "published-a", "published-b", "draft-x"], projects)).toEqual([
      { id: "published-b", title: "B", href: "/work/projects/published-b" },
      { id: "published-a", title: "A", href: "/work/projects/published-a" },
    ]);
  });

  it("숫자 ID는 문자열로 비교", () => {
    expect(matchRecommendations([7], [{ id: "7", title: "Seven" }])).toEqual([{ id: "7", title: "Seven", href: "/work/projects/7" }]);
  });

  it("경로 문자는 인코딩", () => {
    expect(matchRecommendations(["a/b"], [{ id: "a/b", title: "X" }])[0].href).toBe("/work/projects/a%2Fb");
  });

  it("빈 카탈로그는 빈 결과", () => {
    expect(matchRecommendations(["published-a"], [])).toEqual([]);
  });
});
