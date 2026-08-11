import { useState, type SubmitEvent } from "react";
import { useAuth } from "../../context/AuthProvider";
import { ApiError } from "../../api/client";

export function Login() {
  const { signIn, register } = useAuth();
  const [username, setUsername] = useState("");
  const [password, setPassowrd] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isRegister, setIsRegister] = useState(false);
  const messageFor = (err: unknown): string => {
    if (err instanceof ApiError) {
      if (err.status === 409) return "That username is already taken";
      if (err.status === 400) return "Password must be at least 8 characters";
      if (err.status === 401) return "Wrong username or password";
      
    }
    return "Could not reach the server";
  };

  const handleSubmit = async (
    e: SubmitEvent<HTMLFormElement>,
  ): Promise<void> => {
    e.preventDefault();
    setError(null);
    const user = username.trim();

    try {
      if (isRegister) {
        if (password !== confirmPassword) {
          setError("Passwords do not match");
          return;
        }
        await register(user, password);
      }
      await signIn(user, password);
    } catch (err) {
      setError(messageFor(err));
    }
  };

  const handlePageChange = () => {
    setPassowrd("");
    setConfirmPassword("");
    setUsername("");
    setError(null);
    setIsRegister(!isRegister);
  };

  return (
    <div className="flex flex-col items-center justify-center w-full min-h-[86dvh] gap-3">
      <span>THE DAILY LEDGER</span>
      <h1 className="italic text-7xl">
        Todos<span className="text-accent">.</span>
      </h1>
      <span className="text-muted italic">Start your ledger.</span>
      <div className="border border-black w-96"></div>
      {isRegister ? (
        <>
          <form
            onSubmit={handleSubmit}
            className="flex flex-col gap-5 justify-start w-96 my-4 "
          >
            <div className="flex flex-col">
              <label htmlFor="username" className="w-full text-l text-muted">
                Username
              </label>
              <input
                id="username"
                type="text"
                value={username}
                className="text-xl focus:outline-0 border-b border-b-gray-400"
                onChange={(e) => setUsername(e.target.value)}
                placeholder="your username"
              />
            </div>
            <div className="flex flex-col">
              <label htmlFor="pasword" className="w-full text-l text-muted">
                Password
              </label>
              <input
                id="password"
                type="password"
                value={password}
                onChange={(e) => setPassowrd(e.target.value)}
                className="text-xl focus:outline-0 border-b border-b-gray-400"
                placeholder="At least 8 characters"
              />
            </div>
            <div className="flex flex-col">
              <label htmlFor="pasword" className="w-full text-l text-muted">
                Confirm password
              </label>
              <input
                id="password"
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                className="text-xl focus:outline-0 border-b border-b-gray-400"
                placeholder="Once more"
              />
            </div>
            <button
              type="submit"
              className="w-96 bg-accent text-white font-mono py-2.5 hover:bg-amber-800 hover:cursor-pointer"
            >
              Create account
            </button>
          </form>
          {error ? (
            <span className="text-red-600 font-sans">{error}</span>
          ) : (
            <></>
          )}
          <div className="border border-gray-300 w-96"></div>

          <span className="text-muted">
            Already keeping a ledger?{" "}
            <span
              className="text-accent hover:cursor-pointer"
              onClick={() => handlePageChange()}
            >
              Sign in
            </span>
          </span>
        </>
      ) : (
        <>
          <form
            onSubmit={handleSubmit}
            className="flex flex-col gap-5 justify-start w-96 my-4 "
          >
            <div className="flex flex-col">
              <label htmlFor="username" className="w-full text-l text-muted">
                Username
              </label>
              <input
                id="username"
                type="text"
                value={username}
                className="text-xl focus:outline-0 border-b border-b-gray-400"
                onChange={(e) => setUsername(e.target.value)}
                placeholder="your username"
              />
            </div>
            <div className="flex flex-col">
              <label htmlFor="pasword" className="w-full text-l text-muted">
                Password
              </label>
              <input
                id="password"
                type="password"
                value={password}
                onChange={(e) => setPassowrd(e.target.value)}
                className="text-xl focus:outline-0 border-b border-b-gray-400"
                placeholder="●●●●●●●"
              />
            </div>
            <button
              type="submit"
              className="w-96 bg-accent text-white font-mono py-2.5 hover:bg-amber-800 hover:cursor-pointer"
            >
              Sign in
            </button>
          </form>
          {error ? (
            <span className="text-red-600 font-sans">{error}</span>
          ) : (
            <></>
          )}
          <div className="border border-gray-300 w-96"></div>

          <span className="text-muted">
            No account yet?{" "}
            <span
              className="text-accent hover:cursor-pointer"
              onClick={() => handlePageChange()}
            >
              Start now
            </span>
          </span>
        </>
      )}
    </div>
  );
}
