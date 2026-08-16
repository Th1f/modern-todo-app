import { useRef, type KeyboardEvent } from "react";
import { useTasks, type SortKey } from "../../../context/TaskContext";

const OPTIONS: { key: SortKey; label: string }[] = [
  { key: "name", label: "Name" },
  { key: "category", label: "Category" },
  { key: "done", label: "Done" },
];

export function SortBy() {
  const { sortBy, setSortBy, sortDirection, setSortDirection } = useTasks();

  const buttonRef = useRef<(HTMLButtonElement | null)[]>([]);

  const focusButton = (i: number) => {
    buttonRef.current[i]?.focus();
  };

  // Pressing the active option flips the direction; moving to a different one
  // starts ascending, since carrying a descending sort across fields is
  // rarely what anyone means.
  const choose = (key: SortKey) => {
    if (key === sortBy) {
      setSortDirection((current) => (current === "asc" ? "desc" : "asc"));
    } else {
      setSortBy(key);
      setSortDirection("asc");
    }
  };

  const select = (i: number) => {
    choose(OPTIONS[i].key);
    focusButton(i);
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLButtonElement>, i: number) => {
    const last = OPTIONS.length - 1;

    switch (e.key) {
      case "ArrowRight":
      case "ArrowDown":
        e.preventDefault();
        select(i === last ? 0 : i + 1);
        break;
      case "ArrowLeft":
      case "ArrowUp":
        e.preventDefault();
        select(i === 0 ? last : i - 1);
        break;
      case "Home":
        e.preventDefault();
        select(0);
        break;
      case "End":
        e.preventDefault();
        select(last);
        break;
    }
  };

  return (
    <div
      className="flex gap-2 items-center"
      role="radiogroup"
      aria-label="Sort tasks by"
    >
      <span className="font-mono text-muted text-xs">SORT BY</span>
      {OPTIONS.map((option, i) => {
        const isSelected = option.key === sortBy;
        const ascending = sortDirection === "asc";
        return (
          <button
            key={option.key}
            type="button"
            role="radio"
            aria-checked={isSelected}
            // Roving tabindex: Tab reaches the group once, arrows move inside it.
            tabIndex={isSelected ? 0 : -1}
            ref={(el) => {
              buttonRef.current[i] = el;
            }}
            onClick={() => choose(option.key)}
            onKeyDown={(e) => handleKeyDown(e, i)}
            className={`flex items-center gap-1 border px-2 py-1 rounded-full text-xs cursor-pointer ${
              isSelected
                ? "border-accent text-accent"
                : "border-muted text-muted"
            }`}
          >
            {option.label}
            {isSelected && (
              <>
                <span aria-hidden="true">{ascending ? "↑" : "↓"}</span>
                <span className="sr-only">
                  , {ascending ? "ascending" : "descending"}
                </span>
              </>
            )}
          </button>
        );
      })}
    </div>
  );
}
