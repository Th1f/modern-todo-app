import Categories from "../Categories/Categories";
import mockData from "../../data/mockData.json";



export function Header() {
  const mockTasks = mockData;
  const numDoneTasks = () =>{
    let res = 0;
    mockData.forEach((task) => {
      task.isDone ? res++ : res
    })
    return res
  }
  return (
    <>
      <header className="flex flex-col">
        <span className="p-font font-extralight text-muted text-xs">
          THE DAILY LEDGER
        </span>
        <div className="flex justify-between items-center">
          <span className="text-7xl italic">Todos.</span>
          <div className="flex flex-col items-end">
            <span className="italic text-accent text-xl">{numDoneTasks()} of {mockData.length} done</span>
            <span className="text-muted ">{mockData.length - numDoneTasks()} still open</span>
          </div>
        </div>
        <div className="border"></div>
      </header>
      <Categories />
    </>
  );
}
