import { Skeleton } from "../../ui/Skeleton";

export function SummarySkeleton({ rows = 3 }: { rows?: number }) {
  return (
    <div role="status" className="flex flex-1 flex-col mt-10 w1/4 gap-4">
      <span className="sr-only">Loading summary…</span>
      <span className="text-muted">SUMMARY</span>
      {Array.from({ length: rows }, (_, i) => (
        <div key={i} className="flex flex-col gap-1">
          <div className="flex justify-between">
            <Skeleton className="h-5 w-24" />
            <Skeleton className="h-4 w-8" />
          </div>
          <Skeleton className="h-1 w-full rounded-full" />
        </div>
      ))}
    </div>
  );
}
