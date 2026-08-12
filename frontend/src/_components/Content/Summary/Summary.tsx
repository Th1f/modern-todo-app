import { useTasks } from "../../../context/TaskContext";
import { SummarySkeleton } from "./SummarySkeleton";

export function Summary() {
  const { tasks, categories, loading } = useTasks();

  if (loading) {
    return <SummarySkeleton />;
  }

  //Find Categories and its amount of completed and total jobs
  const rows = categories.map((category) => {
    const owned = tasks.filter((task) => task.category.id === category.id);
    return {
      ...category,
      totalJobs: owned.length,
      jobsDone: owned.filter((task) => task.isDone).length,
    };
  });

  return (
    <div className="flex flex-1 flex-col mt-10 w1/4 gap-4">
      <span className="text-muted">SUMMARY</span>
      {rows.map((row) => (
        <div key={row.id} className="flex flex-col gap-1">
          <div className="flex justify-between">
            <span>{row.name}</span>
            <span className="text-muted text-sm">
              {row.jobsDone}/{row.totalJobs}
            </span>
          </div>
          <div
            className="h-1 w-full overflow-hidden rounded-full bg-gray-500/30"
            role="progressbar"
            aria-label={`${row.name} tasks completed`}
            aria-valuenow={row.jobsDone}
            aria-valuemin={0}
            aria-valuemax={row.totalJobs}
          >
            <div
              className="h-full rounded-full transition-[width] duration-300"
              style={{
                width: `${row.totalJobs === 0 ? 0 : (row.jobsDone / row.totalJobs) * 100}%`,
                backgroundColor: row.color,
              }}
            />
          </div>
        </div>
      ))}
    </div>
  );
}
