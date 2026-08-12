import { beforeEach, describe, expect, it, vi } from "vitest";
import { request } from "./client";
import {
  createTask,
  deleteTask,
  getTasks,
  toggleTask,
  updateTask,
} from "./tasks";

vi.mock("./client", () => ({
  request: vi.fn().mockResolvedValue(undefined),
}));

const requestMock = vi.mocked(request);

beforeEach(() => {
  requestMock.mockClear();
});

describe("getTasks", () => {
  it("GETs /tasks", async () => {
    await getTasks();

    expect(requestMock).toHaveBeenCalledWith("/tasks");
  });

  it("returns whatever the server sent", async () => {
    const tasks = [{ id: 1, name: "Buy milk" }];
    requestMock.mockResolvedValueOnce(tasks);

    await expect(getTasks()).resolves.toBe(tasks);
  });
});

describe("createTask", () => {
  it("POSTs the task as JSON", async () => {
    await createTask({ name: "Buy milk", categoryId: 2, isDone: false });

    expect(requestMock).toHaveBeenCalledWith("/tasks", {
      method: "POST",
      body: JSON.stringify({ name: "Buy milk", categoryId: 2, isDone: false }),
    });
  });
});

describe("updateTask", () => {
  it("PATCHes /tasks/:id", async () => {
    await updateTask(7, { name: "Renamed" });

    expect(requestMock).toHaveBeenCalledWith("/tasks/7", {
      method: "PATCH",
      body: JSON.stringify({ name: "Renamed" }),
    });
  });

  it("sends only the fields it was given", async () => {
    await updateTask(7, { isDone: true });

    expect(requestMock).toHaveBeenCalledWith("/tasks/7", {
      method: "PATCH",
      body: '{"isDone":true}',
    });
  });
});

describe("toggleTask", () => {
  it("PATCHes /tasks/:id/toggle with no body", async () => {
    await toggleTask(7);

    expect(requestMock).toHaveBeenCalledWith("/tasks/7/toggle", {
      method: "PATCH",
    });
  });
});

describe("deleteTask", () => {
  it("DELETEs /tasks/:id", async () => {
    await deleteTask(7);

    expect(requestMock).toHaveBeenCalledWith("/tasks/7", { method: "DELETE" });
  });
});
