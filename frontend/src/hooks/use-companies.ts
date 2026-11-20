import { useQuery } from "@tanstack/react-query";
import { getCompanies, type CompanySearchParams } from "@/features/companies/api";

export function useCompanies(params: CompanySearchParams = {}) {
  return useQuery({
    queryKey: ["companies", params],
    queryFn: ({ signal }) => getCompanies(params, signal),
    staleTime: 60_000,
  });
}
