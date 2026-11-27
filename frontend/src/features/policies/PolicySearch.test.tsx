import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  getApiErrorMessage,
  PolicySearchResult,
  searchCustomerPolicies,
} from "@/lib/api/healthdekho-api";
import PolicySearch from "./PolicySearch";

vi.mock("@/lib/api/healthdekho-api", () => ({
  getApiErrorMessage: vi.fn(() => "Request failed"),
  searchCustomerPolicies: vi.fn(),
}));

afterEach(cleanup);

const policy: PolicySearchResult = {
  policyNumber: "HD-POL-0001",
  customerName: "Test Customer",
  dateOfBirth: "1990-04-15",
  policyName: "HealthDekho Secure Health",
  policyType: "HEALTH",
  startDate: "2026-01-31",
  maturityDate: "2036-01-30",
  totalPremiumPayable: 1200,
  premiumPaymentStatus: "PARTIALLY_PAID",
  totalPremiumPaid: 250,
  remainingPremium: 950,
};

function page(
  content: PolicySearchResult[],
  pageNumber = 0,
  totalElements = content.length,
  totalPages = 1
) {
  return {
    content,
    page: pageNumber,
    size: 1,
    totalElements,
    totalPages,
    last: pageNumber === totalPages - 1,
  };
}

beforeEach(() => {
  vi.clearAllMocks();
  vi.mocked(searchCustomerPolicies).mockResolvedValue(page([policy]));
});

describe("customer policy search", () => {
  it("searches by policy identifier and date of birth and displays required fields", async () => {
    render(<PolicySearch />);
    fireEvent.change(screen.getByLabelText("Policy identifier"), {
      target: { value: " HD-POL-0001 " },
    });
    fireEvent.change(screen.getByLabelText("Date of birth"), {
      target: { value: "1990-04-15" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Search policies" }));

    await waitFor(() => {
      expect(searchCustomerPolicies).toHaveBeenCalledWith(
        { policyNumber: "HD-POL-0001", customerName: "", dateOfBirth: "1990-04-15" },
        0
      );
    });
    expect(await screen.findByRole("cell", { name: "Test Customer" })).toBeVisible();
    expect(screen.getByRole("columnheader", { name: "Remaining premium" })).toBeVisible();
    expect(screen.getByRole("cell", { name: "₹950.00" })).toBeVisible();
    expect(screen.getByRole("link", { name: "Manage payments" })).toHaveAttribute(
      "href",
      "/policy?policyNumber=HD-POL-0001"
    );
  });

  it("searches by partial customer name and retains criteria across pages", async () => {
    const secondPagePolicy = { ...policy, policyNumber: "HD-POL-0002" };
    vi.mocked(searchCustomerPolicies)
      .mockResolvedValueOnce(page([policy], 0, 2, 2))
      .mockResolvedValueOnce(page([secondPagePolicy], 1, 2, 2));
    render(<PolicySearch />);
    fireEvent.change(screen.getByLabelText("Customer name"), {
      target: { value: "  cUsToMeR  " },
    });
    fireEvent.click(screen.getByRole("button", { name: "Search policies" }));

    await screen.findByText("Page 1 of 2");
    await waitFor(() => {
      expect(searchCustomerPolicies).toHaveBeenCalledWith(
        { policyNumber: "", customerName: "cUsToMeR", dateOfBirth: "" },
        0
      );
    });
    fireEvent.click(screen.getByRole("button", { name: "Next" }));

    expect(await screen.findByText("HD-POL-0002")).toBeVisible();
    expect(searchCustomerPolicies).toHaveBeenLastCalledWith(
      { policyNumber: "", customerName: "cUsToMeR", dateOfBirth: "" },
      1
    );
  });

  it("rejects unsupported search criteria without calling the API", async () => {
    render(<PolicySearch />);
    fireEvent.change(screen.getByLabelText("Date of birth"), {
      target: { value: "1990-04-15" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Search policies" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Search by policy identifier, customer name, or policy identifier with date of birth."
    );
    expect(searchCustomerPolicies).not.toHaveBeenCalled();
  });

  it("shows a clear no-results state", async () => {
    vi.mocked(searchCustomerPolicies).mockResolvedValueOnce(page([]));
    render(<PolicySearch />);
    fireEvent.change(screen.getByLabelText("Policy identifier"), {
      target: { value: "HD-POL-MISSING" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Search policies" }));

    expect(await screen.findByRole("heading", { name: "No matching policies" })).toBeVisible();
  });

  it("shows API errors", async () => {
    vi.mocked(searchCustomerPolicies).mockRejectedValueOnce(new Error("network error"));
    render(<PolicySearch />);
    fireEvent.change(screen.getByLabelText("Policy identifier"), {
      target: { value: "HD-POL-0001" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Search policies" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Request failed");
    expect(getApiErrorMessage).toHaveBeenCalled();
  });
});
