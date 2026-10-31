export type SiteLink = { label: string; href: string };

export type SiteContent = {
  about: {
    /** ABOUT 본문, 승인 값만, 없으면 null */
    body: string | null;
  };
  contact: {
    /** 대표 이메일, 승인 값만, 없으면 null */
    email: string | null;
    /** 공식 연락 채널, 승인 값만, 없으면 빈 배열 */
    channels: SiteLink[];
  };
  /** 스튜디오 정보, 승인 값만, 없으면 null */
  studio: string | null;
  /** 저작권 문구, 승인 값만, 없으면 null */
  copyright: string | null;
  /** 정책 링크, 승인 값만, 없으면 빈 배열 */
  policyLinks: SiteLink[];
};

// 미제공 항목은 qa/release-gate.md에서 배포 차단 항목으로 관리
export const siteContent: SiteContent = {
  about: { body: null },
  contact: { email: null, channels: [] },
  studio: null,
  copyright: null,
  policyLinks: [],
};
