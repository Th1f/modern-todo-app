import type { Category } from "./categories";
import { request } from "./client";

export interface Task {
  id: number;
  name: string;
  isDone: boolean;
  category: Category;
}

export interface NewTaskInput {
  name: string;
  categoryId: number;
  isDone: boolean;
}

export const getTasks = () => request<Task[]>("/tasks");

export const createTask = (task: NewTaskInput) =>
  request<Task>("/tasks", { method: "POST", body: JSON.stringify(task) });

export interface TaskPatch {
  name?: string;
  categoryId?: number;
  isDone?: boolean;
}

export const updateTask = (id: number, patch: TaskPatch) =>
  request<Task>(`/tasks/${id}`, {
    method: "PATCH",
    body: JSON.stringify(patch),
  });

export const toggleTask = (id: number) =>
  request<Task>(`/tasks/${id}/toggle`, { method: "PATCH" });

export const deleteTask = (id: number) =>
  request<void>(`/tasks/${id}`, { method: "DELETE" });
