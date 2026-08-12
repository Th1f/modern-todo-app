import { Skeleton } from "../../ui/Skeleton";

const WIDTHS = ["w-64", "w-44", "w-72", "w-52", "w-60"];

export function TasksSkeleton({ rows = 5 }: { rows?: number }) {
  return (
    <div role="status" className="flex flex-col">
      <span className="sr-only">Loading tasks…</span>
      {Array.from({ length: rows }, (_, i) => (
        <div
          key={i}
          className="flex gap-2 py-1 border-b border-gray-400 items-center justify-between"
        >
          <div className="flex items-center gap-5 min-w-0 flex-1">
            <Skeleton className="h-3.5 w-4" />
            <Skeleton className="h-5 w-5 rounded-full" />
            <Skeleton className={`h-6 max-w-full ${WIDTHS[i % WIDTHS.length]}`} />
          </div>
          <Skeleton className="h-3 w-16 shrink-0" />
        </div>
      ))}
    </div>
  );
}
