import { useEffect, useState, type ChangeEvent, type SubmitEvent } from "react";
import { useTasks } from "../../../context/TaskProvider";

export function NewTask() {
  const { categories, addTask } = useTasks();
  const [categoryId, setCategoryId] = useState<number | "">("");
  const [taskName, setTaskName] = useState<string>("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (categoryId === "" && categories.length > 0) {
      setCategoryId(categories[0].id);
    }
  }, [categories, categoryId]);

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
        className=" focus:outline-0 w-3/4 border-b-2 border-black  text-xl "
        value={taskName}
        onChange={handleTaskNameChange}
        placeholder="Write a new task..."
        required
      />
      <select
        value={categoryId}
        onChange={(e) => setCategoryId(Number(e.target.value))}
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
