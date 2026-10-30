import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

export default defineConfig({
  plugins: [react()],
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
