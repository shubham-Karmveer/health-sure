import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it } from "vitest";
import Home from "./page";

afterEach(cleanup);

describe("home page", () => {
  it("provides navigation to sign-in and the customer dashboard", () => {
    render(<Home />);

    expect(screen.getByRole("heading", { name: "HealthDekho" })).toBeVisible();
    expect(screen.getByRole("link", { name: "Sign in" })).toHaveAttribute("href", "/login");
    expect(screen.getByRole("link", { name: "Customer dashboard" })).toHaveAttribute(
      "href",
      "/dashboard"
    );
  });
});
