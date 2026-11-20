import {
  AlertTriangle,
  ArrowLeft,
  ArrowUpRight,
  BadgeCheck,
  Briefcase,
  Building2,
  CalendarDays,
  Clock,
  MapPin,
  ShieldCheck,
  Sparkles,
} from "lucide-react";
import { Link, useParams } from "react-router-dom";
import { ApiError } from "@/api/client";
import { Badge } from "@/components/ui/badge";
import { buttonVariants } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState, ErrorState } from "@/components/ui/states";
import { SaveJobButton } from "@/features/candidate/components/save-job-button";
import { TrackApplicationButton } from "@/features/candidate/components/track-application-button";
import { useJob } from "@/hooks/use-job";
import {
  formatDate,
  formatEnum,
  formatExperience,
  formatRelativeDate,
  formatSalary,
} from "@/lib/format";
import { initials } from "@/lib/initials";

const STALE_AFTER_DAYS = 30;

function DetailSkeleton() {
  return (
    <div className="container-page py-12">
      <Skeleton className="h-4 w-28" />
      <div className="mt-8 flex items-center gap-4">
        <Skeleton className="h-14 w-14 rounded-2xl" />
        <div className="space-y-3">
          <Skeleton className="h-8 w-72" />
          <Skeleton className="h-4 w-52" />
        </div>
      </div>
      <div className="mt-10 grid gap-6 lg:grid-cols-[minmax(0,1fr)_340px]">
        <div className="space-y-6">
          <Skeleton className="h-72 w-full rounded-3xl" />
          <Skeleton className="h-44 w-full rounded-3xl" />
        </div>
        <div className="space-y-6">
          <Skeleton className="h-64 w-full rounded-3xl" />
          <Skeleton className="h-44 w-full rounded-3xl" />
        </div>
      </div>
    </div>
  );
}

function DetailRow({ label, value }: { label: string; value: string | null | undefined }) {
  if (!value) {
    return null;
  }

  return (
    <div className="flex items-start justify-between gap-4">
      <dt className="text-slate-500">{label}</dt>
      <dd className="text-right font-medium text-slate-900">{value}</dd>
    </div>
  );
}

export function JobDetailPage() {
  const { jobId } = useParams<{ jobId: string }>();
  const jobQuery = useJob(jobId);

  if (!jobId) {
    return <ErrorState className="container-page my-16" title="Role not found" />;
  }

  if (jobQuery.isPending) {
    return <DetailSkeleton />;
  }

  if (jobQuery.isError) {
    const isNotFound = jobQuery.error instanceof ApiError && jobQuery.error.status === 404;

    return (
      <section className="container-page py-16">
        {isNotFound ? (
          <EmptyState
            className="mx-auto max-w-2xl"
            icon={<Briefcase aria-hidden="true" className="h-8 w-8" />}
            title="Role not found"
            description="This role may have closed, expired, or been removed from the job board."
            action={
              <Link to="/jobs" className={buttonVariants({ variant: "brand" })}>
                Back to all roles
              </Link>
            }
          />
        ) : (
          <ErrorState
            className="mx-auto max-w-2xl"
            title="Couldn't load this role"
            description="The jobs API didn't respond. Make sure the backend is running on port 8080, then try again."
            onRetry={() => void jobQuery.refetch()}
          />
        )}
      </section>
    );
  }

  const job = jobQuery.data;

  if (!job) {
    return <ErrorState className="container-page my-16" title="Couldn't load this role" />;
  }

  const company = job.company;
  const location = job.location;
  const locationLabel = location
    ? [location.area, location.city].filter(Boolean).join(", ") || location.city
    : "Pune";
  const address = location
    ? [location.addressLine, location.area, location.city, location.state, location.postalCode]
        .filter(Boolean)
        .join(", ")
    : null;

  const primarySource =
    job.sources.find((source) => source.applicationUrl) ??
    job.sources.find((source) => source.sourceUrl);
  const applyUrl = primarySource?.applicationUrl ?? primarySource?.sourceUrl ?? null;
  const posted = formatDate(job.postedAt);
  const verifiedDate = formatDate(job.lastVerifiedAt);
  const verifiedRelative = formatRelativeDate(job.lastVerifiedAt);
  const expires = formatDate(job.expiresAt);
  const salary = formatSalary(job.salaryMin, job.salaryMax, job.salaryCurrency, job.salaryPeriod);
  const experience = formatExperience(job.experienceMinYears, job.experienceMaxYears);
  const verifiedTime = job.lastVerifiedAt ? new Date(job.lastVerifiedAt).getTime() : null;
  const isStale =
    verifiedTime === null ||
    Number.isNaN(verifiedTime) ||
    Date.now() - verifiedTime > STALE_AFTER_DAYS * 86_400_000;

  const requiredSkills = job.skills.filter((skill) => skill.requirementType === "REQUIRED");
  const preferredSkills = job.skills.filter((skill) => skill.requirementType === "PREFERRED");
  const otherSkills = job.skills.filter(
    (skill) => skill.requirementType !== "REQUIRED" && skill.requirementType !== "PREFERRED",
  );
  const skillGroups = [
    { label: "Required", skills: requiredSkills },
    { label: "Preferred", skills: preferredSkills },
    { label: "Also listed", skills: otherSkills },
  ].filter((group) => group.skills.length > 0);

  const chips = [
    formatEnum(job.employmentType),
    formatEnum(job.workMode),
    experience,
  ].filter((chip): chip is string => Boolean(chip));

  return (
    <>
      <section className="border-b border-slate-200 bg-white/70">
        <div className="container-page py-10 sm:py-14">
          <Link
            to="/jobs"
            className="inline-flex items-center gap-2 rounded text-sm font-semibold text-slate-600 transition hover:text-slate-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"
          >
            <ArrowLeft aria-hidden="true" className="h-4 w-4" />
            Back to all roles
          </Link>

          <div className="mt-6 flex flex-col gap-6 lg:flex-row lg:items-start lg:justify-between">
            <div>
              {company ? (
                <Link
                  to={`/startups/${company.id}`}
                  className="inline-flex items-center gap-2 rounded text-sm font-medium text-slate-600 transition hover:text-brand-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"
                >
                  {company.logoUrl ? (
                    <img
                      src={company.logoUrl}
                      alt=""
                      className="h-7 w-7 rounded-lg border border-slate-100 bg-white object-cover"
                    />
                  ) : (
                    <span
                      aria-hidden="true"
                      className="grid h-7 w-7 place-items-center rounded-lg bg-slate-900 text-[10px] font-bold text-brand-300"
                    >
                      {initials(company.name)}
                    </span>
                  )}
                  {company.name}
                  {company.verificationStatus === "VERIFIED" ? (
                    <BadgeCheck aria-label="Verified company" className="h-3.5 w-3.5 text-emerald-600" />
                  ) : null}
                </Link>
              ) : null}

              <h1 className="mt-3 text-3xl font-black tracking-tight text-slate-900 sm:text-4xl">
                {job.title}
              </h1>

              <div className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-2 text-sm text-slate-600">
                <span className="inline-flex items-center gap-1.5">
                  <MapPin aria-hidden="true" className="h-3.5 w-3.5" />
                  {locationLabel}
                </span>
                {posted ? (
                  <span className="inline-flex items-center gap-1.5">
                    <CalendarDays aria-hidden="true" className="h-3.5 w-3.5" />
                    Posted {posted}
                  </span>
                ) : null}
                {verifiedRelative ? (
                  <span className="inline-flex items-center gap-1.5">
                    <Clock aria-hidden="true" className="h-3.5 w-3.5" />
                    Checked {verifiedRelative}
                  </span>
                ) : null}
              </div>

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
            </div>

            <div className="flex shrink-0 flex-col items-start gap-2 lg:items-end">
              <p className="text-lg font-bold text-slate-900">{salary ?? "Salary not listed"}</p>
              {applyUrl ? (
                <a
                  href={applyUrl}
                  target="_blank"
                  rel="noreferrer noopener"
                  className={buttonVariants({ variant: "brand", size: "lg" })}
                >
                  Apply on {primarySource?.sourceName ?? "source"}
                  <ArrowUpRight aria-hidden="true" className="h-4 w-4" />
                </a>
              ) : (
                <span className="text-sm text-slate-500">No application link available</span>
              )}
              <div className="flex flex-wrap gap-2 lg:justify-end">
                <SaveJobButton jobId={job.id} />
                <TrackApplicationButton jobId={job.id} />
              </div>
              <p className="text-xs text-slate-500">Opens the original listing in a new tab.</p>
            </div>
          </div>
        </div>
      </section>

      <div className="container-page grid gap-6 py-10 lg:grid-cols-[minmax(0,1fr)_340px] lg:items-start">
        <div className="space-y-6">
          {isStale ? (
            <div
              role="note"
              className="flex gap-3 rounded-3xl border border-amber-200 bg-amber-50 p-5 text-sm text-amber-900"
            >
              <AlertTriangle aria-hidden="true" className="mt-0.5 h-5 w-5 shrink-0" />
              <div>
                <p className="font-semibold">This listing needs a fresh check</p>
                <p className="mt-1 leading-relaxed text-amber-800">
                  {verifiedDate
                    ? `It was last verified on ${verifiedDate}.`
                    : "It has no verification date yet."}{" "}
                  Confirm the role is still open on the source before applying.
                </p>
              </div>
            </div>
          ) : null}

          <section className="rounded-3xl border border-slate-200 bg-white p-6 sm:p-8">
            <h2 className="text-lg font-semibold text-slate-900">About the role</h2>
            <p className="mt-3 whitespace-pre-line text-sm leading-relaxed text-slate-600">
              {job.description ?? "This role doesn't have a description yet."}
            </p>
          </section>

          <section className="rounded-3xl border border-slate-200 bg-white p-6 sm:p-8">
            <h2 className="text-lg font-semibold text-slate-900">Skills</h2>
            {skillGroups.length === 0 ? (
              <p className="mt-3 text-sm text-slate-600">No skills have been tagged for this role.</p>
            ) : (
              <div className="mt-4 space-y-5">
                {skillGroups.map((group) => (
                  <div key={group.label}>
                    <h3 className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                      {group.label}
                    </h3>
                    <ul className="mt-2 flex flex-wrap gap-2">
                      {group.skills.map((skill) => (
                        <li
                          key={skill.skillId}
                          className={
                            group.label === "Required"
                              ? "rounded-full bg-slate-900 px-3 py-1.5 text-xs font-medium text-white"
                              : "rounded-full bg-slate-100 px-3 py-1.5 text-xs font-medium text-slate-700"
                          }
                        >
                          {skill.skillName}
                          {skill.category ? (
                            <span className="ml-1.5 text-[10px] font-normal opacity-70">
                              {skill.category}
                            </span>
                          ) : null}
                        </li>
                      ))}
                    </ul>
                  </div>
                ))}
              </div>
            )}
          </section>
        </div>

        <aside className="space-y-6">
          <section className="rounded-3xl border border-slate-200 bg-white p-6">
            <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-500">
              Role details
            </h2>
            <dl className="mt-4 space-y-3 text-sm">
              <DetailRow label="Employment" value={formatEnum(job.employmentType)} />
              <DetailRow label="Work mode" value={formatEnum(job.workMode)} />
              <DetailRow label="Experience" value={experience} />
              <DetailRow label="Salary" value={salary} />
              <DetailRow label="Fresher-friendly" value={job.fresherEligible ? "Yes" : "No"} />
              <DetailRow label="Posted" value={posted} />
              <DetailRow label="Expires" value={expires} />
              <DetailRow label="Last checked" value={verifiedDate} />
            </dl>
          </section>

          <section className="rounded-3xl border border-slate-200 bg-white p-6">
            <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-500">
              Location
            </h2>
            <p className="mt-3 flex items-start gap-2 text-sm font-medium text-slate-900">
              <MapPin aria-hidden="true" className="mt-0.5 h-4 w-4 shrink-0 text-slate-400" />
              {location?.locationName ?? locationLabel}
            </p>
            {address ? <p className="mt-2 text-sm leading-relaxed text-slate-600">{address}</p> : null}
            {location && location.geocodeStatus !== "VERIFIED" ? (
              <p className="mt-2 text-xs text-amber-700">Approximate location — not shown as a map pin.</p>
            ) : null}
          </section>

          {company ? (
            <section className="rounded-3xl border border-slate-200 bg-white p-6">
              <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-500">
                The company
              </h2>
              <div className="mt-4 flex items-center gap-3">
                {company.logoUrl ? (
                  <img
                    src={company.logoUrl}
                    alt=""
                    className="h-11 w-11 rounded-xl border border-slate-100 bg-white object-cover"
                  />
                ) : (
                  <span
                    aria-hidden="true"
                    className="grid h-11 w-11 place-items-center rounded-xl bg-slate-900 text-xs font-bold text-brand-300"
                  >
                    {initials(company.name)}
                  </span>
                )}
                <div className="min-w-0">
                  <p className="truncate text-sm font-semibold text-slate-900">{company.name}</p>
                  <p className="text-xs text-slate-500">{company.industryName ?? "Pune startup"}</p>
                </div>
              </div>
              <div className="mt-4 flex flex-wrap gap-2">
                <Link
                  to={`/startups/${company.id}`}
                  className={buttonVariants({ variant: "outline", size: "sm" })}
                >
                  <Building2 aria-hidden="true" className="h-4 w-4" />
                  View profile
                </Link>
                {company.websiteUrl ? (
                  <a
                    href={company.websiteUrl}
                    target="_blank"
                    rel="noreferrer noopener"
                    className={buttonVariants({ variant: "ghost", size: "sm" })}
                  >
                    Website
                    <ArrowUpRight aria-hidden="true" className="h-4 w-4" />
                  </a>
                ) : null}
              </div>
            </section>
          ) : null}

          <section className="rounded-3xl border border-slate-200 bg-white p-6">
            <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-500">Sources</h2>
            {job.sources.length === 0 ? (
              <p className="mt-3 text-sm text-slate-600">No source links attached yet.</p>
            ) : (
              <ul className="mt-4 space-y-4">
                {job.sources.map((source, index) => {
                  const checked = formatDate(source.lastCheckedAt);

                  return (
                    <li
                      key={`${source.sourceName}-${source.sourceUrl}-${index}`}
                      className="flex items-start justify-between gap-3"
                    >
                      <div className="min-w-0">
                        <p className="text-sm font-medium text-slate-900">{source.sourceName}</p>
                        {checked ? (
                          <p className="mt-0.5 text-xs text-slate-500">Checked {checked}</p>
                        ) : null}
                      </div>
                      {source.sourceUrl ? (
                        <a
                          href={source.sourceUrl}
                          target="_blank"
                          rel="noreferrer noopener"
                          className="inline-flex shrink-0 items-center gap-1 rounded text-xs font-semibold text-brand-700 transition hover:text-brand-800 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"
                        >
                          Open
                          <ArrowUpRight aria-hidden="true" className="h-3.5 w-3.5" />
                        </a>
                      ) : null}
                    </li>
                  );
                })}
              </ul>
            )}
          </section>

          <section className="rounded-3xl border border-slate-200 bg-slate-900 p-6 text-slate-200">
            <span className="grid h-10 w-10 place-items-center rounded-xl bg-white/10 text-brand-300">
              <ShieldCheck aria-hidden="true" className="h-5 w-5" />
            </span>
            <h2 className="mt-4 text-sm font-semibold text-white">Apply on the original site</h2>
            <p className="mt-2 text-xs leading-relaxed text-slate-400">
              Pune Startup Arena links to the source listing and never takes applications itself.
            </p>
          </section>

          <Badge className="w-full justify-center py-2">Verified job listing</Badge>
        </aside>
      </div>
    </>
  );
}
