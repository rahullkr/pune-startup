import { apiGet } from "@/api/client";
import type { PageResponse } from "@/api/types";
import type { CompanyDetail, CompanySummary, Industry } from "@/features/companies/types";

export interface CompanySearchParams {
  keyword?: string;
  city?: string;
  area?: string;
  industry?: string;
  roleType?: "ALL" | "FRESHER";
  page?: number;
  size?: number;
  sort?: "name" | "newest" | "updated";
}

export function getCompanies(
  params: CompanySearchParams = {},
  signal?: AbortSignal,
): Promise<PageResponse<CompanySummary>> {
  const search = new URLSearchParams();

  if (params.keyword) {
    search.set("keyword", params.keyword);
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
  if (params.roleType) {
    search.set("roleType", params.roleType);
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

  return apiGet<PageResponse<CompanySummary>>(`/companies${query ? `?${query}` : ""}`, signal);
}

export function getCompany(companyId: string, signal?: AbortSignal): Promise<CompanyDetail> {
  return apiGet<CompanyDetail>(`/companies/${companyId}`, signal);
}

export function getIndustries(signal?: AbortSignal): Promise<Industry[]> {
  return apiGet<Industry[]>("/industries", signal);
}

export function getCompanyAreas(signal?: AbortSignal): Promise<string[]> {
  return apiGet<string[]>("/companies/areas", signal);
}
