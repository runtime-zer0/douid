import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, type RenderOptions } from "@testing-library/react";
import type { ReactElement, ReactNode } from "react";

/** 테스트별 QueryClient로 렌더, 재시도 끔 */
export function renderWithQuery(ui: ReactElement, options?: RenderOptions & { queryClient?: QueryClient }) {
  const queryClient =
    options?.queryClient ??
    new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: Infinity }, mutations: { retry: false } } });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  return { queryClient, ...render(ui, { wrapper, ...options }) };
}
