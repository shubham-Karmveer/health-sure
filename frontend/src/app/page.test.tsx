import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import Home from "./page";

vi.mock("@/lib/api/healthdekho-api", () => ({
  fetchDashboard: () =>
    Promise.reject({ response: { status: 401 } }),
  signIn: vi.fn(),
  signOut: vi.fn(),
  getApiErrorMessage: () => "Request failed",
  isUnauthorized: (error: unknown) =>
    typeof error === "object" &&
    error !== null &&
    "response" in error &&
    error.response !== null &&
    typeof error.response === "object" &&
    "status" in error.response &&
    error.response.status === 401,
}));

afterEach(cleanup);

describe("home page", () => {
  it("shows sign-in for an unauthenticated customer", async () => {
    render(<Home />);

    expect(await screen.findByRole("heading", { name: "HealthDekho" })).toBeVisible();
    expect(screen.getByLabelText("Email")).toBeVisible();
    expect(screen.getByLabelText("Password")).toBeVisible();
  });
});
