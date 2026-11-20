import { lazy, Suspense } from "react";
import { Route, Routes } from "react-router-dom";
import { Layout } from "@/components/layout/layout";
import { LoadingState } from "@/components/ui/states";
import { ProtectedRoute } from "@/features/auth/protected-route";
import { CompanyProfilePage } from "@/pages/company-profile-page";
import { DashboardPage } from "@/pages/dashboard-page";
import { HomePage } from "@/pages/home-page";
import { JobDetailPage } from "@/pages/job-detail-page";
import { JobsPage } from "@/pages/jobs-page";
import { LoginPage } from "@/pages/login-page";
import { NotFoundPage } from "@/pages/not-found-page";
import { RegisterPage } from "@/pages/register-page";
import { StartupsPage } from "@/pages/startups-page";

const MapPage = lazy(() =>
  import("@/pages/map-page").then((module) => ({ default: module.MapPage })),
);

export function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<HomePage />} />
        <Route path="startups" element={<StartupsPage />} />
        <Route path="startups/:companyId" element={<CompanyProfilePage />} />
        <Route path="jobs" element={<JobsPage />} />
        <Route path="jobs/:jobId" element={<JobDetailPage />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="register" element={<RegisterPage />} />
        <Route element={<ProtectedRoute />}>
          <Route path="dashboard" element={<DashboardPage />} />
        </Route>
        <Route
          path="map"
          element={
            <Suspense
              fallback={<LoadingState className="container-page my-16" label="Loading the map…" />}
            >
              <MapPage />
            </Suspense>
          }
        />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
