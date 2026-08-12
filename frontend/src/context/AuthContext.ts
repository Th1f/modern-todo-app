import { createContext, useContext } from "react";
import type { CurrentUser } from "../api/auth.ts";

export type AuthContextType = {
  user: CurrentUser | null;
  loading: boolean;
  signIn: (username: string, password: string) => Promise<void>;
  register: (username: string, password: string) => Promise<void>;
  signOut: () => Promise<void>;
};

export const AuthContext = createContext<AuthContextType | undefined>(
  undefined,
);

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used inside the provider");
  return context;
}
