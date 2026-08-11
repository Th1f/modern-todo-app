import Categories from "../Categories/Categories";
import { useTasks } from "../../context/TaskProvider";

export function Header() {
  const { tasks } = useTasks();

  const doneCount = tasks.filter((task) => task.isDone).length;

  return (
    <>
      <header className="flex flex-col">
        <span className="p-font font-extralight text-muted text-xs">
          THE DAILY LEDGER
        </span>
        <div className="flex justify-between items-center">
          <span className="text-7xl italic">Todos.</span>
          <div className="flex flex-col items-end">
            <span className="italic text-accent text-xl">
              {doneCount} of {tasks.length} done
            </span>
            <span className="text-muted ">
              {tasks.length - doneCount} still open
            </span>
          </div>
        </div>
        <div className="border"></div>
      </header>
      <Categories />
    </>
  );
}
