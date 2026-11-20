const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "/api/v1";
const AUTH_TOKEN_KEY = "psa.auth.token";

interface ApiErrorBody {
  error?: string;
  message?: string;
  status?: number;
  requestId?: string;
}

export class ApiError extends Error {
  readonly status: number;
  readonly code?: string;
  readonly requestId?: string;

  constructor(message: string, status: number, code?: string, requestId?: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
    this.requestId = requestId;
  }
}

export function getAuthToken(): string | null {
  return window.localStorage.getItem(AUTH_TOKEN_KEY);
}

export function setAuthToken(token: string): void {
  window.localStorage.setItem(AUTH_TOKEN_KEY, token);
}

export function clearAuthToken(): void {
  window.localStorage.removeItem(AUTH_TOKEN_KEY);
}

interface RequestOptions {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  body?: unknown;
  bodyFormData?: FormData;
  signal?: AbortSignal;
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers = new Headers({ Accept: "application/json" });
  const token = getAuthToken();

  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }
  if (options.body !== undefined) {
    headers.set("Content-Type", "application/json");
  }

  let response: Response;

  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method: options.method ?? "GET",
      signal: options.signal,
      headers,
      body: options.bodyFormData ?? (options.body === undefined ? undefined : JSON.stringify(options.body)),
    });
  } catch (caught) {
    if (caught instanceof DOMException && caught.name === "AbortError") {
      throw caught;
    }
    throw new ApiError("We couldn't reach the service. Check your connection and try again.", 0, "NETWORK_ERROR");
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();
  let body: unknown;

  if (text) {
    try {
      body = JSON.parse(text);
    } catch {
      body = undefined;
    }
  }

  if (!response.ok) {
    const errorBody = body as ApiErrorBody | undefined;

    throw new ApiError(
      errorBody?.message ?? `Request failed with status ${response.status}`,
      response.status,
      errorBody?.error,
      errorBody?.requestId ?? response.headers.get("X-Request-Id") ?? undefined,
    );
  }

  return body as T;
}

export function apiGet<T>(path: string, signal?: AbortSignal): Promise<T> {
  return request<T>(path, { signal });
}

export function apiPost<T>(path: string, body?: unknown, signal?: AbortSignal): Promise<T> {
  return request<T>(path, { method: "POST", body, signal });
}

export function apiPut<T>(path: string, body?: unknown, signal?: AbortSignal): Promise<T> {
  return request<T>(path, { method: "PUT", body, signal });
}

export function apiPatch<T>(path: string, body?: unknown, signal?: AbortSignal): Promise<T> {
  return request<T>(path, { method: "PATCH", body, signal });
}

export function apiDelete<T>(path: string, signal?: AbortSignal): Promise<T> {
  return request<T>(path, { method: "DELETE", signal });
}

export function apiUpload<T>(path: string, body: FormData, signal?: AbortSignal): Promise<T> {
  return request<T>(path, { method: "POST", bodyFormData: body, signal });
}
