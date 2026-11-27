import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import PolicyPage from "./page";

vi.mock("next/navigation", () => ({
  usePathname: () => "/policy",
  useRouter: () => ({ replace: vi.fn() }),
}));

vi.mock("@/features/policies/PolicyDetails", () => ({
  default: () => <div>Payment content</div>,
}));

vi.mock("@/lib/api/healthdekho-api", () => ({
  getApiErrorMessage: vi.fn(),
  signOut: vi.fn(),
}));

afterEach(cleanup);

describe("payments page layout", () => {
  it("keeps the signed-in navigation visible and marks payments as active", () => {
    render(<PolicyPage />);

    expect(screen.getByRole("heading", { name: "Payments and policy details" })).toBeVisible();
    expect(screen.getByRole("link", { name: "Dashboard" })).toBeVisible();
    expect(screen.getByRole("link", { name: "Payments" })).toHaveAttribute("aria-current", "page");
    expect(screen.getByText("Payment content")).toBeVisible();
  });
});
