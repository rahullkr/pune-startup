import { useQuery } from "@tanstack/react-query";
import { getJob } from "@/features/jobs/api";

export function useJob(jobId: string | undefined) {
  return useQuery({
    queryKey: ["jobs", jobId],
    queryFn: ({ signal }) => getJob(jobId as string, signal),
    enabled: Boolean(jobId),
    staleTime: 60_000,
  });
}
