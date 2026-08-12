import { useState, type ChangeEvent, type SubmitEvent } from "react";
import { ApiError } from "../../api/client";
import { useTasks } from "../../context/TaskContext";

//Matches Hex Colors
const HEX_PATTERN = /^#(?:[0-9a-f]{3}){1,2}$/i;

export function NewCategory() {
  const { addCategory } = useTasks();

  const colors = [
    "#C14B2A",
    "#3D6B54",
    "#8C6D2F",
    "#5A4E8C",
    "#2F6577",
    "#96432E",
  ];

  const [categoryName, setCategoryName] = useState<string>("");
  const [selectedColor, setSelectedColor] = useState<string>(colors[0]);
  const [hexColor, setHexColor] = useState<string>("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const typedHex = hexColor.trim();
  const hexIsUsable = typedHex === "" || HEX_PATTERN.test(typedHex);
  const color = HEX_PATTERN.test(typedHex) ? typedHex : selectedColor;

  const handleSubmit = async (
    e: SubmitEvent<HTMLFormElement>,
  ): Promise<void> => {
    e.preventDefault();
    const name = categoryName.trim();
    if (!name) return;

    if (!hexIsUsable) {
      setError("Use a hex colour like #3B82F6");
      return;
    }

    setSaving(true);
    setError(null);
    try {
      await addCategory({ name, color });
      setCategoryName("");
      setSelectedColor(colors[0]);
      setHexColor("");
    } catch (err) {
      setError(
        err instanceof ApiError && err.status === 409
          ? `"${name}" already exists`
          : "Could not save that category",
      );
    } finally {
      setSaving(false);
    }
  };

  const handleCategoryChange = (e: ChangeEvent<HTMLInputElement>): void => {
    setCategoryName(e.target.value);
  };

  const handleHexChange = (e: ChangeEvent<HTMLInputElement>): void => {
    setHexColor(e.target.value);
    setError(null);
  };

  return (
    <form onSubmit={handleSubmit} className="flex gap-1 relative">
      <input
        type="text"
        value={categoryName}
        onChange={handleCategoryChange}
        className="m-1 focus:outline-0 w-24 border-b border-gray-400"
        placeholder="New category"
        required
      />
      <div className="flex gap-1 self-center">
        {colors.map((swatch) => (
          <button
            key={swatch}
            type="button"
            className={`rounded-full w-5 h-5  ${
              color === swatch ? "ring-2 ring-offset-1" : ""
            }`}
            style={{ backgroundColor: swatch }}
            onClick={() => {
              setSelectedColor(swatch);
              setHexColor("");
              setError(null);
            }}
            aria-label={`Select ${swatch}`}
          />
        ))}
      </div>
      <input
        type="text"
        value={hexColor}
        onChange={handleHexChange}
        className={`m-1 focus:outline-0 w-16 border-b ${
          hexIsUsable ? "border-gray-400" : "border-red-500 text-red-600"
        }`}
        placeholder="#hex"
        aria-invalid={!hexIsUsable}
      />
      <button
        type="submit"
        disabled={saving}
        className="w-6 h-6 self-center text-center border rounded-full hover:bg-black hover:text-white hover:cursor-pointer disabled:opacity-40"
      >
        +
      </button>
      {error && (
        <span className="absolute top-full right-0 text-xs text-red-600 whitespace-nowrap">
          {error}
        </span>
      )}
    </form>
  );
}
