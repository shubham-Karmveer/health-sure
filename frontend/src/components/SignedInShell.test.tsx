import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { signOut } from "@/lib/api/healthdekho-api";
import SignedInShell from "./SignedInShell";

const { replace } = vi.hoisted(() => ({ replace: vi.fn() }));

vi.mock("next/navigation", () => ({
  usePathname: () => "/products",
  useRouter: () => ({ replace }),
}));

vi.mock("@/lib/api/healthdekho-api", () => ({
  getApiErrorMessage: vi.fn(() => "Sign out failed."),
  signOut: vi.fn(),
}));

afterEach(cleanup);

beforeEach(() => {
  vi.clearAllMocks();
  vi.mocked(signOut).mockResolvedValue();
});

describe("signed-in shell", () => {
  it("shows links to the signed-in sections and marks the current page", () => {
    render(<SignedInShell pageTitle="Explore plans"><p>Page content</p></SignedInShell>);

    expect(screen.getByRole("heading", { name: "Explore plans" })).toBeVisible();
    expect(screen.getByRole("link", { name: "Dashboard" })).toHaveAttribute("href", "/dashboard");
    expect(screen.getByRole("link", { name: "Explore plans" })).toHaveAttribute("aria-current", "page");
    expect(screen.getByRole("link", { name: "AI recommendations" })).toHaveAttribute("href", "/recommendations");
    expect(screen.getByRole("link", { name: "Search policies" })).toHaveAttribute("href", "/dashboard#policy-search");
    expect(screen.getByText("Page content")).toBeVisible();
  });

  it("signs out and redirects to login", async () => {
    render(<SignedInShell pageTitle="Dashboard"><p>Content</p></SignedInShell>);
    fireEvent.click(screen.getByRole("button", { name: "Sign out" }));

    await waitFor(() => {
      expect(signOut).toHaveBeenCalledOnce();
      expect(replace).toHaveBeenCalledWith("/login");
    });
  });
});
