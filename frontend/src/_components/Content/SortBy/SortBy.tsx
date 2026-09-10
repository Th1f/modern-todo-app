import { useRef, type KeyboardEvent } from "react";
import {
  useTasks,
  type SortDirection,
  type SortKey,
} from "../../../context/TaskContext";

// Each field carries the direction it reads naturally in: newest first for
// Created, A-Z or unfinished-first for the rest.
const OPTIONS: {
  key: SortKey;
  label: string;
  defaultDirection: SortDirection;
}[] = [
  { key: "created", label: "Created", defaultDirection: "desc" },
  { key: "name", label: "Name", defaultDirection: "asc" },
  { key: "category", label: "Category", defaultDirection: "asc" },
  { key: "done", label: "Done", defaultDirection: "asc" },
];

export function SortBy() {
  const { sortBy, setSortBy, sortDirection, setSortDirection } = useTasks();

  const buttonRef = useRef<(HTMLButtonElement | null)[]>([]);

  const focusButton = (i: number) => {
    buttonRef.current[i]?.focus();
  };

  // Pressing the active option flips the direction; moving to a different one
  // starts at that field's default, since carrying a direction across fields
  // is rarely what anyone means.
  const choose = (i: number) => {
    const option = OPTIONS[i];
    if (option.key === sortBy) {
      setSortDirection((current) => (current === "asc" ? "desc" : "asc"));
    } else {
      setSortBy(option.key);
      setSortDirection(option.defaultDirection);
    }
  };

  const select = (i: number) => {
    choose(i);
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
            onClick={() => choose(i)}
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
