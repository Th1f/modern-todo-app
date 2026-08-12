import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { getCurrentUser, login, logout, register } from "./auth";
import { ApiError } from "./client";

let fetchMock: ReturnType<typeof vi.fn>;

beforeEach(() => {
  fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 200 }));
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => {
  vi.unstubAllGlobals();
});

function lastInit() {
  return fetchMock.mock.calls[0][1] as RequestInit;
}

describe("login", () => {
  it("posts form-encoded credentials to /api/login", async () => {
    await login("alice", "password123");

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("/api/login");
    expect(init.method).toBe("POST");
    expect(init.headers).toEqual({
      "Content-Type": "application/x-www-form-urlencoded",
    });
    expect(String(init.body)).toBe("username=alice&password=password123");
  });

  it("sends the session cookie", async () => {
    await login("alice", "password123");

    expect(lastInit().credentials).toBe("include");
  });

  it("escapes characters that would break the form encoding", async () => {
    await login("alice", "p@ss word&more");

    expect(String(lastInit().body)).toBe(
      "username=alice&password=p%40ss+word%26more",
    );
  });

  it("throws ApiError with the status on bad credentials", async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 401 }));

    await expect(login("alice", "wrong")).rejects.toSatisfy(
      (error: unknown) => error instanceof ApiError && error.status === 401,
    );
  });

  it("resolves on success", async () => {
    await expect(login("alice", "password123")).resolves.toBeUndefined();
  });
});

describe("logout", () => {
  it("posts to /api/logout with the session cookie", async () => {
    await logout();

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("/api/logout");
    expect(init.method).toBe("POST");
    expect(init.credentials).toBe("include");
  });
});

describe("getCurrentUser", () => {
  it("GETs /api/me and returns the user", async () => {
    fetchMock.mockResolvedValue(
      new Response(JSON.stringify({ username: "alice" }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );

    await expect(getCurrentUser()).resolves.toEqual({ username: "alice" });
    expect(fetchMock.mock.calls[0][0]).toBe("/api/me");
  });

  it("rejects with 401 when the session has expired", async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 401 }));

    await expect(getCurrentUser()).rejects.toSatisfy(
      (error: unknown) => error instanceof ApiError && error.status === 401,
    );
  });
});

describe("register", () => {
  it("posts the username under the key the backend expects", async () => {
    fetchMock.mockResolvedValue(
      new Response(null, { status: 201, headers: { "content-length": "0" } }),
    );

    await register("Alice", "password123");

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("/api/register");
    expect(init.method).toBe("POST");
    expect(JSON.parse(init.body as string)).toEqual({
      username: "alice",
      password: "password123",
    });
  });

  it("lowercases the username", async () => {
    fetchMock.mockResolvedValue(
      new Response(null, { status: 201, headers: { "content-length": "0" } }),
    );

    await register("ALICE", "password123");

    const body = JSON.parse(fetchMock.mock.calls[0][1].body as string);
    expect(body.username).toBe("alice");
  });

  it("rejects with 409 when the username is taken", async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 409 }));

    await expect(register("alice", "password123")).rejects.toSatisfy(
      (error: unknown) => error instanceof ApiError && error.status === 409,
    );
  });
});
