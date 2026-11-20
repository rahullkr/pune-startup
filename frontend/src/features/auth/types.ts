export interface CandidateSkill {
  skillId: string;
  skillName: string;
  category: string | null;
  proficiency: string | null;
  yearsExperience: number | null;
  source: string;
}

export interface CandidateProfile {
  id: string;
  userId: string;
  email: string;
  fullName: string;
  headline: string | null;
  summary: string | null;
  totalExperienceMonths: number | null;
  preferredCity: string | null;
  preferredWorkMode: string | null;
  skills: CandidateSkill[];
  createdAt: string;
  updatedAt: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  expiresAt: string;
  candidate: CandidateProfile;
}

export interface LoginInput {
  email: string;
  password: string;
}

export interface RegisterInput extends LoginInput {
  fullName: string;
}

export interface CandidateSkillInput {
  name: string;
  proficiency?: string;
  yearsExperience?: number;
}

export interface UpdateProfileInput {
  fullName?: string;
  headline?: string;
  summary?: string;
  totalExperienceMonths?: number;
  preferredCity?: string;
  preferredWorkMode?: string;
  clearExperience?: boolean;
  skills?: CandidateSkillInput[];
}
