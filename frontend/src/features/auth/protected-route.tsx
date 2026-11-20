import { Navigate, Outlet, useLocation } from "react-router-dom";
import { LoadingState } from "@/components/ui/states";
import { useAuth } from "@/features/auth/auth-context";

export function ProtectedRoute() {
  const { candidate, isPending } = useAuth();
  const location = useLocation();

  if (isPending) {
    return <LoadingState className="container-page my-16" label="Checking your session…" />;
  }

  if (!candidate) {
    const next = `${location.pathname}${location.search}`;
    return <Navigate to={`/login?next=${encodeURIComponent(next)}`} replace />;
  }

  return <Outlet />;
}
