import { Download, FileText, RefreshCw, ShieldCheck, Trash2, Upload } from "lucide-react";
import { useEffect, useRef, useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "@/api/client";
import { Button } from "@/components/ui/button";
import { EmptyState, ErrorState, LoadingState } from "@/components/ui/states";
import { deleteResume, getResumes, uploadResume } from "@/features/candidate/api";
import type { CandidateResume, ResumeProcessingStatus } from "@/features/candidate/types";
import { useAuth } from "@/features/auth/auth-context";
import { formatDate, formatRelativeDate } from "@/lib/format";
import { cn } from "@/lib/utils";

const MAX_FILE_BYTES = 5 * 1024 * 1024;

function statusLabel(status: ResumeProcessingStatus | null): string {
  switch (status) {
    case "COMPLETED":
      return "Skills extracted";
    case "FAILED":
      return "Could not parse";
    case "PROCESSING":
      return "Extracting skills";
    case "UPLOADED":
      return "Queued for parsing";
    default:
      return "Pending";
  }
}

function statusClasses(status: ResumeProcessingStatus | null): string {
  switch (status) {
    case "COMPLETED":
      return "border-emerald-200 bg-emerald-50 text-emerald-700";
    case "FAILED":
      return "border-red-200 bg-red-50 text-red-700";
    case "PROCESSING":
    case "UPLOADED":
      return "border-amber-200 bg-amber-50 text-amber-700";
    default:
      return "border-slate-200 bg-slate-100 text-slate-600";
  }
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) {
    return `${bytes} B`;
  }
  if (bytes < 1024 * 1024) {
    return `${Math.round(bytes / 1024)} KB`;
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function isProcessing(resume: CandidateResume): boolean {
  return resume.processingStatus === "UPLOADED" || resume.processingStatus === "PROCESSING";
}

export function ResumeManager() {
  const { refreshProfile } = useAuth();
  const queryClient = useQueryClient();
  const fileInput = useRef<HTMLInputElement>(null);
  const notified = useRef(new Set<string>());
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [consent, setConsent] = useState(false);
  const [primary, setPrimary] = useState(true);
  const [fileError, setFileError] = useState<string | null>(null);

  const resumesQuery = useQuery({
    queryKey: ["resumes"],
    queryFn: ({ signal }) => getResumes(signal),
    refetchInterval: (query) => {
      const data = query.state.data;
      return data?.some(isProcessing) ? 2000 : false;
    },
  });

  const resumes = resumesQuery.data ?? [];
  const upload = useMutation({
    mutationFn: (file: File) => uploadResume({ file, primary }),
    onSuccess: () => {
      setSelectedFile(null);
      setConsent(false);
      setPrimary(true);
      setFileError(null);
      if (fileInput.current) {
        fileInput.current.value = "";
      }
      void queryClient.invalidateQueries({ queryKey: ["resumes"] });
    },
  });

  const remove = useMutation({
    mutationFn: deleteResume,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["resumes"] });
    },
  });

  useEffect(() => {
    const newlyCompleted = resumes.filter(
      (resume) => resume.processingStatus === "COMPLETED" && !notified.current.has(resume.id),
    );
    if (newlyCompleted.length === 0) {
      return;
    }
    newlyCompleted.forEach((resume) => notified.current.add(resume.id));
    void refreshProfile().catch(() => undefined);
    void queryClient.invalidateQueries({ queryKey: ["job-matches"] });
    void queryClient.invalidateQueries({ queryKey: ["dashboard"] });
  }, [resumes, queryClient, refreshProfile]);

  const actionError = upload.error ?? remove.error;
  const actionErrorText =
    actionError instanceof ApiError ? actionError.message : actionError ? "That action didn't work." : null;

  function handleFileChange(file: File | null) {
    setFileError(null);
    upload.reset();
    if (!file) {
      setSelectedFile(null);
      return;
    }
    if (file.size > MAX_FILE_BYTES) {
      setSelectedFile(null);
      setFileError("Choose a file of 5 MB or less.");
      if (fileInput.current) {
        fileInput.current.value = "";
      }
      return;
    }
    setSelectedFile(file);
  }

  function handleUpload(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedFile) {
      setFileError("Choose a PDF, DOCX, or TXT resume first.");
      return;
    }
    if (!consent) {
      setFileError("Please confirm the storage and matching consent before uploading.");
      return;
    }
    setFileError(null);
    upload.mutate(selectedFile);
  }

  return (
    <section id="resume" className="mt-6 rounded-3xl border border-slate-200 bg-white p-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="flex items-center gap-2 text-lg font-semibold text-slate-900">
            <FileText aria-hidden="true" className="h-5 w-5 text-brand-700" />
            Resumes and matching
          </h2>
          <p className="mt-1 max-w-2xl text-sm text-slate-500">
            Upload a resume to extract skills for recommendations. Files stay private and are deleted
            automatically when their retention date passes.
          </p>
        </div>
        <span className="inline-flex items-center gap-1.5 rounded-full bg-slate-100 px-3 py-1.5 text-xs font-semibold text-slate-600">
          <ShieldCheck aria-hidden="true" className="h-3.5 w-3.5" />
          Private storage
        </span>
      </div>

      <form className="mt-5 rounded-2xl border border-slate-200 bg-slate-50/60 p-4" onSubmit={handleUpload}>
        <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_auto] lg:items-end">
          <label className="block text-xs font-semibold text-slate-600">
            {resumes.length > 0 ? "Upload a replacement or another version" : "Choose your resume"}
            <input
              ref={fileInput}
              type="file"
              accept=".pdf,.docx,.txt,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document,text/plain"
              onChange={(event) => handleFileChange(event.target.files?.[0] ?? null)}
              className="mt-1 block w-full cursor-pointer rounded-xl border border-slate-300 bg-white px-3 py-2 text-sm font-normal text-slate-900 file:mr-3 file:rounded-lg file:border-0 file:bg-slate-900 file:px-3 file:py-2 file:text-xs file:font-semibold file:text-white hover:file:bg-slate-800"
            />
            <span className="mt-1 block font-normal text-slate-500">PDF, DOCX, or TXT up to 5 MB.</span>
          </label>
          <Button type="submit" variant="brand" disabled={upload.isPending || !selectedFile || !consent}>
            <Upload aria-hidden="true" className="h-4 w-4" />
            {upload.isPending ? "Uploading…" : resumes.length > 0 ? "Upload resume" : "Upload and parse"}
          </Button>
        </div>

        <div className="mt-4 space-y-2">
          <label className="flex items-start gap-2 text-xs leading-relaxed text-slate-600">
            <input
              type="checkbox"
              checked={consent}
              onChange={(event) => setConsent(event.target.checked)}
              className="mt-0.5 h-4 w-4 rounded border-slate-300 text-brand-700 focus:ring-brand-200"
            />
            <span>
              I agree that Pune Startup stores this file privately, uses its text to extract skills and
              match me to roles, and keeps it for up to 30 days unless I delete it earlier.
            </span>
          </label>
          <label className="flex items-center gap-2 text-xs text-slate-600">
            <input
              type="checkbox"
              checked={primary}
              onChange={(event) => setPrimary(event.target.checked)}
              className="h-4 w-4 rounded border-slate-300 text-brand-700 focus:ring-brand-200"
            />
            Use this as my primary resume
          </label>
        </div>

        {selectedFile ? (
          <p className="mt-3 text-xs text-slate-500">
            Selected: <span className="font-medium text-slate-700">{selectedFile.name}</span> (
            {formatFileSize(selectedFile.size)})
          </p>
        ) : null}
        {fileError ? <p className="mt-3 text-xs text-red-600">{fileError}</p> : null}
        {actionErrorText ? (
          <p role="alert" className="mt-3 text-xs text-red-600">
            {actionErrorText}
          </p>
        ) : null}
      </form>

      <div className="mt-5">
        {resumesQuery.isPending ? <LoadingState label="Loading your resumes…" /> : null}
        {resumesQuery.isError ? (
          <ErrorState
            title="Couldn't load your resumes"
            description="Try again in a moment. Your files are not affected."
            onRetry={() => void resumesQuery.refetch()}
          />
        ) : null}
        {resumesQuery.data && resumes.length === 0 ? (
          <EmptyState
            icon={<FileText aria-hidden="true" className="h-8 w-8" />}
            title="No resume uploaded"
            description="Upload a PDF, DOCX, or TXT file above. We will extract skills and use them to rank roles for you."
          />
        ) : null}

        {resumes.length > 0 ? (
          <ul className="mt-4 space-y-3">
            {resumes.map((resume) => {
              const uploaded = formatRelativeDate(resume.uploadedAt);
              const retention = formatDate(resume.retentionExpiresAt);
              return (
                <li key={resume.id} className="rounded-2xl border border-slate-200 bg-slate-50/60 p-4">
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div className="min-w-0">
                      <div className="flex flex-wrap items-center gap-2">
                        <p className="text-sm font-semibold text-slate-900">{resume.originalFilename}</p>
                        {resume.primary ? (
                          <span className="rounded-full bg-brand-700 px-2.5 py-0.5 text-[11px] font-bold text-white">
                            Primary
                          </span>
                        ) : null}
                        <span
                          className={cn(
                            "inline-flex items-center gap-1 rounded-full border px-2.5 py-0.5 text-[11px] font-semibold",
                            statusClasses(resume.processingStatus),
                          )}
                        >
                          {isProcessing(resume) ? (
                            <RefreshCw aria-hidden="true" className="h-3 w-3 animate-spin" />
                          ) : null}
                          {statusLabel(resume.processingStatus)}
                        </span>
                      </div>
                      <p className="mt-1 text-xs text-slate-500">
                        {formatFileSize(resume.fileSizeBytes)} · Uploaded {uploaded ?? "recently"}
                        {retention ? ` · Retained until ${retention}` : ""}
                      </p>
                      <p className="mt-1 text-xs text-slate-500">
                        {resume.processingStatus === "COMPLETED"
                          ? `${resume.parsedSkillCount} ${
                              resume.parsedSkillCount === 1 ? "skill" : "skills"
                            } added to your profile from this file.`
                          : resume.processingStatus === "FAILED"
                            ? "We couldn't extract skills from this file. Try a PDF, DOCX, or TXT version."
                            : "We are reading the file in the background. This usually takes a few seconds."}
                      </p>
                    </div>
                    <div className="flex flex-wrap gap-2">
                      {resume.downloadUrl ? (
                        <a
                          href={resume.downloadUrl}
                          target="_blank"
                          rel="noreferrer noopener"
                          className="inline-flex h-9 items-center gap-1.5 rounded-xl border border-slate-300 bg-white px-3 text-xs font-semibold text-slate-700 transition hover:border-slate-400 hover:text-slate-900"
                        >
                          <Download aria-hidden="true" className="h-3.5 w-3.5" />
                          Download
                        </a>
                      ) : null}
                      <Button
                        type="button"
                        variant="ghost"
                        size="sm"
                        className="text-red-600 hover:bg-red-50 hover:text-red-700"
                        onClick={() => {
                          if (window.confirm(`Delete "${resume.originalFilename}" permanently?`)) {
                            remove.mutate(resume.id);
                          }
                        }}
                        disabled={remove.isPending}
                      >
                        <Trash2 aria-hidden="true" className="h-4 w-4" />
                        {remove.isPending ? "Deleting…" : "Delete"}
                      </Button>
                    </div>
                  </div>
                </li>
              );
            })}
          </ul>
        ) : null}
      </div>
    </section>
  );
}
