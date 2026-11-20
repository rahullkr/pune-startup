import { apiDelete, apiGet, apiPatch, apiPost, apiPut, apiUpload } from "@/api/client";
import type { PageResponse } from "@/api/types";
import type {
  ApplicationDetail,
  ApplicationStatus,
  ApplicationSummary,
  CandidateDashboard,
  CandidateResume,
  CreateApplicationInput,
  FollowedCompany,
  JobMatch,
  SavedJob,
  UpdateApplicationInput,
  UploadResumeInput,
} from "@/features/candidate/types";

export function getDashboard(signal?: AbortSignal): Promise<CandidateDashboard> {
  return apiGet<CandidateDashboard>("/candidates/me/dashboard", signal);
}

export function getJobMatches(
  page = 0,
  size = 20,
  signal?: AbortSignal,
): Promise<PageResponse<JobMatch>> {
  return apiGet<PageResponse<JobMatch>>(
    `/candidates/me/job-matches?page=${page}&size=${size}`,
    signal,
  );
}

export function getSavedJobs(page = 0, size = 50, signal?: AbortSignal): Promise<PageResponse<SavedJob>> {
  return apiGet<PageResponse<SavedJob>>(
    `/candidates/me/saved-jobs?page=${page}&size=${size}`,
    signal,
  );
}

export function getSavedJob(jobId: string, signal?: AbortSignal): Promise<SavedJob> {
  return apiGet<SavedJob>(`/candidates/me/saved-jobs/${jobId}`, signal);
}

export function saveJob(jobId: string): Promise<SavedJob> {
  return apiPut<SavedJob>(`/candidates/me/saved-jobs/${jobId}`);
}

export function unsaveJob(jobId: string): Promise<void> {
  return apiDelete<void>(`/candidates/me/saved-jobs/${jobId}`);
}

export function getFollowedCompanies(
  page = 0,
  size = 50,
  signal?: AbortSignal,
): Promise<PageResponse<FollowedCompany>> {
  return apiGet<PageResponse<FollowedCompany>>(
    `/candidates/me/followed-companies?page=${page}&size=${size}`,
    signal,
  );
}

export function getFollowedCompany(companyId: string, signal?: AbortSignal): Promise<FollowedCompany> {
  return apiGet<FollowedCompany>(`/candidates/me/followed-companies/${companyId}`, signal);
}

export function followCompany(companyId: string): Promise<FollowedCompany> {
  return apiPut<FollowedCompany>(`/candidates/me/followed-companies/${companyId}`);
}

export function unfollowCompany(companyId: string): Promise<void> {
  return apiDelete<void>(`/candidates/me/followed-companies/${companyId}`);
}

export function getApplications(
  status?: ApplicationStatus,
  page = 0,
  size = 50,
  signal?: AbortSignal,
): Promise<PageResponse<ApplicationSummary>> {
  const search = new URLSearchParams({ page: String(page), size: String(size) });

  if (status) {
    search.set("status", status);
  }

  return apiGet<PageResponse<ApplicationSummary>>(
    `/candidates/me/applications?${search.toString()}`,
    signal,
  );
}

export function getApplication(applicationId: string, signal?: AbortSignal): Promise<ApplicationDetail> {
  return apiGet<ApplicationDetail>(`/candidates/me/applications/${applicationId}`, signal);
}

export function createApplication(input: CreateApplicationInput): Promise<ApplicationDetail> {
  return apiPost<ApplicationDetail>("/candidates/me/applications", input);
}

export function updateApplication(
  applicationId: string,
  input: UpdateApplicationInput,
): Promise<ApplicationDetail> {
  return apiPatch<ApplicationDetail>(`/candidates/me/applications/${applicationId}`, input);
}

export function deleteApplication(applicationId: string): Promise<void> {
  return apiDelete<void>(`/candidates/me/applications/${applicationId}`);
}

export function getResumes(signal?: AbortSignal): Promise<CandidateResume[]> {
  return apiGet<CandidateResume[]>("/candidates/me/resumes", signal);
}

export function uploadResume(input: UploadResumeInput): Promise<CandidateResume> {
  const body = new FormData();
  body.set("file", input.file);
  body.set("consent", "true");
  body.set("consentVersion", input.consentVersion ?? "v1");
  body.set("primary", String(input.primary ?? true));
  return apiUpload<CandidateResume>("/candidates/me/resumes", body);
}

export function deleteResume(resumeId: string): Promise<void> {
  return apiDelete<void>(`/candidates/me/resumes/${resumeId}`);
}
