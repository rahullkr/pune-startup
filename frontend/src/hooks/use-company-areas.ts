import { useQuery } from "@tanstack/react-query";
import { getCompanyAreas } from "@/features/companies/api";

export function useCompanyAreas() {
  return useQuery({
    queryKey: ["companies", "areas"],
    queryFn: ({ signal }) => getCompanyAreas(signal),
    staleTime: 5 * 60_000,
  });
}
