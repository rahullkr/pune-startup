import type { CompanyLocation } from "@/features/companies/types";

export interface JobCompany {
  id: string;
  name: string;
  slug: string;
  logoUrl: string | null;
  websiteUrl: string | null;
  industryName: string | null;
  verificationStatus: string | null;
}

export interface JobSummary {
  id: string;
  title: string;
  employmentType: string | null;
  workMode: string | null;
  experienceMinYears: number | null;
  experienceMaxYears: number | null;
  salaryMin: number | null;
  salaryMax: number | null;
  salaryCurrency: string | null;
  salaryPeriod: string | null;
  status: string | null;
  postedAt: string | null;
  fresherEligible: boolean;
  company: JobCompany | null;
  location: CompanyLocation | null;
}

export interface JobSkill {
  skillId: string;
  skillName: string;
  category: string | null;
  requirementType: string | null;
}

export interface JobSource {
  sourceName: string;
  sourceUrl: string;
  applicationUrl: string | null;
  sourceStatus: string | null;
  firstSeenAt: string | null;
  lastSeenAt: string | null;
  lastCheckedAt: string | null;
}

export interface JobDetail extends JobSummary {
  description: string | null;
  firstPublishedAt: string | null;
  lastVerifiedAt: string | null;
  expiresAt: string | null;
  skills: JobSkill[];
  sources: JobSource[];
  createdAt: string;
  updatedAt: string;
}
