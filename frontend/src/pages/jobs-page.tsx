import { ChevronLeft, ChevronRight, Search, SlidersHorizontal, X } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { EmptyState, ErrorState } from "@/components/ui/states";
import { JobCardSkeleton } from "@/features/jobs/components/job-card-skeleton";
import { JobPreviewCard } from "@/features/jobs/components/job-preview-card";
import { useCompanyAreas } from "@/hooks/use-company-areas";
import { useJobs } from "@/hooks/use-jobs";
import { useSkills } from "@/hooks/use-skills";
import { formatEnum } from "@/lib/format";
import { cn } from "@/lib/utils";

const PAGE_SIZE = 12;
const controlClassName =
  "h-11 w-full rounded-full border border-slate-300 bg-white px-4 text-sm text-slate-900 transition focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500/30 disabled:cursor-not-allowed disabled:bg-slate-50 disabled:text-slate-400";

const experienceBands = [
  { value: "fresher", label: "Fresher-friendly" },
  { value: "0-2", label: "0–2 years" },
  { value: "2-5", label: "2–5 years" },
  { value: "5+", label: "5+ years" },
] as const;

const employmentTypes = [
  { value: "FULL_TIME", label: "Full-time" },
  { value: "PART_TIME", label: "Part-time" },
  { value: "CONTRACT", label: "Contract" },
  { value: "INTERNSHIP", label: "Internship" },
  { value: "TEMPORARY", label: "Temporary" },
];

const workModes = [
  { value: "ONSITE", label: "On-site" },
  { value: "HYBRID", label: "Hybrid" },
  { value: "REMOTE", label: "Remote" },
];

const sortOptions = [
  { value: "newest", label: "Newest first" },
  { value: "oldest", label: "Oldest first" },
  { value: "salary", label: "Salary: high to low" },
];

function FilterChip({ label, onRemove }: { label: string; onRemove: () => void }) {
  return (
    <span className="inline-flex items-center gap-1.5 rounded-full bg-slate-900 py-1 pl-3 pr-1.5 text-xs font-medium text-white">
      {label}
      <button
        type="button"
        onClick={onRemove}
        aria-label={`Remove ${label} filter`}
        className="grid h-5 w-5 place-items-center rounded-full transition hover:bg-white/20 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white"
      >
        <X aria-hidden="true" className="h-3 w-3" />
      </button>
    </span>
  );
}

function experienceBandFilter(band: string): { fresher?: boolean; experienceYears?: number } {
  switch (band) {
    case "fresher":
      return { fresher: true };
    case "0-2":
      return { experienceYears: 1 };
    case "2-5":
      return { experienceYears: 3 };
    case "5+":
      return { experienceYears: 5 };
    default:
      return {};
  }
}

export function JobsPage() {
  const [searchParams, setSearchParams] = useSearchParams();

  const keyword = searchParams.get("q")?.trim() ?? "";
  const area = searchParams.get("area") ?? "";
  const workMode = searchParams.get("workMode") ?? "";
  const employmentType = searchParams.get("employmentType") ?? "";
  const experience = searchParams.get("experience") ?? "";
  const sort = sortOptions.some((option) => option.value === searchParams.get("sort"))
    ? (searchParams.get("sort") as string)
    : "newest";
  const selectedSkills = useMemo(
    () => searchParams.getAll("skill").filter(Boolean),
    [searchParams],
  );
  const parsedPage = Number.parseInt(searchParams.get("page") ?? "0", 10);
  const page = Number.isFinite(parsedPage) && parsedPage > 0 ? parsedPage : 0;

  const [keywordInput, setKeywordInput] = useState(keyword);

  useEffect(() => {
    setKeywordInput(keyword);
  }, [keyword]);

  const updateFilters = useCallback(
    (updates: Record<string, string | null>) => {
      setSearchParams(
        (current) => {
          const next = new URLSearchParams(current);

          for (const [name, value] of Object.entries(updates)) {
            if (value) {
              next.set(name, value);
            } else {
              next.delete(name);
            }
          }

          next.delete("page");
          return next;
        },
        { replace: true },
      );
    },
    [setSearchParams],
  );

  useEffect(() => {
    const nextKeyword = keywordInput.trim();

    if (nextKeyword === keyword) {
      return;
    }

    const timer = window.setTimeout(() => {
      updateFilters({ q: nextKeyword || null });
    }, 350);

    return () => window.clearTimeout(timer);
  }, [keywordInput, keyword, updateFilters]);

  const experienceFilter = experienceBandFilter(experience);

  const jobsQuery = useJobs({
    keyword: keyword || undefined,
    area: area || undefined,
    workMode: workMode || undefined,
    employmentType: employmentType || undefined,
    fresher: experienceFilter.fresher,
    experienceYears: experienceFilter.experienceYears,
    skills: selectedSkills,
    sort: sort as "newest" | "oldest" | "salary",
    page,
    size: PAGE_SIZE,
  });
  const areasQuery = useCompanyAreas();
  const skillsQuery = useSkills();

  const skillsByCategory = useMemo(() => {
    const groups = new Map<string, { id: string; name: string }[]>();

    for (const skill of skillsQuery.data ?? []) {
      const category = skill.category ?? "Other";
      const entries = groups.get(category) ?? [];
      entries.push({ id: skill.id, name: skill.name });
      groups.set(category, entries);
    }

    return [...groups.entries()].sort(([a], [b]) => a.localeCompare(b));
  }, [skillsQuery.data]);

  const hasFilters = Boolean(
    keyword || area || workMode || employmentType || experience || selectedSkills.length,
  );

  function addSkill(skillName: string) {
    if (!skillName) {
      return;
    }

    setSearchParams(
      (current) => {
        if (current.getAll("skill").includes(skillName)) {
          return current;
        }

        const next = new URLSearchParams(current);
        next.append("skill", skillName);
        next.delete("page");
        return next;
      },
      { replace: true },
    );
  }

  function removeSkill(skillName: string) {
    setSearchParams(
      (current) => {
        const next = new URLSearchParams(current);
        next.delete("skill");
        for (const skill of current.getAll("skill")) {
          if (skill !== skillName) {
            next.append("skill", skill);
          }
        }
        next.delete("page");
        return next;
      },
      { replace: true },
    );
  }

  function clearFilters() {
    setKeywordInput("");
    setSearchParams({}, { replace: true });
  }

  function setPage(nextPage: number) {
    setSearchParams((current) => {
      const next = new URLSearchParams(current);

      if (nextPage > 0) {
        next.set("page", String(nextPage));
      } else {
        next.delete("page");
      }

      return next;
    });
  }

  const data = jobsQuery.data;
  const hasFilterOptionError = areasQuery.isError || skillsQuery.isError;
  const experienceLabel = experienceBands.find((band) => band.value === experience)?.label;

  return (
    <section className="container-page py-12 sm:py-16">
      <div className="max-w-2xl">
        <Badge>Open roles</Badge>
        <h1 className="mt-4 text-4xl font-black tracking-tight text-slate-900 sm:text-5xl">
          Work at a Pune startup.
        </h1>
        <p className="mt-4 text-base leading-relaxed text-slate-600 sm:text-lg">
          Search live roles by area, work mode, experience, and skills. Every listing shows the
          company behind it and links to the original application page.
        </p>
      </div>

      <div className="mt-8 rounded-3xl border border-slate-200 bg-white p-4 shadow-sm shadow-slate-900/5 sm:p-5">
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <label className="block sm:col-span-2">
            <span className="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-slate-500">
              Search
            </span>
            <span className="relative block">
              <Search
                aria-hidden="true"
                className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400"
              />
              <input
                type="search"
                value={keywordInput}
                onChange={(event) => setKeywordInput(event.target.value)}
                placeholder="Role, skill, or company"
                className={cn(controlClassName, "pl-10 pr-10")}
              />
              {keywordInput ? (
                <button
                  type="button"
                  onClick={() => {
                    setKeywordInput("");
                    updateFilters({ q: null });
                  }}
                  aria-label="Clear search"
                  className="absolute right-2 top-1/2 grid h-7 w-7 -translate-y-1/2 place-items-center rounded-full text-slate-400 transition hover:bg-slate-100 hover:text-slate-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"
                >
                  <X aria-hidden="true" className="h-4 w-4" />
                </button>
              ) : null}
            </span>
          </label>

          <label className="block">
            <span className="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-slate-500">
              Area
            </span>
            <select
              value={area}
              onChange={(event) => updateFilters({ area: event.target.value || null })}
              disabled={areasQuery.isPending}
              className={controlClassName}
            >
              <option value="">All areas</option>
              {areasQuery.data?.map((option) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </select>
          </label>

          <label className="block">
            <span className="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-slate-500">
              Work mode
            </span>
            <select
              value={workMode}
              onChange={(event) => updateFilters({ workMode: event.target.value || null })}
              className={controlClassName}
            >
              <option value="">Any work mode</option>
              {workModes.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>

          <label className="block">
            <span className="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-slate-500">
              Employment
            </span>
            <select
              value={employmentType}
              onChange={(event) => updateFilters({ employmentType: event.target.value || null })}
              className={controlClassName}
            >
              <option value="">Any type</option>
              {employmentTypes.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>

          <label className="block">
            <span className="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-slate-500">
              Experience
            </span>
            <select
              value={experience}
              onChange={(event) => updateFilters({ experience: event.target.value || null })}
              className={controlClassName}
            >
              <option value="">Any experience</option>
              {experienceBands.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>

          <label className="block">
            <span className="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-slate-500">
              Sort
            </span>
            <select
              value={sort}
              onChange={(event) =>
                updateFilters({ sort: event.target.value === "newest" ? null : event.target.value })
              }
              className={controlClassName}
            >
              {sortOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>

          <label className="block">
            <span className="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-slate-500">
              Add a skill
            </span>
            <select
              value=""
              onChange={(event) => addSkill(event.target.value)}
              disabled={skillsQuery.isPending}
              className={controlClassName}
            >
              <option value="">
                {skillsQuery.isPending ? "Loading skills…" : "Any skill"}
              </option>
              {skillsByCategory.map(([category, skills]) => (
                <optgroup key={category} label={category}>
                  {skills.map((skill) => (
                    <option key={skill.id} value={skill.name} disabled={selectedSkills.includes(skill.name)}>
                      {skill.name}
                    </option>
                  ))}
                </optgroup>
              ))}
            </select>
          </label>
        </div>

        {hasFilterOptionError ? (
          <p className="mt-3 text-xs text-amber-700">
            Some filter options didn&apos;t load.{" "}
            <button
              type="button"
              className="font-semibold underline underline-offset-2"
              onClick={() => {
                if (areasQuery.isError) {
                  void areasQuery.refetch();
                }
                if (skillsQuery.isError) {
                  void skillsQuery.refetch();
                }
              }}
            >
              Retry
            </button>
          </p>
        ) : null}

        {hasFilters ? (
          <div className="mt-4 flex flex-wrap items-center gap-2">
            <span className="text-xs font-semibold uppercase tracking-wide text-slate-500">
              Active
            </span>
            {keyword ? (
              <FilterChip
                label={`“${keyword}”`}
                onRemove={() => {
                  setKeywordInput("");
                  updateFilters({ q: null });
                }}
              />
            ) : null}
            {area ? <FilterChip label={area} onRemove={() => updateFilters({ area: null })} /> : null}
            {workMode ? (
              <FilterChip
                label={formatEnum(workMode) ?? workMode}
                onRemove={() => updateFilters({ workMode: null })}
              />
            ) : null}
            {employmentType ? (
              <FilterChip
                label={formatEnum(employmentType) ?? employmentType}
                onRemove={() => updateFilters({ employmentType: null })}
              />
            ) : null}
            {experienceLabel ? (
              <FilterChip
                label={experienceLabel}
                onRemove={() => updateFilters({ experience: null })}
              />
            ) : null}
            {selectedSkills.map((skill) => (
              <FilterChip key={skill} label={skill} onRemove={() => removeSkill(skill)} />
            ))}
            <button
              type="button"
              onClick={clearFilters}
              className="text-xs font-semibold text-brand-700 underline-offset-2 transition hover:text-brand-800 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"
            >
              Clear all
            </button>
          </div>
        ) : null}
      </div>

      <div className="mt-8 flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <p className="text-sm text-slate-600" role="status" aria-live="polite">
          {data
            ? `${data.totalElements} ${data.totalElements === 1 ? "role" : "roles"} found`
            : "Loading roles…"}
        </p>
        {data && data.totalPages > 1 ? (
          <p className="text-sm text-slate-500">
            Page {page + 1} of {data.totalPages}
          </p>
        ) : null}
      </div>

      {jobsQuery.isPending ? (
        <div className="mt-6 grid gap-5 lg:grid-cols-2">
          {Array.from({ length: 6 }, (_, index) => (
            <JobCardSkeleton key={index} />
          ))}
        </div>
      ) : null}

      {jobsQuery.isError ? (
        <ErrorState
          className="mt-6"
          title="Couldn't load the job board"
          description="The jobs API didn't respond. Make sure the backend is running on port 8080, then try again."
          onRetry={() => void jobsQuery.refetch()}
        />
      ) : null}

      {data && data.content.length === 0 ? (
        <EmptyState
          className="mt-6"
          icon={<SlidersHorizontal aria-hidden="true" className="h-8 w-8" />}
          title="No roles match those filters"
          description={
            hasFilters
              ? "Try a different area, work mode, or skill — or clear the filters to see every open role."
              : "No open roles are published yet. Check back once the job board has been seeded."
          }
          action={
            hasFilters ? (
              <Button variant="outline" size="sm" onClick={clearFilters}>
                Clear filters
              </Button>
            ) : undefined
          }
        />
      ) : null}

      {data && data.content.length > 0 ? (
        <div className="mt-6 grid gap-5 lg:grid-cols-2" aria-busy={jobsQuery.isFetching}>
          {data.content.map((job) => (
            <JobPreviewCard key={job.id} job={job} />
          ))}
        </div>
      ) : null}

      {data && data.totalPages > 1 ? (
        <nav
          aria-label="Job board pagination"
          className="mt-10 flex items-center justify-between gap-4 border-t border-slate-200 pt-6"
        >
          <Button
            variant="outline"
            size="sm"
            onClick={() => setPage(page - 1)}
            disabled={page <= 0}
          >
            <ChevronLeft aria-hidden="true" className="h-4 w-4" />
            Previous
          </Button>
          <p className="text-sm font-medium text-slate-600">
            Page {page + 1} of {data.totalPages}
          </p>
          <Button
            variant="outline"
            size="sm"
            onClick={() => setPage(page + 1)}
            disabled={!data.hasNext}
          >
            Next
            <ChevronRight aria-hidden="true" className="h-4 w-4" />
          </Button>
        </nav>
      ) : null}
    </section>
  );
}
