import { useState } from "react";
import { NewCategory } from "./NewCategory";

export default function Categories() {
  const [selected, setSelected] = useState("all");
  const categories = [
    {
      name: "work",
      color: "#ffff",
    },
    {
      name: "personal",
      color: "#ffff",
    },
    {
      name: "errands",
      color: "#ffff",
    },
  ];

  return (
    <div className="flex border border-gray-400 border-t-0 border-x-0 justify-between overflow-y-auto">
      <div className="flex gap-2">
        <span
          className={
            "flex py-2 px-3  hover:text-black hover:cursor-pointer hover:border-b hover:border-b-accent border-0 " +
            `${selected == "all" ? "text-black border-b border-b-accent border-0 font-bold" : "text-muted"}`
          }
          onClick={() => setSelected("all")}
        >
          All
        </span>
        {categories.map(({ name, color }) => (
          <span
            className={
              "flex py-2 px-3  hover:text-black hover:cursor-pointer hover:border-b hover:border-b-accent border-0 " +
              `${selected == name ? "text-black border-b border-b-accent border-0 font-bold " : "text-muted"}`
            }
            onClick={() => setSelected(`${name}`)}
            key={name}
          >
            {name.charAt(0).toUpperCase() + name.substring(1)}
          </span>
        ))}
      </div>
      <NewCategory/>
    </div>
  );
}
