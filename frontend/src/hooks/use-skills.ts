import { useQuery } from "@tanstack/react-query";
import { getSkills } from "@/features/skills/api";

export function useSkills() {
  return useQuery({
    queryKey: ["skills", "active-jobs"],
    queryFn: ({ signal }) => getSkills(signal),
    staleTime: 5 * 60_000,
  });
}
