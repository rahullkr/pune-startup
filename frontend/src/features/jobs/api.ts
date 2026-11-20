import { apiGet } from "@/api/client";
import type { PageResponse } from "@/api/types";
import type { JobDetail, JobSummary } from "@/features/jobs/types";

export interface JobSearchParams {
  keyword?: string;
  companyId?: string;
  city?: string;
  area?: string;
  industry?: string;
  employmentType?: string;
  workMode?: string;
  fresher?: boolean;
  experienceYears?: number;
  skills?: string[];
  page?: number;
  size?: number;
  sort?: "newest" | "oldest" | "salary";
}

export interface CompanyJobsParams {
  page?: number;
  size?: number;
}

export function getJobs(
  params: JobSearchParams = {},
  signal?: AbortSignal,
): Promise<PageResponse<JobSummary>> {
  const search = new URLSearchParams();

  if (params.keyword) {
    search.set("keyword", params.keyword);
  }
  if (params.companyId) {
    search.set("companyId", params.companyId);
  }
  if (params.city) {
    search.set("city", params.city);
  }
  if (params.area) {
    search.set("area", params.area);
  }
  if (params.industry) {
    search.set("industry", params.industry);
  }
  if (params.employmentType) {
    search.set("employmentType", params.employmentType);
  }
  if (params.workMode) {
    search.set("workMode", params.workMode);
  }
  if (params.fresher !== undefined) {
    search.set("fresher", String(params.fresher));
  }
  if (params.experienceYears !== undefined) {
    search.set("experienceYears", String(params.experienceYears));
  }
  for (const skill of params.skills ?? []) {
    if (skill) {
      search.append("skill", skill);
    }
  }
  if (params.page !== undefined) {
    search.set("page", String(params.page));
  }
  if (params.size !== undefined) {
    search.set("size", String(params.size));
  }
  if (params.sort) {
    search.set("sort", params.sort);
  }

  const query = search.toString();

  return apiGet<PageResponse<JobSummary>>(`/jobs${query ? `?${query}` : ""}`, signal);
}

export function getJob(jobId: string, signal?: AbortSignal): Promise<JobDetail> {
  return apiGet<JobDetail>(`/jobs/${jobId}`, signal);
}

export function getCompanyJobs(
  companyId: string,
  params: CompanyJobsParams = {},
  signal?: AbortSignal,
): Promise<PageResponse<JobSummary>> {
  const search = new URLSearchParams();

  if (params.page !== undefined) {
    search.set("page", String(params.page));
  }
  if (params.size !== undefined) {
    search.set("size", String(params.size));
  }

  const query = search.toString();

  return apiGet<PageResponse<JobSummary>>(
    `/companies/${companyId}/jobs${query ? `?${query}` : ""}`,
    signal,
  );
}
