import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { getJobs, type JobSearchParams } from "@/features/jobs/api";

export function useJobs(params: JobSearchParams = {}) {
  return useQuery({
    queryKey: ["jobs", params],
    queryFn: ({ signal }) => getJobs(params, signal),
    placeholderData: keepPreviousData,
    staleTime: 60_000,
  });
}
