import {
  ArrowLeft,
  ArrowUpRight,
  BadgeCheck,
  Briefcase,
  Building2,
  CalendarDays,
  MapPin,
  ShieldCheck,
  Sparkles,
  Users,
} from "lucide-react";
import { Link, useParams } from "react-router-dom";
import { ApiError } from "@/api/client";
import { Badge } from "@/components/ui/badge";
import { buttonVariants } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState, ErrorState } from "@/components/ui/states";
import { FollowCompanyButton } from "@/features/candidate/components/follow-company-button";
import { useCompany } from "@/hooks/use-company";
import { useCompanyJobs } from "@/hooks/use-company-jobs";
import {
  formatDate,
  formatEmployeeCount,
  formatEnum,
  formatExperience,
  formatSalary,
} from "@/lib/format";
import { initials } from "@/lib/initials";
import { cn } from "@/lib/utils";

function ProfileSkeleton() {
  return (
    <div className="container-page py-12">
      <Skeleton className="h-4 w-32" />
      <div className="mt-8 flex items-center gap-4">
        <Skeleton className="h-16 w-16 rounded-2xl" />
        <div className="space-y-3">
          <Skeleton className="h-8 w-64" />
          <Skeleton className="h-4 w-48" />
        </div>
      </div>
      <div className="mt-10 grid gap-6 lg:grid-cols-[minmax(0,1fr)_320px]">
        <div className="space-y-6">
          <Skeleton className="h-40 w-full rounded-3xl" />
          <Skeleton className="h-64 w-full rounded-3xl" />
        </div>
        <Skeleton className="h-80 w-full rounded-3xl" />
      </div>
    </div>
  );
}

interface JobChip {
  label: string;
  tone?: "brand" | "fresher";
}

export function CompanyProfilePage() {
  const { companyId } = useParams<{ companyId: string }>();
  const companyQuery = useCompany(companyId);
  const jobsQuery = useCompanyJobs(companyId, 6);

  if (!companyId) {
    return <ErrorState className="container-page my-16" title="Startup not found" />;
  }

  if (companyQuery.isPending) {
    return <ProfileSkeleton />;
  }

  if (companyQuery.isError) {
    const isNotFound =
      companyQuery.error instanceof ApiError && companyQuery.error.status === 404;

    return (
      <section className="container-page py-16">
        {isNotFound ? (
          <EmptyState
            className="mx-auto max-w-2xl"
            icon={<Building2 aria-hidden="true" className="h-8 w-8" />}
            title="Startup not found"
            description="This company may have been removed or is no longer published in the directory."
            action={
              <Link to="/startups" className={buttonVariants({ variant: "brand" })}>
                Back to the directory
              </Link>
            }
          />
        ) : (
          <ErrorState
            className="mx-auto max-w-2xl"
            title="Couldn't load this startup"
            description="The company API didn't respond. Make sure the backend is running on port 8080, then try again."
            onRetry={() => void companyQuery.refetch()}
          />
        )}
      </section>
    );
  }

  const company = companyQuery.data;

  if (!company) {
    return <ErrorState className="container-page my-16" title="Couldn't load this startup" />;
  }

  const primaryLocation =
    company.locations.find((location) => location.headquarters) ?? company.locations[0];
  const locationLabel = primaryLocation
    ? [primaryLocation.area, primaryLocation.city].filter(Boolean).join(", ") || primaryLocation.city
    : "Pune";
  const founded = company.foundedYear ? String(company.foundedYear) : null;
  const teamSize = formatEmployeeCount(company.employeeCountMin, company.employeeCountMax);
  const snapshot = [
    { label: "Industry", value: company.industryName ?? "Not specified" },
    { label: "Founded", value: founded },
    { label: "Team size", value: teamSize ? `${teamSize} people` : null },
    { label: "Funding", value: formatEnum(company.fundingStage) },
    { label: "Company type", value: formatEnum(company.companyType) },
    { label: "Verification", value: formatEnum(company.verificationStatus) },
    { label: "Updated", value: formatDate(company.updatedAt) },
  ].filter((entry) => Boolean(entry.value));

  const jobs = jobsQuery.data?.content ?? [];

  return (
    <>
      <section className="border-b border-slate-200 bg-white/70">
        <div className="container-page py-10 sm:py-14">
          <Link
            to="/startups"
            className="inline-flex items-center gap-2 rounded text-sm font-semibold text-slate-600 transition hover:text-slate-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"
          >
            <ArrowLeft aria-hidden="true" className="h-4 w-4" />
            Back to directory
          </Link>

          <div className="mt-6 flex flex-col gap-6 lg:flex-row lg:items-start lg:justify-between">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-start">
              {company.logoUrl ? (
                <img
                  src={company.logoUrl}
                  alt=""
                  className="h-16 w-16 shrink-0 rounded-2xl border border-slate-100 bg-white object-cover"
                />
              ) : (
                <span
                  aria-hidden="true"
                  className="grid h-16 w-16 shrink-0 place-items-center rounded-2xl bg-slate-900 text-lg font-bold text-brand-300"
                >
                  {initials(company.name)}
                </span>
              )}

              <div>
                <div className="flex flex-wrap items-center gap-2">
                  <h1 className="text-3xl font-black tracking-tight text-slate-900 sm:text-4xl">
                    {company.name}
                  </h1>
                  {company.verificationStatus === "VERIFIED" ? (
                    <span className="inline-flex items-center gap-1 rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-semibold text-emerald-700">
                      <BadgeCheck aria-hidden="true" className="h-3.5 w-3.5" />
                      Verified
                    </span>
                  ) : null}
                </div>
                <p className="mt-2 text-sm text-slate-600">
                  {company.industryName ?? "Pune startup"}
                  {company.companyType ? ` · ${formatEnum(company.companyType)}` : ""}
                </p>
                <p className="mt-1 inline-flex items-center gap-1.5 text-sm text-slate-500">
                  <MapPin aria-hidden="true" className="h-3.5 w-3.5" />
                  {locationLabel}
                </p>
              </div>
            </div>

            <div className="flex flex-wrap gap-3">
              <FollowCompanyButton companyId={company.id} />
              {company.websiteUrl ? (
                <a
                  href={company.websiteUrl}
                  target="_blank"
                  rel="noreferrer noopener"
                  className={buttonVariants({ variant: "brand" })}
                >
                  Visit website
                  <ArrowUpRight aria-hidden="true" className="h-4 w-4" />
                </a>
              ) : null}
              <Link to="/jobs" className={buttonVariants({ variant: "outline" })}>
                Browse jobs
              </Link>
            </div>
          </div>
        </div>
      </section>

      <div className="container-page grid gap-6 py-10 lg:grid-cols-[minmax(0,1fr)_320px] lg:items-start">
        <div className="space-y-6">
          <section className="rounded-3xl border border-slate-200 bg-white p-6 sm:p-8">
            <h2 className="text-lg font-semibold text-slate-900">About</h2>
            <p className="mt-3 whitespace-pre-line text-sm leading-relaxed text-slate-600">
              {company.description ?? "This startup hasn't shared a description yet."}
            </p>

            <div className="mt-6 flex flex-wrap gap-2">
              <span className="inline-flex items-center gap-1.5 rounded-full bg-slate-100 px-3 py-1.5 text-xs font-semibold text-slate-700">
                <Briefcase aria-hidden="true" className="h-3.5 w-3.5" />
                {company.activeJobCount} active {company.activeJobCount === 1 ? "role" : "roles"}
              </span>
              {company.fresherJobCount > 0 ? (
                <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-50 px-3 py-1.5 text-xs font-semibold text-emerald-700">
                  <Sparkles aria-hidden="true" className="h-3.5 w-3.5" />
                  {company.fresherJobCount} fresher-friendly
                </span>
              ) : null}
              {founded ? (
                <span className="inline-flex items-center gap-1.5 rounded-full bg-slate-100 px-3 py-1.5 text-xs font-semibold text-slate-700">
                  <CalendarDays aria-hidden="true" className="h-3.5 w-3.5" />
                  Founded {founded}
                </span>
              ) : null}
              {teamSize ? (
                <span className="inline-flex items-center gap-1.5 rounded-full bg-slate-100 px-3 py-1.5 text-xs font-semibold text-slate-700">
                  <Users aria-hidden="true" className="h-3.5 w-3.5" />
                  {teamSize} people
                </span>
              ) : null}
            </div>
          </section>

          <section className="rounded-3xl border border-slate-200 bg-white p-6 sm:p-8">
            <div className="flex items-center justify-between gap-4">
              <h2 className="text-lg font-semibold text-slate-900">Locations</h2>
              <span className="text-xs font-medium text-slate-500">
                {company.locations.length}{" "}
                {company.locations.length === 1 ? "location" : "locations"}
              </span>
            </div>

            {company.locations.length === 0 ? (
              <p className="mt-3 text-sm text-slate-600">
                No office locations have been published yet.
              </p>
            ) : (
              <ul className="mt-4 space-y-3">
                {company.locations.map((location) => {
                  const address =
                    [location.addressLine, location.area, location.city, location.state, location.postalCode]
                      .filter(Boolean)
                      .join(", ") || location.city;
                  const name =
                    location.locationName ??
                    [location.area, location.city].filter(Boolean).join(", ") ??
                    "Pune office";

                  return (
                    <li
                      key={location.id}
                      className="rounded-2xl border border-slate-200 bg-slate-50/60 p-4"
                    >
                      <div className="flex flex-wrap items-start justify-between gap-2">
                        <div className="min-w-0">
                          <p className="text-sm font-semibold text-slate-900">{name}</p>
                          <p className="mt-1 text-sm text-slate-600">{address}</p>
                        </div>
                        <div className="flex flex-wrap items-center gap-2">
                          {location.headquarters ? (
                            <span className="rounded-full bg-slate-900 px-2.5 py-1 text-[11px] font-semibold uppercase tracking-wide text-white">
                              HQ
                            </span>
                          ) : null}
                          <span className="rounded-full border border-slate-200 bg-white px-2.5 py-1 text-[11px] font-medium text-slate-600">
                            {formatEnum(location.locationType) ?? "Office"}
                          </span>
                        </div>
                      </div>
                      <div className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-slate-500">
                        {location.latitude !== null && location.longitude !== null ? (
                          <span>
                            {location.latitude.toFixed(4)}, {location.longitude.toFixed(4)}
                          </span>
                        ) : null}
                        {location.geocodeStatus !== "VERIFIED" ? (
                          <span className="text-amber-700">
                            {formatEnum(location.geocodeStatus) ?? "Approximate"} pin
                          </span>
                        ) : null}
                      </div>
                    </li>
                  );
                })}
              </ul>
            )}
          </section>

          <section className="rounded-3xl border border-slate-200 bg-white p-6 sm:p-8">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <h2 className="text-lg font-semibold text-slate-900">Open roles</h2>
              {jobsQuery.data && jobsQuery.data.totalElements > 0 ? (
                <span className="text-xs font-medium text-slate-500">
                  Showing {jobs.length} of {jobsQuery.data.totalElements}
                </span>
              ) : null}
            </div>

            {jobsQuery.isPending ? (
              <div className="mt-4 space-y-3">
                <Skeleton className="h-20 w-full rounded-2xl" />
                <Skeleton className="h-20 w-full rounded-2xl" />
              </div>
            ) : null}

            {jobsQuery.isError ? (
              <ErrorState
                className="mt-4 border-solid"
                title="Couldn't load open roles"
                description="The jobs API didn't respond. Your company details are still available."
                onRetry={() => void jobsQuery.refetch()}
              />
            ) : null}

            {jobsQuery.data && jobs.length === 0 ? (
              <p className="mt-3 rounded-2xl border border-dashed border-slate-300 bg-slate-50/60 px-4 py-6 text-center text-sm text-slate-600">
                No active roles right now. Check back soon.
              </p>
            ) : null}

            {jobs.length > 0 ? (
              <ul className="mt-4 space-y-3">
                {jobs.map((job) => {
                  const chips: JobChip[] = [];
                  const mode = formatEnum(job.workMode);
                  const type = formatEnum(job.employmentType);
                  const experience = formatExperience(job.experienceMinYears, job.experienceMaxYears);
                  const salary = formatSalary(
                    job.salaryMin,
                    job.salaryMax,
                    job.salaryCurrency,
                    job.salaryPeriod,
                  );
                  const posted = formatDate(job.postedAt);

                  if (mode) {
                    chips.push({ label: mode });
                  }
                  if (type) {
                    chips.push({ label: type });
                  }
                  if (experience) {
                    chips.push({ label: experience });
                  }
                  if (salary) {
                    chips.push({ label: salary });
                  }
                  if (job.fresherEligible) {
                    chips.push({ label: "Fresher-friendly", tone: "fresher" });
                  }

                  return (
                    <li
                      key={job.id}
                      className="rounded-2xl border border-slate-200 bg-slate-50/60 p-4"
                    >
                      <div className="flex flex-wrap items-start justify-between gap-3">
                        <h3 className="text-sm font-semibold text-slate-900">
                          <Link
                            to={`/jobs/${job.id}`}
                            className="rounded transition-colors hover:text-brand-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"
                          >
                            {job.title}
                          </Link>
                        </h3>
                        {posted ? (
                          <span className="text-xs text-slate-500">Posted {posted}</span>
                        ) : null}
                      </div>
                      <div className="mt-3 flex flex-wrap gap-1.5">
                        {chips.map((chip) => (
                          <span
                            key={chip.label}
                            className={cn(
                              "rounded-full px-2.5 py-1 text-[11px] font-medium",
                              chip.tone === "fresher"
                                ? "bg-emerald-50 text-emerald-700"
                                : "bg-white text-slate-600 ring-1 ring-slate-200",
                            )}
                          >
                            {chip.label}
                          </span>
                        ))}
                      </div>
                    </li>
                  );
                })}
              </ul>
            ) : null}

            {jobsQuery.data && jobsQuery.data.totalElements > jobs.length ? (
              <Link
                to="/jobs"
                className="mt-4 inline-flex items-center gap-1.5 rounded text-sm font-semibold text-brand-700 transition hover:text-brand-800 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"
              >
                See all roles
                <ArrowUpRight aria-hidden="true" className="h-4 w-4" />
              </Link>
            ) : null}
          </section>
        </div>

        <aside className="space-y-6">
          <section className="rounded-3xl border border-slate-200 bg-white p-6">
            <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-500">
              At a glance
            </h2>
            <dl className="mt-4 space-y-3 text-sm">
              {snapshot.map((entry) => (
                <div key={entry.label} className="flex items-start justify-between gap-4">
                  <dt className="text-slate-500">{entry.label}</dt>
                  <dd className="text-right font-medium text-slate-900">{entry.value}</dd>
                </div>
              ))}
            </dl>
          </section>

          <section className="rounded-3xl border border-slate-200 bg-white p-6">
            <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-500">
              Sources
            </h2>
            {company.sources.length === 0 ? (
              <p className="mt-3 text-sm text-slate-600">No sources attached yet.</p>
            ) : (
              <ul className="mt-4 space-y-4">
                {company.sources.map((source) => {
                  const verified = formatDate(source.lastVerifiedAt);

                  return (
                    <li
                      key={`${source.sourceName}-${source.externalId ?? source.sourceUrl ?? "source"}`}
                      className="flex items-start justify-between gap-3"
                    >
                      <div className="min-w-0">
                        <p className="text-sm font-medium text-slate-900">{source.sourceName}</p>
                        {verified ? (
                          <p className="mt-0.5 text-xs text-slate-500">Verified {verified}</p>
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
            <h2 className="mt-4 text-sm font-semibold text-white">Source-backed details</h2>
            <p className="mt-2 text-xs leading-relaxed text-slate-400">
              Company facts come from attached sources. Details that couldn&apos;t be verified are left
              blank rather than guessed.
            </p>
          </section>

          <Badge className="w-full justify-center py-2">Pune startup directory</Badge>
        </aside>
      </div>
    </>
  );
}
