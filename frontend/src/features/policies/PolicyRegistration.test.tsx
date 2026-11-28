import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  fetchAvailableProducts,
  fetchDashboard,
  registerPolicy,
} from "@/lib/api/healthdekho-api";
import PolicyRegistration from "./PolicyRegistration";

const { push, replace } = vi.hoisted(() => ({ push: vi.fn(), replace: vi.fn() }));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push, replace }),
  useSearchParams: () => ({
    get: (name: string) => name === "productCode" ? "HD-TERM-001" : null,
  }),
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
  registerPolicy: vi.fn(),
}));

afterEach(cleanup);

const product = {
  productId: 1,
  productCode: "HD-TERM-001",
  policyName: "HealthDekho Secure Term",
  policyType: "TERM",
  termMonths: 240,
  premiumFrequency: "MONTHLY",
  premiumAmount: 1250,
  sumInsured: 5000000,
  minEntryAge: 18,
  maxEntryAge: 60,
};

beforeEach(() => {
  vi.clearAllMocks();
  vi.mocked(fetchDashboard).mockResolvedValue({
    customer: {
      customerId: 1,
      email: "customer@example.com",
      fullName: "Test Customer",
      dateOfBirth: "1990-04-15",
      phone: "5550100",
    },
    policies: [],
  });
  vi.mocked(fetchAvailableProducts).mockResolvedValue([product]);
  vi.mocked(registerPolicy).mockResolvedValue({
    policyNumber: "HD-POL-0001",
    confirmation: "Policy registered successfully.",
    maturityDate: "2050-01-30",
    totalPremiumPayable: 300000,
  });
});

describe("policy registration", () => {
  it("prefills customer and product data and displays calculated policy values", async () => {
    render(<PolicyRegistration />);

    expect(await screen.findByDisplayValue("Test Customer")).toBeVisible();
    expect(screen.getByRole("heading", { name: "HealthDekho Secure Term" })).toBeVisible();
    fireEvent.change(screen.getByLabelText("Policy start date"), {
      target: { value: "2030-01-31" },
    });
    expect(screen.getByText("2050-01-30")).toBeVisible();
    expect(screen.getByText("₹3,00,000.00")).toBeVisible();
  });

  it("submits the selected date and navigates to the registered policy", async () => {
    render(<PolicyRegistration />);
    const startDate = await screen.findByLabelText("Policy start date");
    fireEvent.change(startDate, { target: { value: "2030-01-31" } });
    expect(screen.getByText("2050-01-30")).toBeVisible();
    fireEvent.click(screen.getByRole("button", { name: "Confirm policy registration" }));

    await waitFor(() => {
      expect(registerPolicy).toHaveBeenCalledWith("HD-TERM-001", "2030-01-31");
      expect(push).toHaveBeenCalledWith("/policy?policyNumber=HD-POL-0001");
    });
  });
});
