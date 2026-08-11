import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
  type Dispatch,
  type ReactNode,
  type SetStateAction,
} from "react";
import * as categoriesApi from "../api/categories";
import * as tasksApi from "../api/tasks";
import type { Category, NewCategoryInput } from "../api/categories";
import type { NewTaskInput, Task } from "../api/tasks";

type TaskContextType = {
  tasks: Task[];
  categories: Category[];
  loading: boolean;
  error: string | null;
  selectedCategory: string;
  setSelectedCategory: Dispatch<SetStateAction<string>>;
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

type TaskProviderProps = {
  children: ReactNode;
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

export function TaskProvider({ children }: TaskProviderProps) {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedCategory, setSelectedCategory] = useState("all");

  //Get all task and categories at page load
  useEffect(() => {
    let cancelled = false;
    Promise.all([tasksApi.getTasks(), categoriesApi.getCategories()])
      .then(([taskList, categoryList]) => {
        if (cancelled) return;
        setTasks(taskList);
        setCategories(categoryList);
      })
      .catch((err: Error) => {
        if (!cancelled) setError(err.message);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const addTask = useCallback(async (input: NewTaskInput) => {
    const created = await tasksApi.createTask(input);
    setTasks((prev) => [...prev, created]);
  }, []);

  const addCategory = useCallback(async (input: NewCategoryInput) => {
    const created = await categoriesApi.createCategory(input);
    setCategories((prev) =>
      [...prev, created].sort((a, b) => a.name.localeCompare(b.name)),
    );
  }, []);

  const toggleTask = useCallback(async (id: number) => {
    const flip = (list: Task[]) =>
      list.map((task) =>
        task.id === id ? { ...task, isDone: !task.isDone } : task,
      );

    // Flip immediately so the checkbox feels instant, then reconcile with
    // whatever the server actually stored. Roll back if the request fails.
    setTasks(flip);
    try {
      const updated = await tasksApi.toggleTask(id);
      setTasks((prev) => prev.map((t) => (t.id === id ? updated : t)));
    } catch (err) {
      setTasks(flip);
      setError((err as Error).message);
    }
  }, []);

  const renameTask = useCallback(async (id: number, name: string) => {
    const updated = await tasksApi.updateTask(id, { name });
    setTasks((prev) => prev.map((task) => (task.id === id ? updated : task)));
  }, []);

  const removeTask = useCallback(async (id: number) => {
    await tasksApi.deleteTask(id);
    setTasks((prev) => prev.filter((task) => task.id !== id));
  }, []);

  const renameCategory = useCallback(async (id: number, name: string) => {
    const updated = await categoriesApi.updateCategory(id, { name });
    setCategories((prev) =>
      prev
        .map((category) => (category.id === id ? updated : category))
        .sort((a, b) => a.name.localeCompare(b.name)),
    );
    setTasks((prev) =>
      prev.map((task) =>
        task.category.id === id ? { ...task, category: updated } : task,
      ),
    );
  }, []);

  const removeCategory = useCallback(
    async (id: number) => {
      const removed = categories.find((category) => category.id === id);
      await categoriesApi.deleteCategory(id);
      setCategories((prev) => prev.filter((category) => category.id !== id));
      // Don't leave the tab bar filtering by a category that no longer exists.
      if (removed && selectedCategory === removed.name.toLowerCase()) {
        setSelectedCategory("all");
      }
    },
    [categories, selectedCategory],
  );

  return (
    <TaskContext.Provider
      value={{
        tasks,
        categories,
        loading,
        error,
        selectedCategory,
        setSelectedCategory,
        addTask,
        addCategory,
        toggleTask,
        renameTask,
        removeTask,
        renameCategory,
        removeCategory,
      }}
    >
      {children}
    </TaskContext.Provider>
  );
}
