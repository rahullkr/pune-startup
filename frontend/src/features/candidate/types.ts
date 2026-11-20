import type { JobSummary } from "@/features/jobs/types";

export type ApplicationStatus =
  | "SAVED"
  | "APPLIED"
  | "ASSESSMENT"
  | "INTERVIEW"
  | "OFFER"
  | "REJECTED"
  | "ACCEPTED"
  | "DECLINED"
  | "WITHDRAWN";

export interface SavedJob {
  jobId: string;
  savedAt: string;
  available: boolean;
  job: JobSummary;
}

export interface FollowedCompany {
  companyId: string;
  followedAt: string;
  available: boolean;
  name: string;
  slug: string;
  logoUrl: string | null;
  websiteUrl: string | null;
  industryName: string | null;
  verificationStatus: string | null;
  recordStatus: string | null;
  activeJobCount: number;
}

export interface ApplicationSummary {
  id: string;
  status: ApplicationStatus;
  appliedAt: string | null;
  externalApplicationUrl: string | null;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
  job: JobSummary;
}

export interface ApplicationHistory {
  id: string;
  oldStatus: ApplicationStatus | null;
  newStatus: ApplicationStatus;
  changedAt: string;
  note: string | null;
}

export interface ApplicationDetail extends ApplicationSummary {
  history: ApplicationHistory[];
}

export interface CandidateDashboard {
  savedJobCount: number;
  followedCompanyCount: number;
  applicationCount: number;
  recentSavedJobs: SavedJob[];
  recentFollowedCompanies: FollowedCompany[];
  recentApplications: ApplicationSummary[];
}

export interface MatchedJobSkill {
  skillId: string;
  skillName: string;
  requirementType: string | null;
  proficiency: string | null;
  yearsExperience: number | null;
}

export interface MissingJobSkill {
  skillId: string;
  skillName: string;
  requirementType: string | null;
}

export interface JobMatchSignal {
  code: string;
  message: string;
}

export interface JobMatch {
  matchScore: number;
  matchedSkillCount: number;
  requiredSkillCount: number;
  matchedRequiredSkillCount: number;
  preferredSkillCount: number;
  matchedPreferredSkillCount: number;
  job: JobSummary;
  matchedSkills: MatchedJobSkill[];
  missingSkills: MissingJobSkill[];
  signals: JobMatchSignal[];
}

export type ResumeProcessingStatus = "UPLOADED" | "PROCESSING" | "COMPLETED" | "FAILED";

export interface CandidateResume {
  id: string;
  originalFilename: string;
  contentType: string;
  fileSizeBytes: number;
  processingStatus: ResumeProcessingStatus | null;
  primary: boolean;
  uploadedAt: string;
  processedAt: string | null;
  consentGivenAt: string;
  consentVersion: string;
  retentionExpiresAt: string;
  parsedSkillCount: number;
  parseErrorCode: string | null;
  downloadUrl: string | null;
}

export interface UploadResumeInput {
  file: File;
  primary?: boolean;
  consentVersion?: string;
}

export interface CreateApplicationInput {
  jobId: string;
  status?: ApplicationStatus;
  appliedAt?: string;
  externalApplicationUrl?: string;
  notes?: string;
  statusNote?: string;
}

export interface UpdateApplicationInput {
  status?: ApplicationStatus;
  appliedAt?: string;
  clearAppliedAt?: boolean;
  externalApplicationUrl?: string;
  notes?: string;
  statusNote?: string;
}
