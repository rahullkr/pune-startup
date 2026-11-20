import { apiGet, apiPatch, apiPost } from "@/api/client";
import type {
  AuthResponse,
  CandidateProfile,
  LoginInput,
  RegisterInput,
  UpdateProfileInput,
} from "@/features/auth/types";

export function login(input: LoginInput): Promise<AuthResponse> {
  return apiPost<AuthResponse>("/auth/login", input);
}

export function register(input: RegisterInput): Promise<AuthResponse> {
  return apiPost<AuthResponse>("/auth/register", input);
}

export function logout(): Promise<void> {
  return apiPost<void>("/auth/logout");
}

export function getProfile(signal?: AbortSignal): Promise<CandidateProfile> {
  return apiGet<CandidateProfile>("/candidates/me", signal);
}

export function updateProfile(input: UpdateProfileInput): Promise<CandidateProfile> {
  return apiPatch<CandidateProfile>("/candidates/me", input);
}
