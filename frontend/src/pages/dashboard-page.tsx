import {
  ArrowUpRight,
  Bookmark,
  Briefcase,
  Building2,
  CalendarDays,
  Check,
  ClipboardList,
  ExternalLink,
  Pencil,
  Sparkles,
  Trash2,
  X,
} from "lucide-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState, type FormEvent, type ReactNode } from "react";
import { Link } from "react-router-dom";
import { ApiError } from "@/api/client";
import { Badge } from "@/components/ui/badge";
import { Button, buttonVariants } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState, ErrorState, LoadingState } from "@/components/ui/states";
import { SkillsEditor } from "@/features/candidate/components/skills-editor";
import { ResumeManager } from "@/features/candidate/components/resume-manager";
import { TrackApplicationButton } from "@/features/candidate/components/track-application-button";
import { useAuth } from "@/features/auth/auth-context";
import {
  deleteApplication,
  getApplication,
  getApplications,
  getDashboard,
  getFollowedCompanies,
  getJobMatches,
  getSavedJobs,
  unfollowCompany,
  unsaveJob,
  updateApplication,
} from "@/features/candidate/api";
import type { ApplicationStatus, ApplicationSummary } from "@/features/candidate/types";
import { formatDate, formatEnum, formatRelativeDate } from "@/lib/format";
import { initials } from "@/lib/initials";
import { cn } from "@/lib/utils";

const applicationStatuses: ApplicationStatus[] = [
  "SAVED",
  "APPLIED",
  "ASSESSMENT",
  "INTERVIEW",
  "OFFER",
  "REJECTED",
  "ACCEPTED",
  "DECLINED",
  "WITHDRAWN",
];

function statusClasses(status: ApplicationStatus): string {
  switch (status) {
    case "OFFER":
    case "ACCEPTED":
      return "border-emerald-200 bg-emerald-50 text-emerald-700";
    case "INTERVIEW":
    case "ASSESSMENT":
      return "border-sky-200 bg-sky-50 text-sky-700";
    case "REJECTED":
    case "DECLINED":
    case "WITHDRAWN":
      return "border-slate-200 bg-slate-100 text-slate-600";
    case "SAVED":
      return "border-amber-200 bg-amber-50 text-amber-700";
    default:
      return "border-brand-200 bg-brand-50 text-brand-700";
  }
}

function StatCard({
  label,
  value,
  icon,
  href,
}: {
  label: string;
  value: number;
  icon: ReactNode;
  href: string;
}) {
  return (
    <a
      href={href}
      className="rounded-3xl border border-slate-200 bg-white p-5 transition hover:-translate-y-0.5 hover:border-slate-300 hover:shadow-sm"
    >
      <span className="grid h-10 w-10 place-items-center rounded-xl bg-slate-900 text-brand-300">
        {icon}
      </span>
      <p className="mt-4 text-2xl font-black text-slate-900">{value}</p>
      <p className="mt-1 text-sm font-medium text-slate-500">{label}</p>
    </a>
  );
}

function ApplicationRow({
  application,
  editing,
  editingStatus,
  editingAppliedAt,
  editingNotes,
  editingUrl,
  onEdit,
  onCancel,
  onStatusChange,
  onAppliedAtChange,
  onNotesChange,
  onUrlChange,
  onSave,
  onDelete,
  saving,
  deleting,
  updateError,
}: {
  application: ApplicationSummary;
  editing: boolean;
  editingStatus: ApplicationStatus;
  editingAppliedAt: string;
  editingNotes: string;
  editingUrl: string;
  onEdit: () => void;
  onCancel: () => void;
  onStatusChange: (status: ApplicationStatus) => void;
  onAppliedAtChange: (date: string) => void;
  onNotesChange: (notes: string) => void;
  onUrlChange: (url: string) => void;
  onSave: (event: FormEvent<HTMLFormElement>) => void;
  onDelete: () => void;
  saving: boolean;
  deleting: boolean;
  updateError: string | null;
}) {
  const [showHistory, setShowHistory] = useState(false);
  const historyQuery = useQuery({
    queryKey: ["application", application.id],
    queryFn: ({ signal }) => getApplication(application.id, signal),
    enabled: showHistory,
  });
  const company = application.job.company;
  const location = application.job.location;
  const locationLabel = location
    ? [location.area, location.city].filter(Boolean).join(", ") || location.city
    : null;
  const applied = formatDate(application.appliedAt);
  const updated = formatRelativeDate(application.updatedAt);

  return (
    <li className="rounded-2xl border border-slate-200 bg-slate-50/60 p-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <Link
            to={`/jobs/${application.job.id}`}
            className="text-sm font-semibold text-slate-900 transition hover:text-brand-700"
          >
            {application.job.title}
          </Link>
          <p className="mt-1 text-xs text-slate-500">
            {company?.name ?? "Company"}
            {locationLabel ? ` · ${locationLabel}` : ""}
          </p>
        </div>
        <Badge className={cn("shrink-0", statusClasses(application.status))}>
          {formatEnum(application.status)}
        </Badge>
      </div>

      <div className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-slate-500">
        {applied ? <span>Applied {applied}</span> : <span>Not applied yet</span>}
        {updated ? <span>Updated {updated}</span> : null}
        {application.notes ? <span className="text-slate-600">{application.notes}</span> : null}
      </div>

      {editing ? (
        <form className="mt-4 space-y-3 rounded-2xl border border-slate-200 bg-white p-4" onSubmit={onSave}>
          <div className="grid gap-3 sm:grid-cols-3">
            <label className="text-xs font-semibold text-slate-600">
              Status
              <select
                value={editingStatus}
                onChange={(event) => onStatusChange(event.target.value as ApplicationStatus)}
                className="mt-1 h-10 w-full rounded-xl border border-slate-300 bg-white px-3 text-sm font-normal text-slate-900 outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-200"
              >
                {applicationStatuses.map((status) => (
                  <option key={status} value={status}>
                    {formatEnum(status)}
                  </option>
                ))}
              </select>
            </label>
            <label className="text-xs font-semibold text-slate-600">
              Applied on
              <input
                type="date"
                value={editingAppliedAt}
                onChange={(event) => onAppliedAtChange(event.target.value)}
                className="mt-1 h-10 w-full rounded-xl border border-slate-300 bg-white px-3 text-sm font-normal text-slate-900 outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-200"
              />
            </label>
            <label className="text-xs font-semibold text-slate-600">
              Application link
              <input
                type="url"
                value={editingUrl}
                onChange={(event) => onUrlChange(event.target.value)}
                placeholder="https://company.example/careers"
                className="mt-1 h-10 w-full rounded-xl border border-slate-300 bg-white px-3 text-sm font-normal text-slate-900 outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-200"
              />
            </label>
          </div>
          <label className="block text-xs font-semibold text-slate-600">
            Notes
            <textarea
              value={editingNotes}
              onChange={(event) => onNotesChange(event.target.value)}
              rows={3}
              placeholder="Recruiter name, next steps, interview prep…"
              className="mt-1 w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-sm font-normal text-slate-900 outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-200"
            />
          </label>
          {updateError ? <p className="text-xs text-red-600">{updateError}</p> : null}
          <div className="flex flex-wrap gap-2">
            <Button type="submit" variant="brand" size="sm" disabled={saving}>
              <Check aria-hidden="true" className="h-4 w-4" />
              {saving ? "Saving…" : "Save changes"}
            </Button>
            <Button type="button" variant="ghost" size="sm" onClick={onCancel}>
              <X aria-hidden="true" className="h-4 w-4" />
              Cancel
            </Button>
          </div>
        </form>
      ) : (
        <div className="mt-3 flex flex-wrap items-center gap-2">
          <Link
            to={`/jobs/${application.job.id}`}
            className={buttonVariants({ variant: "ghost", size: "sm" })}
          >
            View job
          </Link>
          <Button variant="outline" size="sm" onClick={onEdit}>
            <Pencil aria-hidden="true" className="h-4 w-4" />
            Edit
          </Button>
          <Button variant="ghost" size="sm" onClick={() => setShowHistory((open) => !open)}>
            <CalendarDays aria-hidden="true" className="h-4 w-4" />
            History
          </Button>
          {application.externalApplicationUrl ? (
            <a
              href={application.externalApplicationUrl}
              target="_blank"
              rel="noreferrer noopener"
              className={buttonVariants({ variant: "ghost", size: "sm" })}
            >
              Apply link
              <ExternalLink aria-hidden="true" className="h-4 w-4" />
            </a>
          ) : null}
          <Button
            variant="ghost"
            size="sm"
            className="text-red-600 hover:bg-red-50 hover:text-red-700"
            onClick={onDelete}
            disabled={deleting}
          >
            <Trash2 aria-hidden="true" className="h-4 w-4" />
            {deleting ? "Deleting…" : "Delete"}
          </Button>
        </div>
      )}

      {showHistory ? (
        <div className="mt-3 rounded-2xl border border-slate-200 bg-white p-4">
          <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Status history</p>
          {historyQuery.isPending ? (
            <p className="mt-2 text-xs text-slate-500">Loading history…</p>
          ) : null}
          {historyQuery.isError ? (
            <p className="mt-2 text-xs text-red-600">Couldn't load this application's history.</p>
          ) : null}
          {historyQuery.data ? (
            <ol className="mt-3 space-y-2">
              {historyQuery.data.history.map((entry) => (
                <li key={entry.id} className="text-xs text-slate-600">
                  <span className="font-semibold text-slate-900">
                    {entry.oldStatus ? formatEnum(entry.oldStatus) : "Created"} →{" "}
                    {formatEnum(entry.newStatus)}
                  </span>
                  <span className="ml-2 text-slate-500">{formatDate(entry.changedAt)}</span>
                  {entry.note ? <p className="mt-0.5 text-slate-500">{entry.note}</p> : null}
                </li>
              ))}
            </ol>
          ) : null}
        </div>
      ) : null}
    </li>
  );
}

export function DashboardPage() {
  const { candidate } = useAuth();
  const queryClient = useQueryClient();
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editingStatus, setEditingStatus] = useState<ApplicationStatus>("APPLIED");
  const [editingAppliedAt, setEditingAppliedAt] = useState("");
  const [editingNotes, setEditingNotes] = useState("");
  const [editingUrl, setEditingUrl] = useState("");

  const dashboardQuery = useQuery({
    queryKey: ["dashboard"],
    queryFn: ({ signal }) => getDashboard(signal),
  });
  const matchesQuery = useQuery({
    queryKey: ["job-matches"],
    queryFn: ({ signal }) => getJobMatches(0, 6, signal),
  });
  const savedQuery = useQuery({
    queryKey: ["saved-jobs"],
    queryFn: ({ signal }) => getSavedJobs(0, 50, signal),
  });
  const followedQuery = useQuery({
    queryKey: ["followed-companies"],
    queryFn: ({ signal }) => getFollowedCompanies(0, 50, signal),
  });
  const applicationsQuery = useQuery({
    queryKey: ["applications"],
    queryFn: ({ signal }) => getApplications(undefined, 0, 50, signal),
  });

  function refreshEngagement() {
    void queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    void queryClient.invalidateQueries({ queryKey: ["saved-jobs"] });
    void queryClient.invalidateQueries({ queryKey: ["saved-job"] });
    void queryClient.invalidateQueries({ queryKey: ["followed-companies"] });
    void queryClient.invalidateQueries({ queryKey: ["followed-company"] });
    void queryClient.invalidateQueries({ queryKey: ["applications"] });
    void queryClient.invalidateQueries({ queryKey: ["application"] });
    void queryClient.invalidateQueries({ queryKey: ["job-matches"] });
  }

  const unsave = useMutation({ mutationFn: unsaveJob, onSuccess: refreshEngagement });
  const unfollow = useMutation({ mutationFn: unfollowCompany, onSuccess: refreshEngagement });
  const update = useMutation({
    mutationFn: ({
      applicationId,
      status,
      appliedAt,
      notes,
      externalApplicationUrl,
    }: {
      applicationId: string;
      status: ApplicationStatus;
      appliedAt: string;
      notes: string;
      externalApplicationUrl: string;
    }) =>
      updateApplication(applicationId, {
        status,
        appliedAt: appliedAt ? new Date(`${appliedAt}T00:00:00`).toISOString() : undefined,
        clearAppliedAt: !appliedAt,
        notes,
        externalApplicationUrl,
        statusNote: "Updated from dashboard",
      }),
    onSuccess: () => {
      setEditingId(null);
      refreshEngagement();
    },
  });
  const remove = useMutation({ mutationFn: deleteApplication, onSuccess: refreshEngagement });

  if (dashboardQuery.isPending) {
    return (
      <div className="container-page py-12">
        <Skeleton className="h-10 w-64" />
        <div className="mt-8 grid gap-4 sm:grid-cols-3">
          <Skeleton className="h-36 rounded-3xl" />
          <Skeleton className="h-36 rounded-3xl" />
          <Skeleton className="h-36 rounded-3xl" />
        </div>
        <Skeleton className="mt-8 h-64 rounded-3xl" />
      </div>
    );
  }

  if (dashboardQuery.isError) {
    return (
      <ErrorState
        className="container-page my-16"
        title="Couldn't load your dashboard"
        description="Your session is active, but the dashboard API didn't respond. Try again in a moment."
        onRetry={() => void dashboardQuery.refetch()}
      />
    );
  }

  const dashboard = dashboardQuery.data;
  const matches = matchesQuery.data?.content ?? [];
  const savedJobs = savedQuery.data?.content ?? [];
  const followedCompanies = followedQuery.data?.content ?? [];
  const applications = applicationsQuery.data?.content ?? [];
  const actionError = unsave.error ?? unfollow.error ?? remove.error ?? update.error;
  const actionErrorText =
    actionError instanceof ApiError ? actionError.message : actionError ? "That action didn't work." : null;
  const firstName = candidate?.fullName?.split(" ")[0] ?? "there";

  function startEditing(application: ApplicationSummary) {
    setEditingId(application.id);
    setEditingStatus(application.status);
    setEditingAppliedAt(application.appliedAt ? application.appliedAt.slice(0, 10) : "");
    setEditingNotes(application.notes ?? "");
    setEditingUrl(application.externalApplicationUrl ?? "");
  }

  function handleDelete(application: ApplicationSummary) {
    if (window.confirm(`Delete the tracked application for "${application.job.title}"?`)) {
      remove.mutate(application.id);
    }
  }

  return (
    <section className="container-page py-10 sm:py-14">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.18em] text-brand-700">
            Candidate dashboard
          </p>
          <h1 className="mt-2 text-3xl font-black tracking-tight text-slate-900 sm:text-4xl">
            Hi, {firstName}
          </h1>
          <p className="mt-2 text-sm text-slate-600">
            Keep saved roles, startup follows, and application notes together.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Link to="/jobs" className={buttonVariants({ variant: "brand", size: "sm" })}>
            <Briefcase aria-hidden="true" className="h-4 w-4" />
            Find roles
          </Link>
          <Link to="/startups" className={buttonVariants({ variant: "outline", size: "sm" })}>
            <Building2 aria-hidden="true" className="h-4 w-4" />
            Browse startups
          </Link>
        </div>
      </div>

      {actionErrorText ? (
        <p role="alert" className="mt-6 rounded-2xl border border-red-200 bg-red-50 p-3 text-sm text-red-700">
          {actionErrorText}
        </p>
      ) : null}

      <div className="mt-8 grid gap-4 sm:grid-cols-3">
        <StatCard
          label="Saved roles"
          value={dashboard.savedJobCount}
          href="#saved-jobs"
          icon={<Bookmark aria-hidden="true" className="h-5 w-5" />}
        />
        <StatCard
          label="Followed startups"
          value={dashboard.followedCompanyCount}
          href="#followed-companies"
          icon={<Building2 aria-hidden="true" className="h-5 w-5" />}
        />
        <StatCard
          label="Tracked applications"
          value={dashboard.applicationCount}
          href="#applications"
          icon={<ClipboardList aria-hidden="true" className="h-5 w-5" />}
        />
      </div>

      <div className="mt-6 grid gap-6 lg:grid-cols-[minmax(0,1.4fr)_minmax(0,0.6fr)] lg:items-start">
        <section id="recommended" className="rounded-3xl border border-slate-200 bg-white p-6">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <h2 className="flex items-center gap-2 text-lg font-semibold text-slate-900">
                <Sparkles aria-hidden="true" className="h-5 w-5 text-brand-700" />
                Recommended roles
              </h2>
              <p className="mt-1 text-sm text-slate-500">
                Ranked by your skills. Each match shows what lines up and what is missing.
              </p>
            </div>
            <Link to="/jobs" className="text-xs font-semibold text-brand-700 hover:text-brand-800">
              Browse all roles
            </Link>
          </div>

          {matchesQuery.isPending ? (
            <LoadingState className="mt-4" label="Finding your matches...?" />
          ) : null}
          {matchesQuery.isError ? (
            <ErrorState
              className="mt-4"
              title="Couldn't load recommendations"
              onRetry={() => void matchesQuery.refetch()}
            />
          ) : null}
          {matchesQuery.data && matches.length === 0 ? (
            <EmptyState
              className="mt-4"
              icon={<Sparkles aria-hidden="true" className="h-8 w-8" />}
              title={candidate?.skills.length ? "No skill matches yet" : "Add skills to get matches"}
              description={
                candidate?.skills.length
                  ? "No active roles match your current skills. Add a few more skills or browse all roles."
                  : "Add a few skills on the right and we will rank active roles for you."
              }
            />
          ) : null}

          {matches.length > 0 ? (
            <ul className="mt-4 space-y-3">
              {matches.map((match) => {
                const company = match.job.company;
                return (
                  <li key={match.job.id} className="rounded-2xl border border-slate-200 bg-slate-50/60 p-4">
                    <div className="flex flex-wrap items-start justify-between gap-3">
                      <div className="min-w-0">
                        <Link
                          to={`/jobs/${match.job.id}`}
                          className="text-sm font-semibold text-slate-900 transition hover:text-brand-700"
                        >
                          {match.job.title}
                        </Link>
                        <p className="mt-1 text-xs text-slate-500">{company?.name ?? "Company"}</p>
                      </div>
                      <span className="inline-flex items-center rounded-full bg-brand-700 px-3 py-1 text-xs font-bold text-white">
                        {match.matchScore}% match
                      </span>
                    </div>

                    {match.matchedSkills.length > 0 ? (
                      <div className="mt-3 flex flex-wrap gap-1.5">
                        {match.matchedSkills.map((skill) => (
                          <span
                            key={skill.skillId}
                            className="rounded-full bg-emerald-50 px-2.5 py-1 text-[11px] font-medium text-emerald-700"
                          >
                            {skill.skillName}
                          </span>
                        ))}
                        {match.missingSkills.map((skill) => (
                          <span
                            key={skill.skillId}
                            className="rounded-full border border-amber-200 bg-amber-50 px-2.5 py-1 text-[11px] font-medium text-amber-700"
                          >
                            Missing {skill.skillName}
                          </span>
                        ))}
                      </div>
                    ) : null}

                    <ul className="mt-3 space-y-1 text-xs text-slate-600">
                      {match.signals.map((signal) => (
                        <li key={signal.code}>{signal.message}</li>
                      ))}
                    </ul>

                    <div className="mt-3 flex flex-wrap gap-2">
                      <Link
                        to={`/jobs/${match.job.id}`}
                        className={buttonVariants({ variant: "ghost", size: "sm" })}
                      >
                        View role
                      </Link>
                      <TrackApplicationButton jobId={match.job.id} />
                    </div>
                  </li>
                );
              })}
            </ul>
          ) : null}
        </section>

        <SkillsEditor />
      </div>

      <ResumeManager />

      <div className="mt-10 grid gap-6 lg:grid-cols-[minmax(0,1.15fr)_minmax(0,0.85fr)] lg:items-start">
        <section id="saved-jobs" className="rounded-3xl border border-slate-200 bg-white p-6">
          <div className="flex items-center justify-between gap-3">
            <h2 className="text-lg font-semibold text-slate-900">Saved roles</h2>
            <Link to="/jobs" className="text-xs font-semibold text-brand-700 hover:text-brand-800">
              Find more
            </Link>
          </div>

          {savedQuery.isPending ? <LoadingState className="mt-4" label="Loading saved roles…" /> : null}
          {savedQuery.isError ? (
            <ErrorState
              className="mt-4"
              title="Couldn't load saved roles"
              onRetry={() => void savedQuery.refetch()}
            />
          ) : null}
          {savedQuery.data && savedJobs.length === 0 ? (
            <EmptyState
              className="mt-4"
              icon={<Bookmark aria-hidden="true" className="h-8 w-8" />}
              title="No saved roles yet"
              description="Save a role from any job page and it will appear here."
              action={
                <Link to="/jobs" className={buttonVariants({ variant: "brand", size: "sm" })}>
                  Browse jobs
                </Link>
              }
            />
          ) : null}

          {savedJobs.length > 0 ? (
            <ul className="mt-4 space-y-3">
              {savedJobs.map((saved) => {
                const savedDate = formatRelativeDate(saved.savedAt);
                const company = saved.job.company;

                return (
                  <li
                    key={saved.jobId}
                    className="rounded-2xl border border-slate-200 bg-slate-50/60 p-4"
                  >
                    <div className="flex flex-wrap items-start justify-between gap-3">
                      <div className="min-w-0">
                        <Link
                          to={`/jobs/${saved.jobId}`}
                          className="text-sm font-semibold text-slate-900 transition hover:text-brand-700"
                        >
                          {saved.job.title}
                        </Link>
                        <p className="mt-1 text-xs text-slate-500">
                          {company?.name ?? "Company"}
                          {company?.industryName ? ` · ${company.industryName}` : ""}
                        </p>
                        <p className="mt-1 text-xs text-slate-500">
                          Saved {savedDate ?? "recently"}
                          {saved.available ? "" : " · no longer active"}
                        </p>
                      </div>
                      <div className="flex flex-wrap gap-2">
                        <TrackApplicationButton jobId={saved.jobId} />
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => unsave.mutate(saved.jobId)}
                          disabled={unsave.isPending}
                        >
                          <Trash2 aria-hidden="true" className="h-4 w-4" />
                          Remove
                        </Button>
                      </div>
                    </div>
                  </li>
                );
              })}
            </ul>
          ) : null}
        </section>

        <section
          id="followed-companies"
          className="rounded-3xl border border-slate-200 bg-white p-6"
        >
          <div className="flex items-center justify-between gap-3">
            <h2 className="text-lg font-semibold text-slate-900">Followed startups</h2>
            <Link to="/startups" className="text-xs font-semibold text-brand-700 hover:text-brand-800">
              Discover
            </Link>
          </div>

          {followedQuery.isPending ? (
            <LoadingState className="mt-4" label="Loading followed startups…" />
          ) : null}
          {followedQuery.isError ? (
            <ErrorState
              className="mt-4"
              title="Couldn't load followed startups"
              onRetry={() => void followedQuery.refetch()}
            />
          ) : null}
          {followedQuery.data && followedCompanies.length === 0 ? (
            <EmptyState
              className="mt-4"
              icon={<Building2 aria-hidden="true" className="h-8 w-8" />}
              title="No followed startups yet"
              description="Follow a startup to keep an eye on its open roles."
              action={
                <Link to="/startups" className={buttonVariants({ variant: "brand", size: "sm" })}>
                  Browse startups
                </Link>
              }
            />
          ) : null}

          {followedCompanies.length > 0 ? (
            <ul className="mt-4 space-y-3">
              {followedCompanies.map((company) => (
                <li
                  key={company.companyId}
                  className="flex items-start gap-3 rounded-2xl border border-slate-200 bg-slate-50/60 p-4"
                >
                  {company.logoUrl ? (
                    <img
                      src={company.logoUrl}
                      alt=""
                      className="h-10 w-10 shrink-0 rounded-xl border border-slate-100 bg-white object-cover"
                    />
                  ) : (
                    <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-slate-900 text-[11px] font-bold text-brand-300">
                      {initials(company.name)}
                    </span>
                  )}
                  <div className="min-w-0 flex-1">
                    <Link
                      to={`/startups/${company.companyId}`}
                      className="text-sm font-semibold text-slate-900 transition hover:text-brand-700"
                    >
                      {company.name}
                    </Link>
                    <p className="mt-1 text-xs text-slate-500">
                      {company.industryName ?? "Pune startup"} · {company.activeJobCount} active{" "}
                      {company.activeJobCount === 1 ? "role" : "roles"}
                    </p>
                    <div className="mt-2 flex flex-wrap gap-2">
                      <Link
                        to={`/startups/${company.companyId}`}
                        className={buttonVariants({ variant: "ghost", size: "sm" })}
                      >
                        View
                        <ArrowUpRight aria-hidden="true" className="h-3.5 w-3.5" />
                      </Link>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => unfollow.mutate(company.companyId)}
                        disabled={unfollow.isPending}
                      >
                        Unfollow
                      </Button>
                    </div>
                  </div>
                </li>
              ))}
            </ul>
          ) : null}
        </section>
      </div>

      <section id="applications" className="mt-6 rounded-3xl border border-slate-200 bg-white p-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="text-lg font-semibold text-slate-900">Applications</h2>
            <p className="mt-1 text-sm text-slate-500">
              A private record of what you applied to. The employer still receives your actual
              application on their own site.
            </p>
          </div>
          <span className="inline-flex items-center gap-1.5 rounded-full bg-slate-100 px-3 py-1.5 text-xs font-semibold text-slate-600">
            <CalendarDays aria-hidden="true" className="h-3.5 w-3.5" />
            {dashboard.applicationCount} tracked
          </span>
        </div>

        {applicationsQuery.isPending ? (
          <LoadingState className="mt-4" label="Loading applications…" />
        ) : null}
        {applicationsQuery.isError ? (
          <ErrorState
            className="mt-4"
            title="Couldn't load applications"
            onRetry={() => void applicationsQuery.refetch()}
          />
        ) : null}
        {applicationsQuery.data && applications.length === 0 ? (
          <EmptyState
            className="mt-4"
            icon={<ClipboardList aria-hidden="true" className="h-8 w-8" />}
            title="No applications tracked"
            description="Open a job and choose “Track application” to add your first record."
            action={
              <Link to="/jobs" className={buttonVariants({ variant: "brand", size: "sm" })}>
                Browse jobs
              </Link>
            }
          />
        ) : null}

        {applications.length > 0 ? (
          <ul className="mt-4 space-y-3">
            {applications.map((application) => (
              <ApplicationRow
                key={application.id}
                application={application}
                editing={editingId === application.id}
                editingStatus={editingStatus}
                editingAppliedAt={editingAppliedAt}
                editingNotes={editingNotes}
                editingUrl={editingUrl}
                onEdit={() => startEditing(application)}
                onCancel={() => setEditingId(null)}
                onStatusChange={setEditingStatus}
                onAppliedAtChange={setEditingAppliedAt}
                onNotesChange={setEditingNotes}
                onUrlChange={setEditingUrl}
                onSave={(event) => {
                  event.preventDefault();
                  update.mutate({
                    applicationId: application.id,
                    status: editingStatus,
                    appliedAt: editingAppliedAt,
                    notes: editingNotes,
                    externalApplicationUrl: editingUrl,
                  });
                }}
                onDelete={() => handleDelete(application)}
                saving={update.isPending && editingId === application.id}
                deleting={remove.isPending}
                updateError={
                  editingId === application.id && update.isError
                    ? update.error instanceof ApiError
                      ? update.error.message
                      : "Couldn't save changes."
                    : null
                }
              />
            ))}
          </ul>
        ) : null}
      </section>
    </section>
  );
}
