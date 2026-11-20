import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import { ApiError, clearAuthToken, getAuthToken, setAuthToken } from "@/api/client";
import { getProfile, login as loginRequest, logout as logoutRequest, register as registerRequest } from "@/features/auth/api";
import type { CandidateProfile, LoginInput, RegisterInput } from "@/features/auth/types";

interface AuthContextValue {
  candidate: CandidateProfile | null;
  isPending: boolean;
  login: (input: LoginInput) => Promise<void>;
  register: (input: RegisterInput) => Promise<void>;
  logout: () => Promise<void>;
  refreshProfile: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [candidate, setCandidate] = useState<CandidateProfile | null>(null);
  const [isPending, setIsPending] = useState(() => Boolean(getAuthToken()));

  useEffect(() => {
    if (!getAuthToken()) {
      setIsPending(false);
      return;
    }

    let active = true;
    getProfile()
      .then((profile) => {
        if (active) {
          setCandidate(profile);
        }
      })
      .catch((error: unknown) => {
        if (active && error instanceof ApiError && error.status === 401) {
          clearAuthToken();
        }
      })
      .finally(() => {
        if (active) {
          setIsPending(false);
        }
      });

    return () => {
      active = false;
    };
  }, []);

  async function login(input: LoginInput) {
    const response = await loginRequest(input);
    setAuthToken(response.token);
    setCandidate(response.candidate);
  }

  async function register(input: RegisterInput) {
    const response = await registerRequest(input);
    setAuthToken(response.token);
    setCandidate(response.candidate);
  }

  async function logout() {
    try {
      await logoutRequest();
    } catch {
      // The local session is cleared even when the server is unreachable.
    } finally {
      clearAuthToken();
      setCandidate(null);
    }
  }

  async function refreshProfile() {
    const profile = await getProfile();
    setCandidate(profile);
  }

  return (
    <AuthContext.Provider value={{ candidate, isPending, login, register, logout, refreshProfile }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used inside AuthProvider");
  }

  return context;
}
