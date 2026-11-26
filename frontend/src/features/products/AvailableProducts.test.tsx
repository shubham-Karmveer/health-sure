import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { fetchAvailableProducts, getApiErrorMessage } from "@/lib/api/healthdekho-api";
import AvailableProducts from "./AvailableProducts";

vi.mock("@/lib/api/healthdekho-api", () => ({
  fetchAvailableProducts: vi.fn(),
  getApiErrorMessage: vi.fn(() => "Request failed"),
}));

afterEach(cleanup);

const product = {
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
};

beforeEach(() => {
  vi.clearAllMocks();
  vi.mocked(fetchAvailableProducts).mockResolvedValue([product]);
});

describe("available products", () => {
  it("shows product information and starts registration for the selected product", async () => {
    const onRegister = vi.fn();
    render(<AvailableProducts onRegister={onRegister} />);

    expect(await screen.findByRole("heading", { name: "HealthDekho Secure Term" })).toBeVisible();
    expect(screen.getByText("₹1,250.00 / monthly")).toBeVisible();
    fireEvent.click(screen.getByRole("button", { name: "Register HealthDekho Secure Term" }));

    expect(onRegister).toHaveBeenCalledWith("HD-TERM-001");
  });

  it("shows an empty state when no products are available", async () => {
    vi.mocked(fetchAvailableProducts).mockResolvedValueOnce([]);
    render(<AvailableProducts onRegister={vi.fn()} />);

    expect(await screen.findByRole("heading", { name: "No products available" })).toBeVisible();
  });

  it("displays product-loading errors", async () => {
    vi.mocked(fetchAvailableProducts).mockRejectedValueOnce(new Error("network error"));
    vi.mocked(getApiErrorMessage).mockReturnValueOnce("Products are unavailable.");
    render(<AvailableProducts onRegister={vi.fn()} />);

    expect(await screen.findByRole("alert")).toHaveTextContent("Products are unavailable.");
  });
});
