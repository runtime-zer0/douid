import type { Metadata } from "next";

import { LoginScreen } from "@/features/admin";

export const metadata: Metadata = { title: "관리자 로그인" };

export default async function AdminLoginPage({ searchParams }: PageProps<"/admin/login">) {
  const { next } = await searchParams;
  return <LoginScreen next={typeof next === "string" ? next : undefined} />;
}
