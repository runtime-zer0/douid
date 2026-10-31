import { readFileSync } from "node:fs";

import react from "@vitejs/plugin-react";
import type { Plugin } from "vite";
import { defineConfig } from "vitest/config";

// Next의 정적 이미지 import와 같은 `{ src, width, height }` 형태로 SVG를 해석, Vite 기본값은 URL 문자열
const nextStaticSvg: Plugin = {
  name: "next-static-svg",
  enforce: "pre",
  load(id) {
    const path = id.split("?", 1)[0];
    if (!path.endsWith(".svg")) return;
    const svg = readFileSync(path, "utf8");
    const size = (name: string) => Number(svg.match(new RegExp(`${name}="(\\d+(?:\\.\\d+)?)"`))?.[1] ?? 0);
    return `export default ${JSON.stringify({ src: path, width: size("width"), height: size("height") })};`;
  },
};

export default defineConfig({
  plugins: [nextStaticSvg, react()],
  resolve: {
    tsconfigPaths: true,
    // Next가 내부 alias로 처리하는 server-only를 테스트에서도 같은 방식으로 해석
    alias: { "server-only": "next/dist/compiled/server-only/empty.js" },
  },
  test: {
    environment: "jsdom",
    setupFiles: ["./src/test/setup.ts"],
    include: ["src/**/*.test.{ts,tsx}"],
  },
});
