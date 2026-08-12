import { useEffect, useState, type ReactNode } from "react";
import * as authApi from "../api/auth.ts";
import type { CurrentUser } from "../api/auth.ts";
import { AuthContext } from "./AuthContext";

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    authApi
      .getCurrentUser()
      .then(setUser)
      .catch(() => setUser(null))
      .finally(() => setLoading(false));
  }, []);

  const signIn = async (username: string, password: string) => {
    await authApi.login(username, password);
    setUser(await authApi.getCurrentUser());
  };

  const register = async (username: string, password: string) => {
    await authApi.register(username, password);
  };

  const signOut = async () => {
    await authApi.logout();
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, loading, signIn, register, signOut }}>
      {children}
    </AuthContext.Provider>
  );
}
