import { useEffect, useRef, useState, type KeyboardEvent } from "react";
import { useTasks, type SortKey } from "../../../context/TaskContext";
import type { Task } from "../../../api/tasks";
import { TasksSkeleton } from "./TasksSkeleton";

// How long the newly added row stays highlighted. Long enough to catch the
// eye, short enough not to linger while you type the next task.
const FLASH_MS = 1500;

// Each falls back to the name so the order is stable rather than
// dependent on whatever the server happened to return.
const COMPARATORS: Record<SortKey, (a: Task, b: Task) => number> = {
  // Ids come from an IDENTITY column, so ascending id *is* creation order.
  // Cheaper than carrying a timestamp the UI never displays.
  created: (a, b) => a.id - b.id,
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
    lastAddedId,
    clearLastAdded,
    toggleTask,
    renameTask,
    removeTask,
  } = useTasks();

  const [editingId, setEditingId] = useState<number | null>(null);
  const [draft, setDraft] = useState("");
  const handled = useRef(false);
  const addedRef = useRef<HTMLDivElement | null>(null);

  // Bring the new row into view if the list has been scrolled, then let the
  // highlight expire. `block: "nearest"` leaves the page alone when it is
  // already visible, which is the common case now that the sort is newest-first.
  useEffect(() => {
    if (lastAddedId === null) return;

    addedRef.current?.scrollIntoView?.({ block: "nearest" });
    const timer = setTimeout(clearLastAdded, FLASH_MS);
    return () => clearTimeout(timer);
  }, [lastAddedId, clearLastAdded]);

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

  const added = visibleTasks.find((task) => task.id === lastAddedId);

  return (
    <div className="flex flex-col">
      {/* The highlight is invisible to screen readers, so say it out loud. */}
      <p role="status" className="sr-only">
        {added ? `Added ${added.name}` : ""}
      </p>
      {visibleTasks.map((task, i) => {
        const isNew = task.id === lastAddedId;
        return (
          <div
            key={task.id}
            ref={isNew ? addedRef : undefined}
            data-new={isNew || undefined}
            className={`group flex gap-2 py-1 border-b border-gray-400 align-middle text-center items-center justify-between ${
              isNew ? "animate-task-flash" : ""
            }`}
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
        );
      })}
    </div>
  );
}
