const API_URL = "http://localhost:8080/api";

export class ApiError extends Error {
  readonly status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

export async function request<T>(
  path: string,
  init?: RequestInit,
): Promise<T> {
  const response = await fetch(`${API_URL}${path}`, {
    headers: { "Content-Type": "application/json" },
    ...init,
  });

  if (!response.ok) {
    throw new ApiError(
      `${init?.method ?? "GET"} ${path} failed with ${response.status}`,
      response.status,
    );
  }
  return response.status === 204 ? (undefined as T) : response.json();
}
