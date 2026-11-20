import { useQuery } from "@tanstack/react-query";
import { getCompanyJobs } from "@/features/jobs/api";

export function useCompanyJobs(companyId: string | undefined, size = 6) {
  return useQuery({
    queryKey: ["company", companyId, "jobs", { size }],
    queryFn: ({ signal }) => getCompanyJobs(companyId as string, { size }, signal),
    enabled: Boolean(companyId),
    staleTime: 60_000,
  });
}
