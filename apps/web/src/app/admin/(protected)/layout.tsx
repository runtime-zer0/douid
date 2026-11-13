import { AdminGate, AdminShell, ReauthProvider, UnsavedChangesProvider } from "@/features/admin";

export default function ProtectedAdminLayout({ children }: LayoutProps<"/admin">) {
  return (
    <ReauthProvider>
      <UnsavedChangesProvider>
        <AdminGate>
          <AdminShell>{children}</AdminShell>
        </AdminGate>
      </UnsavedChangesProvider>
    </ReauthProvider>
  );
}
