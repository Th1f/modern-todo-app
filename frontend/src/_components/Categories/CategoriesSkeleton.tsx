import { Skeleton } from "../ui/Skeleton";

const WIDTHS = ["w-10", "w-14", "w-20", "w-16"];

export function CategoriesSkeleton({ tabs = 4 }: { tabs?: number }) {
  return (
    <div
      role="status"
      className="flex flex-col border border-gray-400 border-t-0 border-x-0 justify-between sm:flex-row"
    >
      <span className="sr-only">Loading categories…</span>
      <div className="flex gap-2 items-center">
        {Array.from({ length: tabs }, (_, i) => (
          <div key={i} className="flex items-center py-2 px-3">
            <Skeleton className={`h-5 ${WIDTHS[i % WIDTHS.length]}`} />
          </div>
        ))}
      </div>
      <div className="flex gap-1 items-center py-1">
        <Skeleton className="h-5 w-24" />
        <Skeleton className="h-5 w-5 rounded-full" />
        <Skeleton className="h-5 w-5 rounded-full" />
        <Skeleton className="h-5 w-5 rounded-full" />
        <Skeleton className="h-6 w-6 rounded-full" />
      </div>
    </div>
  );
}
