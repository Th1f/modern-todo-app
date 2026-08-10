import { Summary } from "./Summary/Summary";
import { NewTask } from "./NewTask/NewTask";
import { Tasks } from "./Tasks/Tasks";

export function Content() {
  return (
    <div className="flex gap-10">
      <div className="flex flex-col w-2/3">
        <NewTask />
        <Tasks />
      </div>
      <div className="border border-gray-300 mt-10"></div>
      <Summary />
    </div>
  );
}
