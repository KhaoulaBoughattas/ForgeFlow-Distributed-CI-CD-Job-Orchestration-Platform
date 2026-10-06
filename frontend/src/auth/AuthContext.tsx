import { createContext, useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import { apiClient, setAuthToken } from '../api/client';
import type { AuthResponse, LoginRequest, RegisterRequest, UserSummary } from '../api/types';

interface StoredSession {
  token: string;
  user: UserSummary;
}

interface AuthContextValue {
  user: UserSummary | null;
  token: string | null;
  isLoading: boolean;
  login: (request: LoginRequest) => Promise<void>;
  register: (request: RegisterRequest) => Promise<void>;
  logout: () => void;
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined);

const STORAGE_KEY = 'forgeflow.session';

function loadStoredSession(): StoredSession | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as StoredSession) : null;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<StoredSession | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const stored = loadStoredSession();
    if (stored) {
      setAuthToken(stored.token);
      setSession(stored);
    }
    setIsLoading(false);
  }, []);

  const persist = useCallback((next: StoredSession | null) => {
    setSession(next);
    setAuthToken(next?.token ?? null);
    if (next) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
    } else {
      localStorage.removeItem(STORAGE_KEY);
    }
  }, []);

  const login = useCallback(
    async (request: LoginRequest) => {
      const response = await apiClient.post<AuthResponse>('/api/v1/auth/login', request);
      persist({ token: response.token, user: response.user });
    },
    [persist],
  );

  const register = useCallback(
    async (request: RegisterRequest) => {
      const response = await apiClient.post<AuthResponse>('/api/v1/auth/register', request);
      persist({ token: response.token, user: response.user });
    },
    [persist],
  );

  const logout = useCallback(() => {
    persist(null);
  }, [persist]);

  const value = useMemo<AuthContextValue>(
    () => ({
      user: session?.user ?? null,
      token: session?.token ?? null,
      isLoading,
      login,
      register,
      logout,
    }),
    [session, isLoading, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
