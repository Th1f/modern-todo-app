import Categories from "../Categories/Categories";
import { useTasks } from "../../context/TaskProvider";
import { useAuth } from "../../context/AuthProvider";
import { Skeleton } from "../ui/Skeleton";

export function Header() {
  const { tasks, loading } = useTasks();
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
            {loading ? (
              <div role="status" className="flex flex-col items-end gap-2 py-1">
                <span className="sr-only">Loading your totals…</span>
                <Skeleton className="h-6 w-32" />
                <Skeleton className="h-5 w-24" />
              </div>
            ) : (
              <>
                <span className="italic text-accent text-xl">
                  {doneCount} of {tasks.length} done
                </span>
                <span className="text-muted ">
                  {tasks.length - doneCount} still open
                </span>
              </>
            )}
          </div>
        </div>
        <div className="border"></div>
      </header>
      <Categories />
    </>
  );
}
