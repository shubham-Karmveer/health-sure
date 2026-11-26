import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  DashboardData,
  fetchAvailableProducts,
  fetchDashboard,
  signOut,
} from "@/lib/api/healthdekho-api";
import Dashboard from "./Dashboard";

const { push, replace } = vi.hoisted(() => ({ push: vi.fn(), replace: vi.fn() }));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push, replace }),
}));

vi.mock("@/lib/api/healthdekho-api", () => ({
  fetchAvailableProducts: vi.fn(),
  fetchDashboard: vi.fn(),
  getApiErrorMessage: vi.fn(() => "Request failed"),
  isUnauthorized: (error: unknown) =>
    typeof error === "object" &&
    error !== null &&
    "response" in error &&
    error.response !== null &&
    typeof error.response === "object" &&
    "status" in error.response &&
    error.response.status === 401,
  signOut: vi.fn(),
}));

afterEach(cleanup);

const dashboard: DashboardData = {
  customer: {
    customerId: "customer-1",
    email: "customer@example.com",
    fullName: "Test Customer",
    dateOfBirth: "1990-04-15",
    phone: null,
  },
  policies: [
    {
      policyNumber: "HD-POL-0001",
      policyName: "HealthDekho Secure Term",
      policyType: "TERM",
      termMonths: 240,
      startDate: "2026-01-31",
      maturityDate: "2046-01-30",
      premiumFrequency: "MONTHLY",
      premiumAmount: 1250,
      sumInsured: 5000000,
      status: "ACTIVE",
    },
  ],
};

beforeEach(() => {
  vi.clearAllMocks();
  vi.mocked(fetchDashboard).mockResolvedValue(dashboard);
  vi.mocked(fetchAvailableProducts).mockResolvedValue([]);
  vi.mocked(signOut).mockResolvedValue();
});

describe("customer dashboard", () => {
  it("shows policy fields and navigates to policy details", async () => {
    render(<Dashboard />);

    expect(await screen.findByRole("heading", { name: "Welcome, Test Customer" })).toBeVisible();
    expect(screen.getByText("HD-POL-0001")).toBeVisible();
    expect(screen.getByText("2026-01-31")).toBeVisible();
    expect(screen.getByText("2046-01-30")).toBeVisible();
    expect(screen.getByRole("link", { name: "Manage payments" })).toHaveAttribute(
      "href",
      "/policy?policyNumber=HD-POL-0001"
    );
    expect(screen.getByRole("link", { name: "Find a policy by number" })).toHaveAttribute(
      "href",
      "/policy"
    );
    expect(screen.getByRole("heading", { name: "Get product recommendations" })).toBeVisible();
  });

  it("opens registration for the selected product in the current tab", async () => {
    vi.mocked(fetchAvailableProducts).mockResolvedValueOnce([{
      productId: "product-1",
      productCode: "HD-TERM-001",
      policyName: "HealthDekho Secure Term",
      policyType: "TERM",
      termMonths: 240,
      premiumFrequency: "MONTHLY",
      premiumAmount: 1250,
      sumInsured: 5000000,
      minEntryAge: 18,
      maxEntryAge: 60,
    }]);
    render(<Dashboard />);
    await screen.findByRole("heading", { name: "Welcome, Test Customer" });
    fireEvent.click(await screen.findByRole("button", { name: "Register HealthDekho Secure Term" }));

    expect(push).toHaveBeenCalledWith("/register?productCode=HD-TERM-001");
  });

  it("redirects unauthenticated customers to sign-in", async () => {
    vi.mocked(fetchDashboard).mockRejectedValueOnce({ response: { status: 401 } });
    render(<Dashboard />);

    await waitFor(() => expect(replace).toHaveBeenCalledWith("/login"));
  });

  it("signs out and returns to sign-in", async () => {
    render(<Dashboard />);
    await screen.findByRole("heading", { name: "Welcome, Test Customer" });
    fireEvent.click(screen.getByRole("button", { name: "Sign out" }));

    await waitFor(() => {
      expect(signOut).toHaveBeenCalledOnce();
      expect(replace).toHaveBeenCalledWith("/login");
    });
  });
});
