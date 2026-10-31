import Image from "next/image";

import darkLogo from "@/assets/brand/douid-logo-dark.svg";
import lightLogo from "@/assets/brand/douid-logo-light.svg";
import { cn } from "@/shared/lib/utils";

const LOGOS = { dark: darkLogo, light: lightLogo } as const;

type BrandMarkProps = {
  /** 배경 기준 변형, dark는 어두운 배경용 흰색 로고 */
  tone?: keyof typeof LOGOS;
  /** 같은 이름을 이미 제공하는 요소 안이면 보조 기술에서 숨김 */
  decorative?: boolean;
  className?: string;
};

/** DOUID 정본 SVG 렌더러, 크기만 조정하고 형태는 원본 유지 */
export function BrandMark({ tone = "dark", decorative = false, className }: BrandMarkProps) {
  return (
    <Image
      src={LOGOS[tone]}
      alt={decorative ? "" : "DOUID"}
      aria-hidden={decorative || undefined}
      // 모든 위치가 같은 파일이고 헤더에서 바로 받으므로 지연 로딩 이점 없음
      loading="eager"
      unoptimized
      draggable={false}
      className={cn("h-auto w-40 select-none", className)}
    />
  );
}
