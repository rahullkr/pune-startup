import { ArrowUpRight, BadgeCheck, Briefcase, MapPin, Sparkles } from "lucide-react";
import { Link } from "react-router-dom";
import type { JobSummary } from "@/features/jobs/types";
import {
  formatEnum,
  formatExperience,
  formatRelativeDate,
  formatSalary,
} from "@/lib/format";
import { initials } from "@/lib/initials";
import { cn } from "@/lib/utils";

export function JobPreviewCard({ job }: { job: JobSummary }) {
  const company = job.company;
  const location = job.location;
  const locationLabel = location
    ? [location.area, location.city].filter(Boolean).join(", ")
    : "Pune";
  const posted = formatRelativeDate(job.postedAt);
  const salary = formatSalary(job.salaryMin, job.salaryMax, job.salaryCurrency, job.salaryPeriod);
  const experience = formatExperience(job.experienceMinYears, job.experienceMaxYears);
  const chips = [
    formatEnum(job.workMode),
    formatEnum(job.employmentType),
    experience,
  ].filter((chip): chip is string => Boolean(chip));

  return (
    <article className="group flex h-full flex-col rounded-3xl border border-slate-200 bg-white p-5 transition hover:-translate-y-0.5 hover:border-slate-300 hover:shadow-lg hover:shadow-slate-900/5">
      <div className="flex items-start gap-3">
        {company?.logoUrl ? (
          <img
            src={company.logoUrl}
            alt=""
            className="h-11 w-11 shrink-0 rounded-xl border border-slate-100 bg-white object-cover"
          />
        ) : (
          <span
            aria-hidden="true"
            className="grid h-11 w-11 shrink-0 place-items-center rounded-xl bg-slate-900 text-xs font-bold text-brand-300"
          >
            {company ? initials(company.name) : <Briefcase className="h-4 w-4" />}
          </span>
        )}

        <div className="min-w-0 flex-1">
          <h3 className="text-base font-semibold leading-snug text-slate-900">
            <Link
              to={`/jobs/${job.id}`}
              className="rounded transition-colors group-hover:text-brand-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"
            >
              {job.title}
            </Link>
          </h3>
          {company ? (
            <p className="mt-1 flex flex-wrap items-center gap-1.5 text-sm text-slate-600">
              <Link
                to={`/startups/${company.id}`}
                className="rounded font-medium transition-colors hover:text-brand-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"
              >
                {company.name}
              </Link>
              {company.verificationStatus === "VERIFIED" ? (
                <BadgeCheck
                  aria-label="Verified company"
                  className="h-3.5 w-3.5 text-emerald-600"
                />
              ) : null}
            </p>
          ) : null}
        </div>

        {posted ? (
          <span className="shrink-0 text-right text-[11px] font-medium text-slate-500">
            {posted}
          </span>
        ) : null}
      </div>

      <p className="mt-3 flex items-center gap-1.5 text-xs text-slate-500">
        <MapPin aria-hidden="true" className="h-3.5 w-3.5" />
        {locationLabel}
      </p>

      <div className="mt-4 flex flex-wrap gap-1.5">
        {chips.map((chip) => (
          <span
            key={chip}
            className="rounded-full bg-slate-100 px-2.5 py-1 text-[11px] font-medium text-slate-600"
          >
            {chip}
          </span>
        ))}
        {job.fresherEligible ? (
          <span className="inline-flex items-center gap-1 rounded-full bg-emerald-50 px-2.5 py-1 text-[11px] font-medium text-emerald-700">
            <Sparkles aria-hidden="true" className="h-3 w-3" />
            Fresher-friendly
          </span>
        ) : null}
      </div>

      <div className="mt-5 flex items-end justify-between gap-3 border-t border-slate-100 pt-4">
        <p className={cn("text-sm font-semibold", salary ? "text-slate-900" : "text-slate-400")}>
          {salary ?? "Salary not listed"}
        </p>
        <Link
          to={`/jobs/${job.id}`}
          className="inline-flex items-center gap-1 rounded text-xs font-semibold text-brand-700 transition hover:text-brand-800 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"
        >
          View role
          <ArrowUpRight aria-hidden="true" className="h-3.5 w-3.5" />
        </Link>
      </div>
    </article>
  );
}
