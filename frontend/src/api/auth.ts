import { ApiError, request } from "./client";

const API_URL = "/api";

export interface CurrentUser {
  username: string;
}

export async function login(username: string, password: string): Promise<void> {
  const res = await fetch(`${API_URL}/login`, {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ username, password }),
  });

  if (!res.ok) {
    throw new ApiError("Invalid username or password", res.status);
  }
}

export async function logout(): Promise<void> {
  await fetch(`${API_URL}/logout`, {
    method: "POST",
    credentials: "include",
  });
}

export const getCurrentUser = () => request<CurrentUser>("/me");

export const register = (username: string, password: string) => {
  const lower = username.toLowerCase();
  request<void>("/register", {
    method: "POST",
    body: JSON.stringify({ lower, password }),
  });
};
