import {
  createContext,
  useState,
  type Dispatch,
  type SetStateAction,
  type ReactNode,
} from "react";

type TaskContextType = {
  selectedCategory: string;
  setSelectedCategory: Dispatch<SetStateAction<string>>;
};

type TaskProviderProps = {
  children: ReactNode;
};

export const TaskContext = createContext<TaskContextType | undefined>(
  undefined,
);

export function TaskProvider({ children }: TaskProviderProps) {
  const [selectedCategory, setSelectedCategory] = useState("");

  return (
    <TaskContext.Provider value={{ selectedCategory, setSelectedCategory }}>
      {children}
    </TaskContext.Provider>
  );
}
