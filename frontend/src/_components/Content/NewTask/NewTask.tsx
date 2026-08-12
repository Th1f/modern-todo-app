import { useState, type ChangeEvent, type SubmitEvent } from "react";
import { useTasks } from "../../../context/TaskContext";

export function NewTask() {
  const { categories, addTask } = useTasks();
  const [chosenId, setChosenId] = useState<number | "">("");
  const [taskName, setTaskName] = useState<string>("");
  const [saving, setSaving] = useState(false);

  // Derived rather than synced in an effect: until the user picks something,
  // the first category is the selection.
  const fallbackId: number | "" = categories.length > 0 ? categories[0].id : "";
  const categoryId: number | "" = chosenId === "" ? fallbackId : chosenId;

  const handleSubmit = async (e: SubmitEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!taskName.trim() || categoryId === "") return;

    setSaving(true);
    try {
      await addTask({
        name: taskName.trim(),
        categoryId,
        isDone: false,
      });
      setTaskName("");
    } finally {
      setSaving(false);
    }
  };

  const handleTaskNameChange = (e: ChangeEvent<HTMLInputElement>) => {
    setTaskName(e.target.value);
  };

  return (
    <form onSubmit={handleSubmit} className="flex gap-3 my-10 w-full">
      <input
        type="text"
        className=" focus:outline-0 w-full border-b-2 border-black  text-xl "
        value={taskName}
        onChange={handleTaskNameChange}
        placeholder="Write a new task..."
        required
      />
      <select
        value={categoryId}
        onChange={(e) => setChosenId(Number(e.target.value))}
        className="border-b border-gray-400 text-muted"
      >
        {categories.map((category) => (
          <option key={category.id} value={category.id}>
            {category.name}
          </option>
        ))}
      </select>
      <button
        type="submit"
        disabled={saving || categoryId === ""}
        className="bg-accent px-5 py-2 text-white text-sm disabled:opacity-50"
      >
        {saving ? "..." : "ADD"}
      </button>
    </form>
  );
}
