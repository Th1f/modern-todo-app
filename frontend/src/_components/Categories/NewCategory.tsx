import { useState, type ChangeEvent } from "react";

export function NewCategory() {
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

  const handleSubmit = (e: React.SyntheticEvent<HTMLFormElement>): void => {
    e.preventDefault();
    console.log({ categoryName, selectedColor, hexColor });
    setCategoryName("");
    setSelectedColor(colors[0]);
    setHexColor("");
  };

  const handleCategoryChange = (e: ChangeEvent<HTMLInputElement>): void => {
    setCategoryName(e.target.value);
  };

  const handleHexChange = (e: ChangeEvent<HTMLInputElement>): void => {
    setHexColor(e.target.value);
  };

  return (
    <form onSubmit={handleSubmit} className="flex gap-1">
      <input
        type="text"
        value={categoryName}
        onChange={handleCategoryChange}
        className="m-1 focus:outline-0 w-24 border-b border-gray-400"
        placeholder="New category"
        required
      />
      <div className="flex gap-1 self-center">
        {colors.map((color) => (
          <button
            key={color}
            type="button"
            className={`rounded-full w-5 h-5  ${
              selectedColor === color ? "ring-2 ring-offset-1" : ""
            }`}
            style={{ backgroundColor: color }}
            onClick={() => {
              setSelectedColor(color);
              setHexColor(color);
            }}
            aria-label={`Select ${color}`}
          />
        ))}
      </div>
      <input
        type="text"
        value={hexColor}
        onChange={handleHexChange}
        className="m-1 focus:outline-0 w-16 border-b border-gray-400"
        placeholder="#hex"
      />
      <button
        type="submit"
        className="w-6 h-6 self-center text-center border rounded-full hover:bg-black hover:text-white hover:cursor-pointer"
      >
        +
      </button>
    </form>
  );
}
