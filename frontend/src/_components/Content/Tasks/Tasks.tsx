import { useRef, useState, type KeyboardEvent } from "react";
import { useTasks, type SortKey } from "../../../context/TaskContext";
import type { Task } from "../../../api/tasks";
import { TasksSkeleton } from "./TasksSkeleton";

// Each falls back to the name so the order is stable rather than
// dependent on whatever the server happened to return.
const COMPARATORS: Record<SortKey, (a: Task, b: Task) => number> = {
  name: (a, b) => a.name.localeCompare(b.name),
  category: (a, b) =>
    a.category.name.localeCompare(b.category.name) ||
    a.name.localeCompare(b.name),
  done: (a, b) => Number(a.isDone) - Number(b.isDone) || a.name.localeCompare(b.name),
};

export function Tasks() {
  const {
    tasks,
    loading,
    error,
    selectedCategory,
    sortBy,
    sortDirection,
    toggleTask,
    renameTask,
    removeTask,
  } = useTasks();

  const [editingId, setEditingId] = useState<number | null>(null);
  const [draft, setDraft] = useState("");
  const handled = useRef(false);
  const startEdit = (id: number, name: string) => {
    handled.current = false;
    setEditingId(id);
    setDraft(name);
  };

  const finishEdit = async (id: number, original: string, save: boolean) => {
    if (handled.current) return;
    handled.current = true;

    const name = draft.trim();
    setEditingId(null);

    if (save && name && name !== original) {
      await renameTask(id, name);
    }
  };

  const handleKeyDown = (
    e: KeyboardEvent<HTMLInputElement>,
    id: number,
    original: string,
  ) => {
    if (e.key === "Enter") finishEdit(id, original, true);
    if (e.key === "Escape") finishEdit(id, original, false);
  };

  if (loading) {
    return <TasksSkeleton />;
  }

  if (error) {
    return (
      <div className="text-red-600 py-4">
        Could not reach the API: {error}
        <div className="text-muted text-sm mt-1">
          Is the backend running on http://localhost:8080?
        </div>
      </div>
    );
  }

  const showingAll = !selectedCategory || selectedCategory === "all";
  const direction = sortDirection === "asc" ? 1 : -1;
  const visibleTasks = tasks
    .filter(
      (task) =>
        showingAll || task.category.name.toLowerCase() === selectedCategory,
    )
    .sort((a, b) => direction * COMPARATORS[sortBy](a, b));

  if (visibleTasks.length === 0) {
    return <div className="text-muted py-4 italic text-center">Nothing here yet...</div>;
  }

  return (
    <div className="flex flex-col">
      {visibleTasks.map((task, i) => (
        <div
          key={task.id}
          className="group flex gap-2 py-1 border-b border-gray-400 align-middle text-center items-center justify-between"
        >
          <div className="flex items-center gap-5 min-w-0 flex-1">
            <span className="text-muted text-sm">
              {i < 9 ? "0" : ""}
              {i + 1}
            </span>
            <input
              type="checkbox"
              id={`task-${task.id}`}
              checked={task.isDone}
              onChange={() => toggleTask(task.id)}
              className="rounded-full accent-accent h-5 w-5 self-center border-gray-400"
            />
            {editingId === task.id ? (
              <input
                type="text"
                value={draft}
                autoFocus
                onChange={(e) => setDraft(e.target.value)}
                onKeyDown={(e) => handleKeyDown(e, task.id, task.name)}
                onBlur={() => finishEdit(task.id, task.name, true)}
                className="text-xl flex-1 min-w-0 focus:outline-0 border-b border-accent bg-transparent"
              />
            ) : (
              <label
                htmlFor={`task-${task.id}`}
                onDoubleClick={() => startEdit(task.id, task.name)}
                className={`text-xl truncate ${task.isDone ? "decoration-accent line-through text-muted" : ""}`}
              >
                {task.name}
              </label>
            )}
          </div>

          <div className="flex items-center gap-3 shrink-0">
            <span
              className="text-[0.8rem] font-mono"
              style={{ color: task.category.color }}
            >
              {task.category.name.toUpperCase()}
            </span>

            <div className="flex gap-1 opacity-0 group-hover:opacity-100 focus-within:opacity-100 transition-opacity">
              <button
                type="button"
                onClick={() => startEdit(task.id, task.name)}
                aria-label={`Rename ${task.name}`}
                title="Rename"
                className="w-6 h-6 text-sm text-muted rounded-full hover:bg-black hover:text-white hover:cursor-pointer"
              >
                ✎
              </button>
              <button
                type="button"
                onClick={() => {
                  if (window.confirm(`Delete "${task.name}"?`)) {
                    removeTask(task.id);
                  }
                }}
                aria-label={`Delete ${task.name}`}
                title="Delete"
                className="w-6 h-6 text-sm text-muted rounded-full hover:bg-red-600 hover:text-white hover:cursor-pointer"
              >
                ✕
              </button>
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}
