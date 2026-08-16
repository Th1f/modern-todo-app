import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import {
  makeTask,
  personal,
  renderWithTasks,
  work,
} from "../../../test/renderWithTasks";
import { Tasks } from "./Tasks";

afterEach(() => {
  vi.restoreAllMocks();
});

describe("states", () => {
  it("shows a skeleton while loading", () => {
    renderWithTasks(<Tasks />, { loading: true });

    expect(screen.getByRole("status")).toBeInTheDocument();
    expect(screen.getByText("Loading tasks…")).toBeInTheDocument();
    expect(screen.queryByRole("checkbox")).not.toBeInTheDocument();
  });

  it("prefers the skeleton over the empty state while loading", () => {
    renderWithTasks(<Tasks />, { loading: true, tasks: [] });

    expect(screen.queryByText("Nothing here yet...")).not.toBeInTheDocument();
  });

  it("shows the error instead of the list", () => {
    renderWithTasks(<Tasks />, {
      error: "Failed to fetch",
      tasks: [makeTask(1, "Buy milk")],
    });

    expect(screen.getByText(/Could not reach the API/)).toBeInTheDocument();
    expect(screen.queryByText("Buy milk")).not.toBeInTheDocument();
  });

  it("shows an empty state when there are no tasks", () => {
    renderWithTasks(<Tasks />);

    expect(screen.getByText("Nothing here yet...")).toBeInTheDocument();
  });

  it("shows the empty state when the filter matches nothing", () => {
    renderWithTasks(<Tasks />, {
      tasks: [makeTask(1, "Buy milk", work)],
      selectedCategory: "personal",
    });

    expect(screen.getByText("Nothing here yet...")).toBeInTheDocument();
  });
});

describe("listing", () => {
  it("sorts by name when sortBy is name", () => {
    renderWithTasks(<Tasks />, {
      sortBy: "name",
      tasks: [
        makeTask(1, "Ship release", work),
        makeTask(2, "Call mum", personal),
        makeTask(3, "Archive notes", work),
      ],
    });

    const names = screen.getAllByText(/Ship release|Call mum|Archive notes/);
    expect(names.map((node) => node.textContent)).toEqual([
      "Archive notes",
      "Call mum",
      "Ship release",
    ]);
  });

  it("sorts incomplete before complete when sortBy is done", () => {
    renderWithTasks(<Tasks />, {
      sortBy: "done",
      tasks: [
        makeTask(1, "Finished thing", work, true),
        makeTask(2, "Open thing", work, false),
      ],
    });

    const names = screen.getAllByText(/Finished thing|Open thing/);
    expect(names.map((node) => node.textContent)).toEqual([
      "Open thing",
      "Finished thing",
    ]);
  });

  it("reverses the order when the direction is descending", () => {
    renderWithTasks(<Tasks />, {
      sortBy: "name",
      sortDirection: "desc",
      tasks: [
        makeTask(1, "Apple", work),
        makeTask(2, "Zebra", work),
        makeTask(3, "Mango", work),
      ],
    });

    const names = screen.getAllByText(/Apple|Zebra|Mango/);
    expect(names.map((node) => node.textContent)).toEqual([
      "Zebra",
      "Mango",
      "Apple",
    ]);
  });

  it("puts completed first when done is sorted descending", () => {
    renderWithTasks(<Tasks />, {
      sortBy: "done",
      sortDirection: "desc",
      tasks: [
        makeTask(1, "Open thing", work, false),
        makeTask(2, "Finished thing", work, true),
      ],
    });

    const names = screen.getAllByText(/Open thing|Finished thing/);
    expect(names.map((node) => node.textContent)).toEqual([
      "Finished thing",
      "Open thing",
    ]);
  });

  it("mirrors the tie-break when descending", () => {
    renderWithTasks(<Tasks />, {
      sortBy: "category",
      sortDirection: "desc",
      tasks: [
        makeTask(1, "Apple", work),
        makeTask(2, "Zebra", work),
        makeTask(3, "Call mum", personal),
      ],
    });

    const names = screen.getAllByText(/Apple|Zebra|Call mum/);
    expect(names.map((node) => node.textContent)).toEqual([
      "Zebra",
      "Apple",
      "Call mum",
    ]);
  });

  it("breaks ties on name so the order is stable", () => {
    renderWithTasks(<Tasks />, {
      sortBy: "category",
      tasks: [
        makeTask(1, "Zebra", work),
        makeTask(2, "Apple", work),
      ],
    });

    const names = screen.getAllByText(/Zebra|Apple/);
    expect(names.map((node) => node.textContent)).toEqual(["Apple", "Zebra"]);
  });

  it("sorts the filtered list too, not just the full one", () => {
    renderWithTasks(<Tasks />, {
      selectedCategory: "work",
      sortBy: "name",
      tasks: [
        makeTask(1, "Zebra", work),
        makeTask(2, "Apple", work),
        makeTask(3, "Call mum", personal),
      ],
    });

    const names = screen.getAllByText(/Zebra|Apple|Call mum/);
    expect(names.map((node) => node.textContent)).toEqual(["Apple", "Zebra"]);
  });

  it("sorts by category name when showing all", () => {
    renderWithTasks(<Tasks />, {
      sortBy: "category",
      tasks: [
        makeTask(1, "Ship release", work),
        makeTask(2, "Call mum", personal),
      ],
      selectedCategory: "all",
    });

    const names = screen.getAllByText(/Ship release|Call mum/);
    expect(names.map((node) => node.textContent)).toEqual([
      "Call mum",
      "Ship release",
    ]);
  });

  it("shows only the selected category", () => {
    renderWithTasks(<Tasks />, {
      tasks: [
        makeTask(1, "Ship release", work),
        makeTask(2, "Call mum", personal),
      ],
      selectedCategory: "work",
    });

    expect(screen.getByText("Ship release")).toBeInTheDocument();
    expect(screen.queryByText("Call mum")).not.toBeInTheDocument();
  });

  it("renders the category name in caps and its colour", () => {
    renderWithTasks(<Tasks />, { tasks: [makeTask(1, "Ship release", work)] });

    expect(screen.getByText("WORK")).toHaveStyle({ color: "#3B82F6" });
  });

  it("checks the box for a completed task", () => {
    renderWithTasks(<Tasks />, {
      tasks: [makeTask(1, "Done thing", work, true)],
    });

    expect(screen.getByRole("checkbox")).toBeChecked();
  });
});

describe("toggling", () => {
  it("calls toggleTask with the task id", async () => {
    const user = userEvent.setup();
    const { value } = renderWithTasks(<Tasks />, {
      tasks: [makeTask(7, "Buy milk")],
    });

    await user.click(screen.getByRole("checkbox"));

    expect(value.toggleTask).toHaveBeenCalledWith(7);
  });
});

describe("renaming", () => {
  it("saves the new name on Enter", async () => {
    const user = userEvent.setup();
    const { value } = renderWithTasks(<Tasks />, {
      tasks: [makeTask(7, "Buy milk")],
    });

    await user.click(screen.getByRole("button", { name: "Rename Buy milk" }));
    await user.clear(screen.getByRole("textbox"));
    await user.type(screen.getByRole("textbox"), "Buy oat milk{Enter}");

    expect(value.renameTask).toHaveBeenCalledWith(7, "Buy oat milk");
  });

  it("discards the edit on Escape", async () => {
    const user = userEvent.setup();
    const { value } = renderWithTasks(<Tasks />, {
      tasks: [makeTask(7, "Buy milk")],
    });

    await user.click(screen.getByRole("button", { name: "Rename Buy milk" }));
    await user.clear(screen.getByRole("textbox"));
    await user.type(screen.getByRole("textbox"), "Something else{Escape}");

    expect(value.renameTask).not.toHaveBeenCalled();
    expect(screen.getByText("Buy milk")).toBeInTheDocument();
  });

  it("ignores a rename that changes nothing", async () => {
    const user = userEvent.setup();
    const { value } = renderWithTasks(<Tasks />, {
      tasks: [makeTask(7, "Buy milk")],
    });

    await user.click(screen.getByRole("button", { name: "Rename Buy milk" }));
    await user.type(screen.getByRole("textbox"), "{Enter}");

    expect(value.renameTask).not.toHaveBeenCalled();
  });

  it("ignores a rename to blank", async () => {
    const user = userEvent.setup();
    const { value } = renderWithTasks(<Tasks />, {
      tasks: [makeTask(7, "Buy milk")],
    });

    await user.click(screen.getByRole("button", { name: "Rename Buy milk" }));
    await user.clear(screen.getByRole("textbox"));
    await user.type(screen.getByRole("textbox"), "   {Enter}");

    expect(value.renameTask).not.toHaveBeenCalled();
  });

  it("starts editing on double click", async () => {
    const user = userEvent.setup();
    renderWithTasks(<Tasks />, { tasks: [makeTask(7, "Buy milk")] });

    await user.dblClick(screen.getByText("Buy milk"));

    expect(screen.getByRole("textbox")).toHaveValue("Buy milk");
  });
});

describe("deleting", () => {
  it("removes the task once confirmed", async () => {
    const user = userEvent.setup();
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const { value } = renderWithTasks(<Tasks />, {
      tasks: [makeTask(7, "Buy milk")],
    });

    await user.click(screen.getByRole("button", { name: "Delete Buy milk" }));

    expect(window.confirm).toHaveBeenCalledWith('Delete "Buy milk"?');
    expect(value.removeTask).toHaveBeenCalledWith(7);
  });

  it("keeps the task when the confirm is dismissed", async () => {
    const user = userEvent.setup();
    vi.spyOn(window, "confirm").mockReturnValue(false);
    const { value } = renderWithTasks(<Tasks />, {
      tasks: [makeTask(7, "Buy milk")],
    });

    await user.click(screen.getByRole("button", { name: "Delete Buy milk" }));

    expect(value.removeTask).not.toHaveBeenCalled();
  });
});
