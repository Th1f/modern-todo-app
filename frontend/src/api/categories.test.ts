import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  createCategory,
  deleteCategory,
  getCategories,
  updateCategory,
} from "./categories";
import { request } from "./client";

vi.mock("./client", () => ({
  request: vi.fn().mockResolvedValue(undefined),
}));

const requestMock = vi.mocked(request);

beforeEach(() => {
  requestMock.mockClear();
});

describe("getCategories", () => {
  it("GETs /categories", async () => {
    await getCategories();

    expect(requestMock).toHaveBeenCalledWith("/categories");
  });
});

describe("createCategory", () => {
  it("POSTs the category as JSON", async () => {
    await createCategory({ name: "Fitness", color: "#EF4444" });

    expect(requestMock).toHaveBeenCalledWith("/categories", {
      method: "POST",
      body: JSON.stringify({ name: "Fitness", color: "#EF4444" }),
    });
  });

  it("does not send an id", async () => {
    await createCategory({ name: "Fitness", color: "#EF4444" });

    const [, init] = requestMock.mock.calls[0];
    expect(JSON.parse(init!.body as string)).not.toHaveProperty("id");
  });
});

describe("updateCategory", () => {
  it("PATCHes /categories/:id", async () => {
    await updateCategory(3, { name: "Job" });

    expect(requestMock).toHaveBeenCalledWith("/categories/3", {
      method: "PATCH",
      body: JSON.stringify({ name: "Job" }),
    });
  });

  it("can patch the colour alone", async () => {
    await updateCategory(3, { color: "#10B981" });

    expect(requestMock).toHaveBeenCalledWith("/categories/3", {
      method: "PATCH",
      body: '{"color":"#10B981"}',
    });
  });
});

describe("deleteCategory", () => {
  it("DELETEs /categories/:id", async () => {
    await deleteCategory(3);

    expect(requestMock).toHaveBeenCalledWith("/categories/3", {
      method: "DELETE",
    });
  });
});
