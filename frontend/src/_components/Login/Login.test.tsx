import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "../../api/client";
import { Login } from "./Login";

const signIn = vi.fn().mockResolvedValue(undefined);
const register = vi.fn().mockResolvedValue(undefined);
const signOut = vi.fn().mockResolvedValue(undefined);

vi.mock("../../context/AuthProvider", () => ({
  useAuth: () => ({ user: null, loading: false, signIn, register, signOut }),
}));

beforeEach(() => {
  vi.clearAllMocks();
  signIn.mockResolvedValue(undefined);
  register.mockResolvedValue(undefined);
});

const username = () => screen.getByLabelText("Username");
const password = () => screen.getByLabelText("Password");
const confirmPassword = () => screen.getByLabelText("Confirm password");

async function switchToRegister(user: ReturnType<typeof userEvent.setup>) {
  await user.click(screen.getByText("Start now"));
}

describe("sign in", () => {
  it("signs in with the trimmed username", async () => {
    const user = userEvent.setup();
    render(<Login />);

    await user.type(username(), "  alice  ");
    await user.type(password(), "password123");
    await user.click(screen.getByRole("button", { name: "Sign in" }));

    expect(signIn).toHaveBeenCalledWith("alice", "password123");
    expect(register).not.toHaveBeenCalled();
  });

  it("shows a message for wrong credentials", async () => {
    const user = userEvent.setup();
    signIn.mockRejectedValue(new ApiError("nope", 401));
    render(<Login />);

    await user.type(username(), "alice");
    await user.type(password(), "wrong");
    await user.click(screen.getByRole("button", { name: "Sign in" }));

    expect(
      await screen.findByText("Wrong username or password"),
    ).toBeInTheDocument();
  });

  it("shows a message when the server is unreachable", async () => {
    const user = userEvent.setup();
    signIn.mockRejectedValue(new TypeError("Failed to fetch"));
    render(<Login />);

    await user.type(username(), "alice");
    await user.type(password(), "password123");
    await user.click(screen.getByRole("button", { name: "Sign in" }));

    expect(
      await screen.findByText("Could not reach the server"),
    ).toBeInTheDocument();
  });
});

describe("register", () => {
  it("registers then signs in", async () => {
    const user = userEvent.setup();
    render(<Login />);
    await switchToRegister(user);

    await user.type(username(), "alice");
    await user.type(password(), "password123");
    await user.type(confirmPassword(), "password123");
    await user.click(screen.getByRole("button", { name: "Create account" }));

    expect(register).toHaveBeenCalledWith("alice", "password123");
    expect(signIn).toHaveBeenCalledWith("alice", "password123");
  });

  it("refuses mismatched passwords without calling the API", async () => {
    const user = userEvent.setup();
    render(<Login />);
    await switchToRegister(user);

    await user.type(username(), "alice");
    await user.type(password(), "password123");
    await user.type(confirmPassword(), "password124");
    await user.click(screen.getByRole("button", { name: "Create account" }));

    expect(await screen.findByText("Passwords do not match")).toBeInTheDocument();
    expect(register).not.toHaveBeenCalled();
    expect(signIn).not.toHaveBeenCalled();
  });

  it("reports a taken username", async () => {
    const user = userEvent.setup();
    register.mockRejectedValue(new ApiError("nope", 409));
    render(<Login />);
    await switchToRegister(user);

    await user.type(username(), "alice");
    await user.type(password(), "password123");
    await user.type(confirmPassword(), "password123");
    await user.click(screen.getByRole("button", { name: "Create account" }));

    expect(
      await screen.findByText("That username is already taken"),
    ).toBeInTheDocument();
    expect(signIn).not.toHaveBeenCalled();
  });

  it("reports a too-short password", async () => {
    const user = userEvent.setup();
    register.mockRejectedValue(new ApiError("nope", 400));
    render(<Login />);
    await switchToRegister(user);

    await user.type(username(), "alice");
    await user.type(password(), "short");
    await user.type(confirmPassword(), "short");
    await user.click(screen.getByRole("button", { name: "Create account" }));

    expect(
      await screen.findByText("Password must be at least 8 characters"),
    ).toBeInTheDocument();
  });
});

describe("switching modes", () => {
  it("swaps the form", async () => {
    const user = userEvent.setup();
    render(<Login />);

    expect(screen.queryByLabelText("Confirm password")).not.toBeInTheDocument();

    await switchToRegister(user);

    expect(screen.getByLabelText("Confirm password")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Create account" })).toBeInTheDocument();
  });

  it("clears the fields and any error", async () => {
    const user = userEvent.setup();
    signIn.mockRejectedValue(new ApiError("nope", 401));
    render(<Login />);

    await user.type(username(), "alice");
    await user.type(password(), "wrong");
    await user.click(screen.getByRole("button", { name: "Sign in" }));
    expect(
      await screen.findByText("Wrong username or password"),
    ).toBeInTheDocument();

    await switchToRegister(user);

    expect(username()).toHaveValue("");
    expect(password()).toHaveValue("");
    expect(
      screen.queryByText("Wrong username or password"),
    ).not.toBeInTheDocument();
  });
});
