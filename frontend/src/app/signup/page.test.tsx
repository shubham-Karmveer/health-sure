import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it } from "vitest";
import SignupPage from "./page";

afterEach(cleanup);

describe("signup page", () => {
  it("explains that account creation is unavailable and does not offer a fake submission", () => {
    render(<SignupPage />);

    expect(screen.getByRole("heading", { name: "Create your account" })).toBeVisible();
    expect(screen.getByRole("status")).toHaveTextContent("No information entered here will be submitted or stored.");
    expect(screen.getByRole("button", { name: "Sign-up is not available yet" })).toBeDisabled();
    expect(screen.getByRole("link", { name: "Sign in" })).toHaveAttribute("href", "/login");
  });
});
