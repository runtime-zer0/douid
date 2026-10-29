import path from "node:path";
import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // 모노레포 상위 lockfile 탐색을 막고 apps/web을 독립 프로젝트 루트로 고정
  turbopack: {
    root: path.join(__dirname),
  },
};

export default nextConfig;
