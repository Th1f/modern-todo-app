import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { describe, expect, it } from "vitest";
import {
  TaskContext,
  type SortDirection,
  type SortKey,
} from "../../../context/TaskContext";
import {
  makeTaskContextValue,
  renderWithTasks,
} from "../../../test/renderWithTasks";
import { SortBy } from "./SortBy";

// The selected option's accessible name carries its direction, so match on the
// label prefix rather than the whole string.
const option = (label: string) =>
  screen.getByRole("radio", { name: new RegExp(`^${label}`) });

/**
 * Holds real state, so selection and direction actually move. renderWithTasks
 * alone gives a frozen value, which is right for asserting what the component
 * *asks* for but cannot show the state following along.
 */
function Stateful({ initial = "category" as SortKey }) {
  const [sortBy, setSortBy] = useState<SortKey>(initial);
  const [sortDirection, setSortDirection] = useState<SortDirection>("asc");
  return (
    <TaskContext.Provider
      value={makeTaskContextValue({
        sortBy,
        setSortBy,
        sortDirection,
        setSortDirection,
      })}
    >
      <SortBy />
    </TaskContext.Provider>
  );
}

describe("rendering", () => {
  it("offers the three sort options", () => {
    renderWithTasks(<SortBy />);

    expect(screen.getAllByRole("radio")).toHaveLength(3);
    expect(
      screen.getByRole("radiogroup", { name: "Sort tasks by" }),
    ).toBeInTheDocument();
  });

  it("marks the context's sort key as checked", () => {
    renderWithTasks(<SortBy />, { sortBy: "name" });

    expect(option("Name")).toBeChecked();
    expect(option("Category")).not.toBeChecked();
    expect(option("Done")).not.toBeChecked();
  });

  it("gives only the selected option a tab stop", () => {
    renderWithTasks(<SortBy />, { sortBy: "category" });

    expect(option("Category")).toHaveAttribute("tabindex", "0");
    expect(option("Name")).toHaveAttribute("tabindex", "-1");
    expect(option("Done")).toHaveAttribute("tabindex", "-1");
  });

  it("announces the direction of the selected option only", () => {
    renderWithTasks(<SortBy />, { sortBy: "category", sortDirection: "asc" });

    expect(option("Category")).toHaveAccessibleName(/ascending/);
    expect(option("Name")).toHaveAccessibleName("Name");
  });

  it("shows a down arrow when descending", () => {
    renderWithTasks(<SortBy />, { sortBy: "category", sortDirection: "desc" });

    expect(option("Category")).toHaveTextContent("↓");
    expect(option("Category")).toHaveAccessibleName(/descending/);
  });
});

describe("clicking", () => {
  it("asks the context for the clicked sort key", async () => {
    const user = userEvent.setup();
    const { value } = renderWithTasks(<SortBy />, { sortBy: "category" });

    await user.click(option("Name"));

    expect(value.setSortBy).toHaveBeenCalledWith("name");
    expect(value.setSortDirection).toHaveBeenCalledWith("asc");
  });

  it("flips to descending when the selected option is pressed again", async () => {
    const user = userEvent.setup();
    render(<Stateful />);

    expect(option("Category")).toHaveTextContent("↑");

    await user.click(option("Category"));

    expect(option("Category")).toHaveTextContent("↓");
    expect(option("Category")).toBeChecked();
  });

  it("flips back to ascending on a third press", async () => {
    const user = userEvent.setup();
    render(<Stateful />);

    await user.click(option("Category"));
    await user.click(option("Category"));

    expect(option("Category")).toHaveTextContent("↑");
  });

  it("starts a different option ascending rather than inheriting descending", async () => {
    const user = userEvent.setup();
    render(<Stateful />);

    await user.click(option("Category"));
    expect(option("Category")).toHaveTextContent("↓");

    await user.click(option("Name"));

    expect(option("Name")).toBeChecked();
    expect(option("Name")).toHaveTextContent("↑");
  });

  it("moves the tab stop to the new selection", async () => {
    const user = userEvent.setup();
    render(<Stateful />);

    await user.click(option("Done"));

    expect(option("Done")).toBeChecked();
    expect(option("Done")).toHaveAttribute("tabindex", "0");
    expect(option("Category")).toHaveAttribute("tabindex", "-1");
  });
});

describe("keyboard", () => {
  it("tab lands on the selected option, not the first one", async () => {
    const user = userEvent.setup();
    render(<Stateful />);

    await user.tab();

    expect(option("Category")).toHaveFocus();
  });

  it("ArrowRight moves selection and focus forward", async () => {
    const user = userEvent.setup();
    render(<Stateful />);

    await user.tab();
    await user.keyboard("{ArrowRight}");

    expect(option("Done")).toHaveFocus();
    expect(option("Done")).toBeChecked();
  });

  it("ArrowLeft moves backward", async () => {
    const user = userEvent.setup();
    render(<Stateful />);

    await user.tab();
    await user.keyboard("{ArrowLeft}");

    expect(option("Name")).toHaveFocus();
    expect(option("Name")).toBeChecked();
  });

  it("arrowing to a new option resets it to ascending", async () => {
    const user = userEvent.setup();
    render(<Stateful />);

    await user.click(option("Category"));
    expect(option("Category")).toHaveTextContent("↓");

    await user.keyboard("{ArrowRight}");

    expect(option("Done")).toBeChecked();
    expect(option("Done")).toHaveTextContent("↑");
  });

  it("wraps around at the end", async () => {
    const user = userEvent.setup();
    render(<Stateful />);

    await user.tab();
    await user.keyboard("{ArrowRight}{ArrowRight}");

    expect(option("Name")).toHaveFocus();
    expect(option("Name")).toBeChecked();
  });

  it("wraps around at the start", async () => {
    const user = userEvent.setup();
    render(<Stateful />);

    await user.tab();
    await user.keyboard("{ArrowLeft}{ArrowLeft}");

    expect(option("Done")).toHaveFocus();
  });

  it("ArrowDown and ArrowUp behave like Right and Left", async () => {
    const user = userEvent.setup();
    render(<Stateful />);

    await user.tab();
    await user.keyboard("{ArrowDown}");
    expect(option("Done")).toHaveFocus();

    await user.keyboard("{ArrowUp}");
    expect(option("Category")).toHaveFocus();
  });

  it("Home and End jump to the ends", async () => {
    const user = userEvent.setup();
    render(<Stateful />);

    await user.tab();
    await user.keyboard("{End}");
    expect(option("Done")).toBeChecked();

    await user.keyboard("{Home}");
    expect(option("Name")).toBeChecked();
  });

  it("leaves the group on Tab rather than cycling within it", async () => {
    const user = userEvent.setup();
    render(<Stateful />);

    await user.tab();
    await user.tab();

    expect(screen.getAllByRole("radio").some((el) => el === document.activeElement)).toBe(
      false,
    );
  });
});
