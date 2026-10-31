import type { Metadata } from "next";
import Link from "next/link";

import { SiteFooter, SiteHeader, siteContent } from "@/features/site";

export const metadata: Metadata = { title: "Contact" };

export default function ContactPage() {
  const { email, channels } = siteContent.contact;
  const hasContact = email !== null || channels.length > 0;

  return (
    <>
      <SiteHeader tone="light" />
      <main className="mx-auto flex w-full max-w-3xl flex-1 flex-col gap-8 px-4 pt-32 pb-24 sm:px-6">
        <h1 className="font-heading text-4xl tracking-tight md:text-5xl">CONTACT</h1>
        {hasContact ? (
          <address className="flex flex-col gap-3 not-italic">
            {email && (
              <a href={`mailto:${email}`} className="w-fit underline underline-offset-4">
                {email}
              </a>
            )}
            {channels.map((channel) => (
              <a key={channel.href} href={channel.href} className="w-fit underline underline-offset-4">
                {channel.label}
              </a>
            ))}
          </address>
        ) : (
          <>
            <p className="text-muted-foreground">콘텐츠 준비 중입니다.</p>
            <Link href="/work" className="w-fit underline underline-offset-4">
              WORK 보기
            </Link>
          </>
        )}
      </main>
      <SiteFooter />
    </>
  );
}
