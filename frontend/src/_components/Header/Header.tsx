import Categories from "../Categories/Categories";
import { useTasks } from "../../context/TaskProvider";
import { useAuth } from "../../context/AuthProvider";

export function Header() {
  const { tasks } = useTasks();
  const{signOut} = useAuth();
  const doneCount = tasks.filter((task) => task.isDone).length;

  return (
    <>
      <header className="flex flex-col">
        <div className="flex justify-between">
          <span className="p-font font-extralight text-muted text-xs">
            THE DAILY LEDGER
          </span>
          <button className="bg-accent text-white font-mono px-3 py-1 hover:cursor-pointer hover:bg-amber-800" onClick={() => signOut()}>Logout</button>
        </div>
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
