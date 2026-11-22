import { ChatWidget } from "@/features/chat";
import { getRecommendationCatalog } from "@/features/work/server";

// 채팅은 공개 페이지에만, 공개 라우트 사이 이동에서도 위젯 상태 유지
export default async function PublicLayout({ children }: LayoutProps<"/">) {
  return (
    <>
      {children}
      <ChatWidget projects={await getRecommendationCatalog()} />
    </>
  );
}
