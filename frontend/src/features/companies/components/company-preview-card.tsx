import { ArrowUpRight, Briefcase, Sparkles } from "lucide-react";
import { Link } from "react-router-dom";
import type { CompanySummary } from "@/features/companies/types";
import { initials } from "@/lib/initials";

export function CompanyPreviewCard({ company }: { company: CompanySummary }) {
  const location = company.areas.length > 0 ? company.areas.join(" · ") : "Pune";
  const jobLabel = company.activeJobCount === 1 ? "open role" : "open roles";

  return (
    <Link
      to={`/startups/${company.id}`}
      className="group flex h-full flex-col rounded-3xl border border-slate-200 bg-white p-6 transition hover:-translate-y-0.5 hover:border-brand-200 hover:shadow-lg hover:shadow-slate-900/5 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"
    >
      <div className="flex items-start justify-between gap-4">
        <div className="flex min-w-0 items-center gap-3">
          {company.logoUrl ? (
            <img
              src={company.logoUrl}
              alt=""
              loading="lazy"
              className="h-11 w-11 rounded-xl border border-slate-100 bg-white object-cover"
            />
          ) : (
            <span
              aria-hidden="true"
              className="grid h-11 w-11 shrink-0 place-items-center rounded-xl bg-slate-900 text-sm font-bold text-brand-300"
            >
              {initials(company.name)}
            </span>
          )}
          <div className="min-w-0">
            <h3 className="truncate text-base font-semibold text-slate-900">{company.name}</h3>
            <p className="truncate text-sm text-slate-500">{company.industryName ?? "Pune startup"}</p>
          </div>
        </div>
        <ArrowUpRight
          aria-hidden="true"
          className="h-4 w-4 shrink-0 text-slate-400 transition group-hover:-translate-y-0.5 group-hover:translate-x-0.5 group-hover:text-brand-600"
        />
      </div>

      <p className="mt-4 text-sm text-slate-500">{location}</p>

      <div className="mt-auto flex flex-wrap items-center gap-2 pt-6 text-xs font-medium">
        <span className="inline-flex items-center gap-1.5 rounded-full bg-slate-100 px-2.5 py-1 text-slate-700">
          <Briefcase aria-hidden="true" className="h-3.5 w-3.5" />
          {company.activeJobCount} {jobLabel}
        </span>
        {company.fresherJobCount > 0 ? (
          <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-50 px-2.5 py-1 text-emerald-700">
            <Sparkles aria-hidden="true" className="h-3.5 w-3.5" />
            {company.fresherJobCount} fresher-friendly
          </span>
        ) : null}
      </div>
    </Link>
  );
}
