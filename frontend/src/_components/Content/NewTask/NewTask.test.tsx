import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { personal, renderWithTasks, work } from "../../../test/renderWithTasks";
import { NewTask } from "./NewTask";

describe("NewTask", () => {
  it("lists every category as an option", () => {
    renderWithTasks(<NewTask />, { categories: [work, personal] });

    expect(screen.getByRole("option", { name: "Work" })).toBeInTheDocument();
    expect(screen.getByRole("option", { name: "Personal" })).toBeInTheDocument();
  });

  it("preselects the first category", async () => {
    renderWithTasks(<NewTask />, { categories: [work, personal] });

    await waitFor(() =>
      expect(screen.getByRole("combobox")).toHaveValue(String(work.id)),
    );
  });

  it("disables the button until a category exists", () => {
    renderWithTasks(<NewTask />, { categories: [] });

    expect(screen.getByRole("button", { name: "ADD" })).toBeDisabled();
  });

  it("submits the trimmed name with the chosen category", async () => {
    const user = userEvent.setup();
    const { value } = renderWithTasks(<NewTask />, {
      categories: [work, personal],
    });

    await user.type(screen.getByRole("textbox"), "  Buy milk  ");
    await user.selectOptions(screen.getByRole("combobox"), String(personal.id));
    await user.click(screen.getByRole("button", { name: "ADD" }));

    expect(value.addTask).toHaveBeenCalledWith({
      name: "Buy milk",
      categoryId: personal.id,
      isDone: false,
    });
  });

  it("clears the input after a successful add", async () => {
    const user = userEvent.setup();
    renderWithTasks(<NewTask />, { categories: [work] });

    await user.type(screen.getByRole("textbox"), "Buy milk");
    await user.click(screen.getByRole("button", { name: "ADD" }));

    await waitFor(() => expect(screen.getByRole("textbox")).toHaveValue(""));
  });

  it("does not submit a whitespace-only name", async () => {
    const user = userEvent.setup();
    const { value } = renderWithTasks(<NewTask />, { categories: [work] });

    await user.type(screen.getByRole("textbox"), "   ");
    await user.click(screen.getByRole("button", { name: "ADD" }));

    expect(value.addTask).not.toHaveBeenCalled();
  });

});
