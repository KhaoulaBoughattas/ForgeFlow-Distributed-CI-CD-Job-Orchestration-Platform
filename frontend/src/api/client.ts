const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

let authToken: string | null = null;

/** Called by AuthContext whenever the session token changes (including on logout, with null). */
export function setAuthToken(token: string | null): void {
  authToken = token;
}

export class ApiError extends Error {
  readonly status: number;
  readonly body: unknown;

  constructor(status: number, body: unknown, message: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.body = body;
  }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set('Content-Type', 'application/json');
  if (authToken) {
    headers.set('Authorization', `Bearer ${authToken}`);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, { ...init, headers });

  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();
  const body = text ? JSON.parse(text) : undefined;

  if (!response.ok) {
    const message =
      (body && typeof body === 'object' && 'detail' in body && String((body as { detail: unknown }).detail)) ||
      (body && typeof body === 'object' && 'title' in body && String((body as { title: unknown }).title)) ||
      `Request failed with status ${response.status}`;
    throw new ApiError(response.status, body, message);
  }

  return body as T;
}

export const apiClient = {
  get: <T>(path: string) => request<T>(path, { method: 'GET' }),
  post: <T>(path: string, data?: unknown) =>
    request<T>(path, { method: 'POST', body: data !== undefined ? JSON.stringify(data) : undefined }),
  put: <T>(path: string, data?: unknown) =>
    request<T>(path, { method: 'PUT', body: data !== undefined ? JSON.stringify(data) : undefined }),
  delete: <T>(path: string) => request<T>(path, { method: 'DELETE' }),
};
