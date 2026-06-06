// src/app/providers.tsx

import { QueryClientProvider } from "@tanstack/react-query";

import { queryClient } from "./queryClient";

type AppProvidersProps = {
  children: React.ReactNode;
};

export function AppProviders({ children }: AppProvidersProps) {
  return (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
}
