import { render } from "@testing-library/react";
import type { ContextType, ReactElement } from "react";
import { vi } from "vitest";
import type { Category } from "../api/categories";
import type { Task } from "../api/tasks";
import { TaskContext } from "../context/TaskContext";

export type TaskContextValue = NonNullable<ContextType<typeof TaskContext>>;

export const work: Category = { id: 1, name: "Work", color: "#3B82F6" };
export const personal: Category = { id: 2, name: "Personal", color: "#10B981" };
export const errands: Category = { id: 3, name: "Errands", color: "#F59E0B" };

export function makeTask(
  id: number,
  name: string,
  category: Category = work,
  isDone = false,
): Task {
  return { id, name, category, isDone };
}

export function makeTaskContextValue(
  overrides: Partial<TaskContextValue> = {},
): TaskContextValue {
  return {
    tasks: [],
    categories: [],
    loading: false,
    error: null,
    selectedCategory: "all",
    setSelectedCategory: vi.fn(),
    sortBy: "created",
    setSortBy: vi.fn(),
    sortDirection: "desc",
    setSortDirection: vi.fn(),
    lastAddedId: null,
    clearLastAdded: vi.fn(),
    addTask: vi.fn().mockResolvedValue(undefined),
    addCategory: vi.fn().mockResolvedValue(undefined),
    toggleTask: vi.fn().mockResolvedValue(undefined),
    renameTask: vi.fn().mockResolvedValue(undefined),
    removeTask: vi.fn().mockResolvedValue(undefined),
    renameCategory: vi.fn().mockResolvedValue(undefined),
    removeCategory: vi.fn().mockResolvedValue(undefined),
    ...overrides,
  };
}

export function renderWithTasks(
  ui: ReactElement,
  overrides: Partial<TaskContextValue> = {},
) {
  const value = makeTaskContextValue(overrides);

  return {
    value,
    ...render(
      <TaskContext.Provider value={value}>{ui}</TaskContext.Provider>,
    ),
  };
}
