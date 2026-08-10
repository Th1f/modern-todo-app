import { useContext, useState } from "react";
import mockData from "../../../data/mockData.json";
import { TaskContext } from "../../../context/TaskProvider";

interface Task {
  id: string;
  name: string;
  color: string;
  category: string;
  isDone: boolean;
}

export function Tasks() {
  const { selectedCategory } = useContext(TaskContext) ?? {};
  const [mockTasks, setMockTasks] = useState<Task[]>(mockData);

  const handleChangeDone = (id: string) => {
    const mutated = mockTasks.map((task) =>
      task.id === id ? { ...task, isDone: !task.isDone } : task,
    );
    setMockTasks(mutated);
  };

  const visibleTasks =
    !selectedCategory || selectedCategory === "all"
      ? [...mockTasks].sort((a, b) => a.category.localeCompare(b.category))
      : mockTasks.filter(
          (task) => task.category.toLowerCase() === selectedCategory,
        );

  return (
    <div className="flex flex-col">
      {visibleTasks.map((task, i) => (
        <div
          key={task.id}
          className="flex gap-2 py-1 border-b border-gray-400 align-middle text-center items-center justify-between"
        >
          <div className="flex items-center gap-5">
            <span className="text-muted text-sm">
              {i < 9 ? "0" : ""}
              {i + 1}
            </span>
            <input
              type="checkbox"
              name=""
              id={task.id}
              checked={task.isDone}
              onChange={() => handleChangeDone(task.id)}
              className="rounded-full accent-accent h-5 w-5 self-center border-gray-400"
            />
            <label
              htmlFor={task.id}
              className={`text-xl ${task.isDone ? "decoration-accent line-through text-muted" : ""}`}
            >
              {task.name}
            </label>
          </div>
          <div>
            <span
              className={`text-[0.8rem] font-mono`}
              style={{ color: task.color }}
            >
              {task.category.toUpperCase()}
            </span>
          </div>
        </div>
      ))}
    </div>
  );
}
