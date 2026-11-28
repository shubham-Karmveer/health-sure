import { cleanup, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  DashboardData,
  fetchDashboard,
} from "@/lib/api/healthdekho-api";
import Dashboard from "./Dashboard";

const { replace } = vi.hoisted(() => ({ replace: vi.fn() }));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace }),
}));

vi.mock("@/lib/api/healthdekho-api", () => ({
  fetchDashboard: vi.fn(),
  getApiErrorMessage: vi.fn(() => "Request failed"),
  searchCustomerPolicies: vi.fn(),
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

const dashboard: DashboardData = {
  customer: {
    customerId: 1,
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
});

describe("customer dashboard", () => {
  it("shows policy fields and navigates to policy details", async () => {
    render(<Dashboard />);

    expect(await screen.findByRole("heading", { name: "Welcome back, Test Customer" })).toBeVisible();
    expect(screen.getByText("HD-POL-0001")).toBeVisible();
    expect(screen.getByText("2026-01-31")).toBeVisible();
    expect(screen.getByText("2046-01-30")).toBeVisible();
    expect(screen.getByText("Registered policies")).toBeVisible();
    expect(screen.getByText("Active policies")).toBeVisible();
    expect(screen.getByRole("link", { name: "Manage payments" })).toHaveAttribute(
      "href",
      "/policy?policyNumber=HD-POL-0001"
    );
    expect(screen.getByRole("heading", { name: "Search your policies" })).toBeVisible();
    expect(screen.queryByRole("heading", { name: "Available products" })).not.toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "Get product recommendations" })).not.toBeInTheDocument();
  });

  it("links customers with no policies to available plans", async () => {
    vi.mocked(fetchDashboard).mockResolvedValueOnce({ ...dashboard, policies: [] });
    render(<Dashboard />);

    expect(await screen.findByRole("heading", { name: "No policies yet" })).toBeVisible();
    expect(screen.getByRole("link", { name: "Browse available plans" })).toHaveAttribute(
      "href",
      "/products"
    );
  });

  it("redirects unauthenticated customers to sign-in", async () => {
    vi.mocked(fetchDashboard).mockRejectedValueOnce({ response: { status: 401 } });
    render(<Dashboard />);

    await waitFor(() => expect(replace).toHaveBeenCalledWith("/login"));
  });
});
