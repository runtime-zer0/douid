import Link from "next/link";

import { cn } from "@/shared/lib/utils";

import { type SiteContent, siteContent } from "../content";
import { BrandMark } from "./BrandMark";
import { NAV_ITEMS } from "./navigation";
import { FOCUS_RING } from "./styles";

/** 사이트 공통 푸터, 건너뛰기 링크 도착점, 승인 콘텐츠가 있는 영역만 표시 */
export function SiteFooter({ content = siteContent }: { content?: SiteContent }) {
  const { contact, studio, copyright, policyLinks } = content;
  const hasContact = contact.email !== null || contact.channels.length > 0;

  return (
    <footer
      id="site-footer"
      tabIndex={-1}
      className="bg-footer px-4 py-16 text-footer-foreground outline-none sm:px-6 md:py-24"
    >
      <div className="mx-auto flex max-w-6xl flex-col gap-12">
        <div className="flex flex-col gap-10 md:flex-row md:items-start md:justify-between">
          <BrandMark className="w-32" />
          <nav aria-label="푸터 메뉴">
            <ul className="flex flex-wrap gap-6 text-sm tracking-[0.2em]">
              {NAV_ITEMS.map((item) => (
                <li key={item.href}>
                  <Link href={item.href} className={cn(FOCUS_RING, "opacity-80 hover:opacity-100")}>
                    {item.label}
                  </Link>
                </li>
              ))}
            </ul>
          </nav>
        </div>
        {hasContact && (
          <address className="flex flex-col gap-2 text-sm not-italic">
            {contact.email && (
              <a href={`mailto:${contact.email}`} className={cn(FOCUS_RING, "w-fit hover:underline")}>
                {contact.email}
              </a>
            )}
            {contact.channels.map((channel) => (
              <a key={channel.href} href={channel.href} className={cn(FOCUS_RING, "w-fit hover:underline")}>
                {channel.label}
              </a>
            ))}
          </address>
        )}
        {(studio || copyright || policyLinks.length > 0) && (
          <div className="flex flex-col gap-3 border-t border-white/10 pt-8 text-xs opacity-70 md:flex-row md:justify-between">
            <div className="flex flex-col gap-1">
              {studio && <p>{studio}</p>}
              {copyright && <p>{copyright}</p>}
            </div>
            {policyLinks.length > 0 && (
              <nav aria-label="정책">
                <ul className="flex flex-wrap gap-4">
                  {policyLinks.map((link) => (
                    <li key={link.href}>
                      <Link href={link.href} className={cn(FOCUS_RING, "hover:underline")}>
                        {link.label}
                      </Link>
                    </li>
                  ))}
                </ul>
              </nav>
            )}
          </div>
        )}
      </div>
    </footer>
  );
}
