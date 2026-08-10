import { useState, type ChangeEvent } from "react";

export function NewTask() {
  const options = ["Work", "Personal", "Errands"];
  const [selected, setSelected] = useState<string>("");
  const [taskName, setTaskName] = useState<string>("");

  const handleSubmit = (e: React.SyntheticEvent<HTMLFormElement>) => {
    e.preventDefault();
    console.log(selected,taskName)
  };

  const handleTaskNameChange = (e: ChangeEvent<HTMLInputElement>) => {
    setTaskName(e.target.value);
  };
  return (
    <form onSubmit={handleSubmit} className="flex gap-3 my-10 w-full">
      <input
        type="text"
        className=" focus:outline-0 w-3/4 border-b-2 border-black  text-xl "
        onChange={handleTaskNameChange}
        placeholder="Write a new task..."
        required
      />
      <select
        value={selected}
        onChange={(e) => setSelected(e.target.value)}
        className="border-b border-gray-400 text-muted"
      >
        {options.map((opt) => (
          <option key={opt.toLowerCase()} value={opt.toLowerCase()}>
            {opt}
          </option>
        ))}
      </select>
      <button type="submit" className="bg-accent px-5 py-2 text-white text-sm">
        ADD
      </button>
    </form>
  );
}
