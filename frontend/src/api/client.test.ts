import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiError, request } from "./client";

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function mockFetch(response: Response) {
  const fetchMock = vi.fn().mockResolvedValue(response);
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("request", () => {
  it("prefixes the path with /api and sends the session cookie", async () => {
    const fetchMock = mockFetch(jsonResponse([{ id: 1 }]));

    await request("/tasks");

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/tasks",
      expect.objectContaining({
        credentials: "include",
        headers: { "Content-Type": "application/json" },
      }),
    );
  });

  it("parses a JSON body", async () => {
    mockFetch(jsonResponse([{ id: 1, name: "Buy milk" }]));

    await expect(request("/tasks")).resolves.toEqual([
      { id: 1, name: "Buy milk" },
    ]);
  });

  it("returns undefined for 204 instead of parsing an empty body", async () => {
    mockFetch(new Response(null, { status: 204 }));

    await expect(request("/tasks/1", { method: "DELETE" })).resolves.toBeUndefined();
  });

  it("returns undefined when content-length is 0", async () => {
    mockFetch(new Response("", { status: 201, headers: { "content-length": "0" } }));

    await expect(request("/register", { method: "POST" })).resolves.toBeUndefined();
  });

  it("throws ApiError carrying the status code", async () => {
    mockFetch(jsonResponse({ message: "nope" }, 409));

    await expect(request("/register", { method: "POST" })).rejects.toSatisfy(
      (error: unknown) => error instanceof ApiError && error.status === 409,
    );
  });

  it("names the method and path in the error message", async () => {
    mockFetch(jsonResponse({}, 404));

    await expect(request("/tasks/99", { method: "PATCH" })).rejects.toThrow(
      "PATCH /tasks/99 failed with 404",
    );
  });

  it("lets the caller override the method and body", async () => {
    const fetchMock = mockFetch(jsonResponse({ id: 1 }, 201));

    await request("/tasks", {
      method: "POST",
      body: JSON.stringify({ name: "Buy milk" }),
    });

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/tasks",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({ name: "Buy milk" }),
      }),
    );
  });
});
