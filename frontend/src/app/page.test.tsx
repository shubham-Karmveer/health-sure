import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it } from "vitest";
import Home from "./page";

afterEach(cleanup);

describe("home page", () => {
  it("introduces HealthDekho and links visitors to sign-in and signup", () => {
    render(<Home />);

    expect(screen.getByRole("heading", { name: "Make confident choices about your cover." })).toBeVisible();
    expect(screen.getByRole("link", { name: "Sign in" })).toHaveAttribute("href", "/login");
    expect(screen.getByRole("link", { name: "Get started" })).toHaveAttribute("href", "/signup");
    expect(screen.getByRole("link", { name: "Sign in to your account" })).toHaveAttribute("href", "/login");
  });
});
