import { useQuery } from "@tanstack/react-query";
import { getIndustries } from "@/features/companies/api";

export function useIndustries() {
  return useQuery({
    queryKey: ["industries"],
    queryFn: ({ signal }) => getIndustries(signal),
    staleTime: 5 * 60_000,
  });
}
