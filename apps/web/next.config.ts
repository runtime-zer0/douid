import path from "node:path";
import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // 모노레포 상위 lockfile 탐색을 막고 apps/web을 독립 프로젝트 루트로 고정
  turbopack: {
    root: path.join(__dirname),
  },
  async rewrites() {
    const apiOrigin = process.env.API_ORIGIN;
    // 브라우저는 같은 Origin `/api`만 호출, 세션·CSRF 쿠키를 first-party로 유지
    return apiOrigin ? [{ source: "/api/:path*", destination: `${apiOrigin}/api/:path*` }] : [];
  },
};

export default nextConfig;
