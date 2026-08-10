import mockTasks from "../../../data/mockData.json";

export function Summary() {
  const categories = Array.from(
    new Set(mockTasks.map((task) => task.category)),
  ).map((name) => ({
    name,
    color: mockTasks.find((task) => task.category === name)!.color,
    totalJobs: mockTasks.filter((task) => task.category === name).length,
    jobsDone: mockTasks.filter((task) => task.category === name && task.isDone)
      .length,
  }));
  return (
    <div className="flex flex-1 flex-col mt-10 w1/4 gap-4">
      <span className="text-muted">SUMMARY</span>
      {categories.map((cat) => (
        <div key={cat.name} className="flex flex-col gap-1">
          <div className="flex justify-between">
            <span>{cat.name}</span>
            <span className="text-muted text-sm">
              {cat.jobsDone}/{cat.totalJobs}
            </span>
          </div>
          <div
            className="h-1 w-full overflow-hidden rounded-full bg-gray-500/30"
            role="progressbar"
            aria-label={`${cat.name} tasks completed`}
            aria-valuenow={cat.jobsDone}
            aria-valuemin={0}
            aria-valuemax={cat.totalJobs}
          >
            <div
              className="h-full rounded-full transition-[width] duration-300"
              style={{
                width: `${(cat.jobsDone / cat.totalJobs) * 100}%`,
                backgroundColor: cat.color,
              }}
            />
          </div>
        </div>
      ))}
    </div>
  );
}
