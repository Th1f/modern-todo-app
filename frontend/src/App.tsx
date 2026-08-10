import { useState } from "react";
import { Content } from "./_components/Content/Content";
import { Header } from "./_components/Header/Header";
import "./App.css";
import { TaskProvider } from "./context/TaskProvider";

function App() {
  return (
    <>
      <TaskProvider>
        <Header />
        <Content />
      </TaskProvider>
    </>
  );
}

export default App;
