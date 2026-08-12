import { useRef, useState, type KeyboardEvent } from "react";
import { NewCategory } from "./NewCategory";
import { CategoriesSkeleton } from "./CategoriesSkeleton";
import { ApiError } from "../../api/client";
import { useTasks } from "../../context/TaskProvider";

export default function Categories() {
  const {
    categories,
    tasks,
    loading,
    selectedCategory,
    setSelectedCategory,
    renameCategory,
    removeCategory,
  } = useTasks();

  const [editingId, setEditingId] = useState<number | null>(null);
  const [draft, setDraft] = useState("");
  const [error, setError] = useState<string | null>(null);
  const handled = useRef(false);

  const startEdit = (id: number, name: string) => {
    handled.current = false;
    setError(null);
    setEditingId(id);
    setDraft(name);
  };

  const finishEdit = async (id: number, original: string, save: boolean) => {
    if (handled.current) return;
    handled.current = true;

    const name = draft.trim();
    setEditingId(null);
    if (!save || !name || name === original) return;

    try {
      await renameCategory(id, name);
      if (selectedCategory === original.toLowerCase()) {
        setSelectedCategory(name.toLowerCase());
      }
    } catch (err) {
      setError(
        err instanceof ApiError && err.status === 409
          ? `"${name}" already exists`
          : "Could not rename that category",
      );
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

  const handleDelete = async (id: number, name: string) => {
    const owned = tasks.filter((task) => task.category.id === id).length;
    if (owned > 0) {
      setError(`"${name}" still has ${owned} task${owned === 1 ? "" : "s"}`);
      return;
    }
    if (!window.confirm(`Delete the "${name}" category?`)) return;

    setError(null);
    try {
      await removeCategory(id);
    } catch (err) {
      setError(
        err instanceof ApiError && err.status === 409
          ? `"${name}" is still in use`
          : "Could not delete that category",
      );
    }
  };

  if (loading) {
    return <CategoriesSkeleton />;
  }

  const tabClass = (isSelected: boolean) =>
    "flex items-center py-2 px-3 hover:text-black hover:cursor-pointer hover:border-b hover:border-b-accent border-0 " +
    (isSelected
      ? "text-black border-b border-b-accent border-0 font-bold"
      : "text-muted");

  return (
    <>
      <div className="flex flex-col  border border-gray-400 border-t-0 border-x-0 justify-between overflow-y-auto sm:flex-row">
        <div className="flex gap-2 items-center">
          <span
            className={tabClass(selectedCategory === "all")}
            onClick={() => setSelectedCategory("all")}
          >
            All
          </span>

          {categories.map((category) => {
            const value = category.name.toLowerCase();
            const isSelected = selectedCategory === value;

            if (editingId === category.id) {
              return (
                <input
                  key={category.id}
                  type="text"
                  value={draft}
                  autoFocus
                  onChange={(e) => setDraft(e.target.value)}
                  onKeyDown={(e) =>
                    handleKeyDown(e, category.id, category.name)
                  }
                  onBlur={() => finishEdit(category.id, category.name, true)}
                  className="my-1 w-24 px-2 focus:outline-0 border-b border-accent bg-transparent"
                />
              );
            }

            return (
              <span
                key={category.id}
                className={tabClass(isSelected)}
                onClick={() => setSelectedCategory(value)}
                onDoubleClick={() => startEdit(category.id, category.name)}
              >
                {category.name}

                {isSelected && (
                  <span className="flex gap-1 ml-2">
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        startEdit(category.id, category.name);
                      }}
                      aria-label={`Rename ${category.name}`}
                      title="Rename"
                      className="w-5 h-5 text-xs font-normal text-muted rounded-full hover:bg-black hover:text-white"
                    >
                      ✎
                    </button>
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        handleDelete(category.id, category.name);
                      }}
                      aria-label={`Delete ${category.name}`}
                      title="Delete"
                      className="w-5 h-5 text-xs font-normal text-muted rounded-full hover:bg-red-600 hover:text-white"
                    >
                      ✕
                    </button>
                  </span>
                )}
              </span>
            );
          })}
        </div>
        <NewCategory />
      </div>
      {error && <div className="text-xs text-red-600 mt-1">{error}</div>}
    </>
  );
}
