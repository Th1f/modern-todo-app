import { Summary } from "./Summary/Summary";
import { NewTask } from "./NewTask/NewTask";
import { Tasks } from "./Tasks/Tasks";
import { SortBy } from "./SortBy/SortBy";

export function Content() {
  return (
    <div className="flex flex-col gap-10 sm:flex-row">
      <div className="flex flex-col sm:w-2/3 w-full">
        <NewTask />
        <SortBy />
        <Tasks />
      </div>
      <div className="border border-gray-300 mt-10"></div>
      <Summary />
    </div>
  );
}
