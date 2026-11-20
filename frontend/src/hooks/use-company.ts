import { useQuery } from "@tanstack/react-query";
import { getCompany } from "@/features/companies/api";

export function useCompany(companyId: string | undefined) {
  return useQuery({
    queryKey: ["company", companyId],
    queryFn: ({ signal }) => getCompany(companyId as string, signal),
    enabled: Boolean(companyId),
    staleTime: 60_000,
  });
}
