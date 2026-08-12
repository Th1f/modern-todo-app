import { screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import {
  errands,
  makeTask,
  personal,
  renderWithTasks,
  work,
} from "../../../test/renderWithTasks";
import { Summary } from "./Summary";

describe("Summary", () => {
  it("shows a skeleton while loading", () => {
    renderWithTasks(<Summary />, { loading: true, categories: [work] });

    expect(screen.getByRole("status")).toBeInTheDocument();
    expect(screen.getByText("Loading summary…")).toBeInTheDocument();
    expect(screen.queryByText("Work")).not.toBeInTheDocument();
  });

  it("lists a row per category", () => {
    renderWithTasks(<Summary />, { categories: [work, personal, errands] });

    expect(screen.getByText("Work")).toBeInTheDocument();
    expect(screen.getByText("Personal")).toBeInTheDocument();
    expect(screen.getByText("Errands")).toBeInTheDocument();
  });

  it("counts done over total per category", () => {
    renderWithTasks(<Summary />, {
      categories: [work, personal],
      tasks: [
        makeTask(1, "Ship release", work, true),
        makeTask(2, "Write docs", work, false),
        makeTask(3, "Call mum", personal, false),
      ],
    });

    expect(screen.getByText("1/2")).toBeInTheDocument();
    expect(screen.getByText("0/1")).toBeInTheDocument();
  });

  it("does not count another category's tasks", () => {
    renderWithTasks(<Summary />, {
      categories: [errands],
      tasks: [makeTask(1, "Ship release", work, true)],
    });

    expect(screen.getByText("0/0")).toBeInTheDocument();
  });

  it("exposes progress to assistive tech", () => {
    renderWithTasks(<Summary />, {
      categories: [work],
      tasks: [
        makeTask(1, "Ship release", work, true),
        makeTask(2, "Write docs", work, false),
      ],
    });

    const bar = screen.getByRole("progressbar", { name: "Work tasks completed" });
    expect(bar).toHaveAttribute("aria-valuenow", "1");
    expect(bar).toHaveAttribute("aria-valuemin", "0");
    expect(bar).toHaveAttribute("aria-valuemax", "2");
  });

  it("fills the bar proportionally", () => {
    renderWithTasks(<Summary />, {
      categories: [work],
      tasks: [
        makeTask(1, "a", work, true),
        makeTask(2, "b", work, true),
        makeTask(3, "c", work, false),
        makeTask(4, "d", work, false),
      ],
    });

    const fill = screen.getByRole("progressbar").firstElementChild;
    expect(fill).toHaveStyle({ width: "50%", backgroundColor: "#3B82F6" });
  });

  it("shows an empty bar rather than NaN for a category with no tasks", () => {
    renderWithTasks(<Summary />, { categories: [work] });

    const fill = screen.getByRole("progressbar").firstElementChild;
    expect(fill).toHaveStyle({ width: "0%" });
  });
});
