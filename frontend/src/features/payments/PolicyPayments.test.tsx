import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  fetchPaymentHistory,
  fetchPolicyDetails,
  fetchPolicyInstallments,
  recordPolicyPayments,
} from "@/lib/api/healthdekho-api";
import PolicyPayments from "./PolicyPayments";

const { replace } = vi.hoisted(() => ({ replace: vi.fn() }));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace }),
}));

vi.mock("@/lib/api/healthdekho-api", () => ({
  fetchPaymentHistory: vi.fn(),
  fetchPolicyDetails: vi.fn(),
  fetchPolicyInstallments: vi.fn(),
  getApiErrorMessage: vi.fn(() => "Request failed"),
  isUnauthorized: (error: unknown) =>
    typeof error === "object" &&
    error !== null &&
    "response" in error &&
    error.response !== null &&
    typeof error.response === "object" &&
    "status" in error.response &&
    error.response.status === 401,
  recordPolicyPayments: vi.fn(),
}));

afterEach(cleanup);

beforeEach(() => {
  vi.clearAllMocks();
  vi.mocked(fetchPolicyDetails).mockResolvedValue({
    policyNumber: "HD-POL-0001",
    customerName: "Test Customer",
    dateOfBirth: "1990-04-15",
    phone: "5550100",
    policyName: "HealthDekho Secure Term",
    policyType: "TERM",
    termMonths: 240,
    startDate: "2026-01-31",
    maturityDate: "2046-01-30",
    premiumFrequency: "MONTHLY",
    premiumAmount: 1250,
    sumInsured: 5000000,
    totalPremiumPayable: 300000,
    totalPremiumPaid: 0,
    remainingPremium: 300000,
    totalPenaltyAssessed: 0,
    totalPenaltyPaid: 0,
    remainingPenalty: 0,
    remainingBalance: 300000,
    estimatedPenalty: 0,
    estimatedBalance: 300000,
    status: "ACTIVE",
  });
  vi.mocked(fetchPolicyInstallments).mockResolvedValue([{
    installmentNumber: 1,
    dueDate: "2026-01-31",
    gracePeriodEndDate: "2026-02-28",
    scheduledAmount: 1250,
    paidAmount: 0,
    remainingPremium: 1250,
    penaltyAmount: 0,
    paidPenaltyAmount: 0,
    remainingPenalty: 0,
    estimatedPenalty: 0,
    remainingBalance: 1250,
    estimatedBalance: 1250,
    paymentStatus: "PENDING",
  }]);
  vi.mocked(fetchPaymentHistory).mockResolvedValue([]);
  vi.mocked(recordPolicyPayments).mockResolvedValue({
    policyNumber: "HD-POL-0001",
    confirmation: "Payment recorded successfully.",
    totalPremiumPayable: 300000,
    totalPremiumPaid: 1250,
    remainingPremium: 298750,
    totalPenaltyAssessed: 0,
    totalPenaltyPaid: 0,
    remainingPenalty: 0,
    remainingBalance: 298750,
    payments: [{
      transactionReference: "HD-TXN-0001",
      installmentNumber: 1,
      paymentDate: "2026-11-26T10:00:00Z",
      amountPaid: 1250,
      premiumAmount: 1250,
      penaltyAmount: 0,
      paymentMethod: "UPI",
    }],
  });
});

describe("policy payment page", () => {
  it("shows schedule details and submits the selected installment", async () => {
    render(<PolicyPayments policyNumber="HD-POL-0001" />);

    expect(await screen.findByRole("heading", { name: "Policy HD-POL-0001" })).toBeVisible();
    expect(screen.getByText("2026-02-28")).toBeVisible();
    fireEvent.click(screen.getByRole("checkbox", { name: "Select installment 1" }));
    fireEvent.click(screen.getByRole("button", { name: "Record selected payments" }));

    expect(await screen.findByRole("status")).toHaveTextContent("1 payment(s) recorded.");
    await waitFor(() => {
      expect(recordPolicyPayments).toHaveBeenCalledWith(
        "HD-POL-0001",
        "UPI",
        [{ installmentNumber: 1, amountPaid: 1250 }]
      );
    });
  });
});
