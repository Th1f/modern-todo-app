import { CategoriesSkeleton } from "../Categories/CategoriesSkeleton";
import { SummarySkeleton } from "../Content/Summary/SummarySkeleton";
import { TasksSkeleton } from "../Content/Tasks/TasksSkeleton";
import { Skeleton } from "./Skeleton";

export function PageSkeleton() {
  return (
    <div role="status">
      <span className="sr-only">Loading your ledger…</span>
      <header className="flex flex-col">
        <div className="flex justify-between">
          <span className="p-font font-extralight text-muted text-xs">
            THE DAILY LEDGER
          </span>
          <Skeleton className="h-8 w-20 rounded-none" />
        </div>
        <div className="flex justify-between items-center">
          <span className="text-7xl italic">Todos.</span>
          <div className="flex flex-col items-end gap-2">
            <Skeleton className="h-6 w-32" />
            <Skeleton className="h-5 w-24" />
          </div>
        </div>
        <div className="border"></div>
      </header>

      <CategoriesSkeleton />

      <div className="flex flex-col gap-10 sm:flex-row">
        <div className="flex flex-col sm:w-2/3 w-full">
          <div className="flex gap-3 my-10 w-full">
            <Skeleton className="h-8 flex-1" />
            <Skeleton className="h-8 w-24" />
            <Skeleton className="h-9 w-16 rounded-none" />
          </div>
          <TasksSkeleton />
        </div>
        <div className="border border-gray-300 mt-10"></div>
        <SummarySkeleton />
      </div>
    </div>
  );
}
