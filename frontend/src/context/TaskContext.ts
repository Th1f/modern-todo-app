import {
  createContext,
  useContext,
  type Dispatch,
  type SetStateAction,
} from "react";
import type { Category, NewCategoryInput } from "../api/categories";
import type { NewTaskInput, Task } from "../api/tasks";

export type SortKey = "created" | "name" | "category" | "done";
export type SortDirection = "asc" | "desc";

export type TaskContextType = {
  tasks: Task[];
  categories: Category[];
  loading: boolean;
  error: string | null;
  selectedCategory: string;
  setSelectedCategory: Dispatch<SetStateAction<string>>;
  sortBy: SortKey;
  setSortBy: Dispatch<SetStateAction<SortKey>>;
  sortDirection: SortDirection;
  setSortDirection: Dispatch<SetStateAction<SortDirection>>;
  // The task added most recently, so the list can point it out instead of
  // leaving the user to hunt for it. Null once the list has done so.
  lastAddedId: number | null;
  clearLastAdded: () => void;
  //Create Operations
  addTask: (input: NewTaskInput) => Promise<void>;
  addCategory: (input: NewCategoryInput) => Promise<void>;

  //Update Operations
  toggleTask: (id: number) => Promise<void>;
  renameTask: (id: number, name: string) => Promise<void>;
  removeTask: (id: number) => Promise<void>;

  //Delete Operations
  renameCategory: (id: number, name: string) => Promise<void>;
  removeCategory: (id: number) => Promise<void>;
};

export const TaskContext = createContext<TaskContextType | undefined>(
  undefined,
);

export function useTasks() {
  const context = useContext(TaskContext);
  if (!context) {
    throw new Error("useTasks must be used inside a <TaskProvider>");
  }
  return context;
}
