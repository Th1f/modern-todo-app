import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "../../api/client";
import {
  makeTask,
  personal,
  renderWithTasks,
  work,
} from "../../test/renderWithTasks";
import Categories from "./Categories";

afterEach(() => {
  vi.restoreAllMocks();
});

describe("tabs", () => {
  it("renders All plus every category", () => {
    renderWithTasks(<Categories />, { categories: [work, personal] });

    expect(screen.getByText("All")).toBeInTheDocument();
    expect(screen.getByText("Work")).toBeInTheDocument();
    expect(screen.getByText("Personal")).toBeInTheDocument();
  });

  it("selects a category in lower case", async () => {
    const user = userEvent.setup();
    const { value } = renderWithTasks(<Categories />, {
      categories: [work, personal],
    });

    await user.click(screen.getByText("Personal"));

    expect(value.setSelectedCategory).toHaveBeenCalledWith("personal");
  });

  it("selects all", async () => {
    const user = userEvent.setup();
    const { value } = renderWithTasks(<Categories />, {
      categories: [work],
      selectedCategory: "work",
    });

    await user.click(screen.getByText("All"));

    expect(value.setSelectedCategory).toHaveBeenCalledWith("all");
  });

  it("only offers rename and delete on the selected tab", () => {
    renderWithTasks(<Categories />, {
      categories: [work, personal],
      selectedCategory: "work",
    });

    expect(screen.getByRole("button", { name: "Rename Work" })).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Rename Personal" }),
    ).not.toBeInTheDocument();
  });
});

describe("renaming", () => {
  async function startRenamingWork(
    user: ReturnType<typeof userEvent.setup>,
    replacement: string,
  ) {
    await user.click(screen.getByRole("button", { name: "Rename Work" }));
    const input = screen.getByDisplayValue("Work");
    await user.clear(input);
    await user.type(input, replacement);
  }

  it("saves on Enter", async () => {
    const user = userEvent.setup();
    const { value } = renderWithTasks(<Categories />, {
      categories: [work],
      selectedCategory: "work",
    });

    await startRenamingWork(user, "Job{Enter}");

    expect(value.renameCategory).toHaveBeenCalledWith(work.id, "Job");
  });

  it("follows the rename with the filter", async () => {
    const user = userEvent.setup();
    const { value } = renderWithTasks(<Categories />, {
      categories: [work],
      selectedCategory: "work",
    });

    await startRenamingWork(user, "Job{Enter}");

    expect(value.setSelectedCategory).toHaveBeenCalledWith("job");
  });

  it("shows a friendly message when the name is taken", async () => {
    const user = userEvent.setup();
    const renameCategory = vi.fn().mockRejectedValue(new ApiError("nope", 409));
    renderWithTasks(<Categories />, {
      categories: [work],
      selectedCategory: "work",
      renameCategory,
    });

    await startRenamingWork(user, "Personal{Enter}");

    expect(await screen.findByText('"Personal" already exists')).toBeInTheDocument();
  });

  it("discards on Escape", async () => {
    const user = userEvent.setup();
    const { value } = renderWithTasks(<Categories />, {
      categories: [work],
      selectedCategory: "work",
    });

    await startRenamingWork(user, "Job{Escape}");

    expect(value.renameCategory).not.toHaveBeenCalled();
  });
});

describe("deleting", () => {
  it("refuses while the category still holds tasks", async () => {
    const user = userEvent.setup();
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(true);
    const { value } = renderWithTasks(<Categories />, {
      categories: [work],
      selectedCategory: "work",
      tasks: [makeTask(1, "Ship release", work)],
    });

    await user.click(screen.getByRole("button", { name: "Delete Work" }));

    expect(screen.getByText('"Work" still has 1 task')).toBeInTheDocument();
    expect(confirmSpy).not.toHaveBeenCalled();
    expect(value.removeCategory).not.toHaveBeenCalled();
  });

  it("pluralises the refusal message", async () => {
    const user = userEvent.setup();
    renderWithTasks(<Categories />, {
      categories: [work],
      selectedCategory: "work",
      tasks: [makeTask(1, "a", work), makeTask(2, "b", work)],
    });

    await user.click(screen.getByRole("button", { name: "Delete Work" }));

    expect(screen.getByText('"Work" still has 2 tasks')).toBeInTheDocument();
  });

  it("deletes an empty category once confirmed", async () => {
    const user = userEvent.setup();
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const { value } = renderWithTasks(<Categories />, {
      categories: [work],
      selectedCategory: "work",
    });

    await user.click(screen.getByRole("button", { name: "Delete Work" }));

    expect(window.confirm).toHaveBeenCalledWith('Delete the "Work" category?');
    expect(value.removeCategory).toHaveBeenCalledWith(work.id);
  });

  it("does nothing when the confirm is dismissed", async () => {
    const user = userEvent.setup();
    vi.spyOn(window, "confirm").mockReturnValue(false);
    const { value } = renderWithTasks(<Categories />, {
      categories: [work],
      selectedCategory: "work",
    });

    await user.click(screen.getByRole("button", { name: "Delete Work" }));

    expect(value.removeCategory).not.toHaveBeenCalled();
  });

  it("reports a server-side conflict", async () => {
    const user = userEvent.setup();
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const removeCategory = vi.fn().mockRejectedValue(new ApiError("nope", 409));
    renderWithTasks(<Categories />, {
      categories: [work],
      selectedCategory: "work",
      removeCategory,
    });

    await user.click(screen.getByRole("button", { name: "Delete Work" }));

    expect(await screen.findByText('"Work" is still in use')).toBeInTheDocument();
  });
});
