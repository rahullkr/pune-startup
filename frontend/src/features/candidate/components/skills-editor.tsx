import { Plus, Sparkles, X } from "lucide-react";
import { useEffect, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "@/api/client";
import { Button } from "@/components/ui/button";
import { updateProfile } from "@/features/auth/api";
import { useAuth } from "@/features/auth/auth-context";
import { useSkills } from "@/hooks/use-skills";

interface SelectedSkill {
  skillId: string;
  name: string;
  proficiency?: string;
  yearsExperience?: number;
}

function sameSkillIds(left: SelectedSkill[], right: SelectedSkill[]): boolean {
  const leftIds = left.map((skill) => skill.skillId).sort();
  const rightIds = right.map((skill) => skill.skillId).sort();
  return leftIds.length === rightIds.length && leftIds.every((id, index) => id === rightIds[index]);
}

export function SkillsEditor() {
  const { candidate, refreshProfile } = useAuth();
  const skillsQuery = useSkills();
  const queryClient = useQueryClient();
  const [selected, setSelected] = useState<SelectedSkill[]>([]);
  const [pendingSkillId, setPendingSkillId] = useState("");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setSelected(
      (candidate?.skills ?? []).map((skill) => ({
        skillId: skill.skillId,
        name: skill.skillName,
        proficiency: skill.proficiency ?? undefined,
        yearsExperience: skill.yearsExperience ?? undefined,
      })),
    );
  }, [candidate]);

  const initial = (candidate?.skills ?? []).map((skill) => ({
    skillId: skill.skillId,
    name: skill.skillName,
  }));
  const selectedIds = new Set(selected.map((skill) => skill.skillId));
  const available = (skillsQuery.data ?? []).filter((skill) => !selectedIds.has(skill.id));

  const save = useMutation({
    mutationFn: () =>
      updateProfile({
        skills: selected.map((skill) => ({
          name: skill.name,
          proficiency: skill.proficiency,
          yearsExperience: skill.yearsExperience,
        })),
      }),
    onSuccess: async () => {
      setError(null);
      await refreshProfile();
      await queryClient.invalidateQueries({ queryKey: ["job-matches"] });
    },
    onError: (caught) => {
      setError(caught instanceof ApiError ? caught.message : "Couldn't save your skills. Try again.");
    },
  });

  function addSkill(skillId: string) {
    setPendingSkillId("");
    const skill = skillsQuery.data?.find((option) => option.id === skillId);
    if (!skill || selectedIds.has(skill.id)) {
      return;
    }
    setSelected((current) => [...current, { skillId: skill.id, name: skill.name }]);
  }

  function removeSkill(skillId: string) {
    setSelected((current) => current.filter((skill) => skill.skillId !== skillId));
  }

  return (
    <div className="rounded-3xl border border-slate-200 bg-white p-5">
      <div className="flex items-start gap-3">
        <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-brand-50 text-brand-700">
          <Sparkles aria-hidden="true" className="h-5 w-5" />
        </span>
        <div>
          <h2 className="text-sm font-semibold text-slate-900">Your skills</h2>
          <p className="mt-1 text-xs leading-relaxed text-slate-500">
            We use these to rank roles and explain each match.
          </p>
        </div>
      </div>

      <div className="mt-4 flex flex-wrap gap-2">
        {selected.length === 0 ? (
          <p className="text-xs text-slate-500">No skills yet. Add a few to unlock recommendations.</p>
        ) : null}
        {selected.map((skill) => (
          <span
            key={skill.skillId}
            className="inline-flex items-center gap-1 rounded-full border border-brand-200 bg-brand-50 py-1 pl-2.5 pr-1.5 text-xs font-medium text-brand-800"
          >
            {skill.name}
            <button
              type="button"
              onClick={() => removeSkill(skill.skillId)}
              className="grid h-5 w-5 place-items-center rounded-full text-brand-700 transition hover:bg-brand-200"
              aria-label={`Remove ${skill.name}`}
            >
              <X aria-hidden="true" className="h-3 w-3" />
            </button>
          </span>
        ))}
      </div>

      <div className="mt-4 flex gap-2">
        <select
          value={pendingSkillId}
          onChange={(event) => addSkill(event.target.value)}
          className="h-10 min-w-0 flex-1 rounded-xl border border-slate-300 bg-white px-3 text-sm text-slate-900 outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-200"
          aria-label="Add a skill"
        >
          <option value="">Add a skill...</option>
          {available.map((skill) => (
            <option key={skill.id} value={skill.id}>
              {skill.name}
            </option>
          ))}
        </select>
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={() => addSkill(pendingSkillId)}
          disabled={!pendingSkillId}
        >
          <Plus aria-hidden="true" className="h-4 w-4" />
          Add
        </Button>
      </div>

      {skillsQuery.isError ? (
        <p className="mt-3 text-xs text-red-600">Couldn't load the skill list.</p>
      ) : null}
      {error ? <p className="mt-3 text-xs text-red-600">{error}</p> : null}

      <div className="mt-4 flex items-center justify-between gap-3">
        <span className="text-xs text-slate-400">
          {selected.length} {selected.length === 1 ? "skill" : "skills"} selected
        </span>
        <Button
          type="button"
          variant="brand"
          size="sm"
          onClick={() => save.mutate()}
          disabled={save.isPending || sameSkillIds(selected, initial)}
        >
          {save.isPending ? "Saving..." : "Save skills"}
        </Button>
      </div>
    </div>
  );
}
