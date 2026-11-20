import { apiGet } from "@/api/client";
import type { SkillOption } from "@/features/skills/types";

export function getSkills(signal?: AbortSignal): Promise<SkillOption[]> {
  return apiGet<SkillOption[]>("/skills", signal);
}
