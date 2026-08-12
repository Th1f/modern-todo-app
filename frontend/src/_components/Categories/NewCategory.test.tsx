import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { ApiError } from "../../api/client";
import { renderWithTasks } from "../../test/renderWithTasks";
import { NewCategory } from "./NewCategory";

const nameBox = () => screen.getByPlaceholderText("New category");
const hexBox = () => screen.getByPlaceholderText("#hex");
const addButton = () => screen.getByRole("button", { name: "+" });

describe("NewCategory", () => {
  it("submits the name with the default swatch", async () => {
    const user = userEvent.setup();
    const { value } = renderWithTasks(<NewCategory />);

    await user.type(nameBox(), "Fitness");
    await user.click(addButton());

    expect(value.addCategory).toHaveBeenCalledWith({
      name: "Fitness",
      color: "#C14B2A",
    });
  });

  it("uses a chosen swatch", async () => {
    const user = userEvent.setup();
    const { value } = renderWithTasks(<NewCategory />);

    await user.type(nameBox(), "Fitness");
    await user.click(screen.getByRole("button", { name: "Select #3D6B54" }));
    await user.click(addButton());

    expect(value.addCategory).toHaveBeenCalledWith({
      name: "Fitness",
      color: "#3D6B54",
    });
  });

  it("prefers a valid typed hex over the swatch", async () => {
    const user = userEvent.setup();
    const { value } = renderWithTasks(<NewCategory />);

    await user.type(nameBox(), "Fitness");
    await user.type(hexBox(), "#abc");
    await user.click(addButton());

    expect(value.addCategory).toHaveBeenCalledWith({
      name: "Fitness",
      color: "#abc",
    });
  });

  it("trims the name", async () => {
    const user = userEvent.setup();
    const { value } = renderWithTasks(<NewCategory />);

    await user.type(nameBox(), "  Fitness  ");
    await user.click(addButton());

    expect(value.addCategory).toHaveBeenCalledWith(
      expect.objectContaining({ name: "Fitness" }),
    );
  });

  it("refuses a malformed hex", async () => {
    const user = userEvent.setup();
    const { value } = renderWithTasks(<NewCategory />);

    await user.type(nameBox(), "Fitness");
    await user.type(hexBox(), "not-a-colour");
    await user.click(addButton());

    expect(value.addCategory).not.toHaveBeenCalled();
    expect(screen.getByText("Use a hex colour like #3B82F6")).toBeInTheDocument();
    expect(hexBox()).toHaveAttribute("aria-invalid", "true");
  });

  it("does not submit a whitespace-only name", async () => {
    const user = userEvent.setup();
    const { value } = renderWithTasks(<NewCategory />);

    await user.type(nameBox(), "   ");
    await user.click(addButton());

    expect(value.addCategory).not.toHaveBeenCalled();
  });

  it("resets the form after a successful add", async () => {
    const user = userEvent.setup();
    renderWithTasks(<NewCategory />);

    await user.type(nameBox(), "Fitness");
    await user.type(hexBox(), "#abcdef");
    await user.click(addButton());

    await waitFor(() => expect(nameBox()).toHaveValue(""));
    expect(hexBox()).toHaveValue("");
  });

  it("reports a duplicate name and keeps what was typed", async () => {
    const user = userEvent.setup();
    const addCategory = vi.fn().mockRejectedValue(new ApiError("nope", 409));
    renderWithTasks(<NewCategory />, { addCategory });

    await user.type(nameBox(), "Work");
    await user.click(addButton());

    expect(await screen.findByText('"Work" already exists')).toBeInTheDocument();
    expect(nameBox()).toHaveValue("Work");
  });

  it("falls back to a generic message on other failures", async () => {
    const user = userEvent.setup();
    const addCategory = vi.fn().mockRejectedValue(new Error("network down"));
    renderWithTasks(<NewCategory />, { addCategory });

    await user.type(nameBox(), "Fitness");
    await user.click(addButton());

    expect(
      await screen.findByText("Could not save that category"),
    ).toBeInTheDocument();
  });
});
