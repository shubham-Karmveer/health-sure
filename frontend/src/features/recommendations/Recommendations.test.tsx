import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { createRecommendation } from "@/lib/api/healthdekho-api";
import Recommendations from "./Recommendations";

vi.mock("@/lib/api/healthdekho-api", () => ({
  createRecommendation: vi.fn(),
  getApiErrorMessage: vi.fn(() => "Recommendations are unavailable."),
}));

afterEach(cleanup);

beforeEach(() => {
  vi.clearAllMocks();
  vi.mocked(createRecommendation).mockResolvedValue({
    requestId: "recommendation-1",
    dateOfBirth: "1990-04-15",
    calculatedAge: 36,
    insuranceGoals: "Family protection",
    preferredTerm: "20 years",
    preferredPolicyType: "TERM",
    modelName: "local-stub",
    modelVersion: "v1",
    promptVersion: "test",
    requestedAt: "2026-11-26T10:00:00Z",
    recommendations: [{
      productId: "product-1",
      productCode: "HD-TERM-001",
      policyName: "HealthDekho Secure Term",
      policyType: "TERM",
      termMonths: 240,
      premiumFrequency: "MONTHLY",
      premiumAmount: 1250,
      sumInsured: 5000000,
      rankOrder: 1,
      recommendationReason: "Matches the selected insurance type.",
      suitability: "HIGH",
      selectedForRegistration: false,
    }],
  });
});

describe("recommendations", () => {
  it("submits profile details, displays results, and starts registration", async () => {
    const onRegister = vi.fn();
    render(<Recommendations dateOfBirth="1990-04-15" onRegister={onRegister} />);
    expect(screen.queryByLabelText("Date of birth")).not.toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("Insurance goals or preferences"), {
      target: { value: "Family protection" },
    });
    fireEvent.change(screen.getByLabelText("Preferred policy term"), {
      target: { value: "20 years" },
    });
    fireEvent.change(screen.getByLabelText("Insurance type"), {
      target: { value: "TERM" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Get recommendations" }));

    expect(await screen.findByText("Matches the selected insurance type.")).toBeVisible();
    expect(createRecommendation).toHaveBeenCalledWith({
      dateOfBirth: "1990-04-15",
      insuranceGoals: "Family protection",
      preferredTerm: "20 years",
      preferredPolicyType: "TERM",
    });
    fireEvent.click(screen.getByRole("button", { name: "Register HealthDekho Secure Term" }));
    expect(onRegister).toHaveBeenCalledWith("HD-TERM-001");
  });

  it("shows a clear error when recommendation generation fails", async () => {
    vi.mocked(createRecommendation).mockRejectedValueOnce(new Error("provider down"));
    render(<Recommendations dateOfBirth="1990-04-15" onRegister={vi.fn()} />);
    fireEvent.click(screen.getByRole("button", { name: "Get recommendations" }));

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent("Recommendations are unavailable.");
    });
  });
});
