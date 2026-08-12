import { Content } from "./_components/Content/Content";
import { Header } from "./_components/Header/Header";
import { Login } from "./_components/Login/Login";
import "./App.css";
import { PageSkeleton } from "./_components/ui/PageSkeleton";
import { useAuth } from "./context/AuthProvider";
import { TaskProvider } from "./context/TaskProvider";

function App() {
  const { user, loading } = useAuth();
  if (loading) {
    return <PageSkeleton />;
  }
  if (!user) {
    return <Login />;
  }
  return (
    <TaskProvider>
      <Header />
      <Content />
    </TaskProvider>
  );
}

export default App;
