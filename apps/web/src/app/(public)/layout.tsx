import { ChatWidget } from "@/features/chat";
import { getPublishedProjects } from "@/features/work/server";

// 채팅은 공개 페이지에만, 공개 라우트 사이 이동에서도 위젯 상태 유지
export default function PublicLayout({ children }: LayoutProps<"/">) {
  const projects = getPublishedProjects().map(({ id, title }) => ({ id, title }));

  return (
    <>
      {children}
      <ChatWidget projects={projects} />
    </>
  );
}
