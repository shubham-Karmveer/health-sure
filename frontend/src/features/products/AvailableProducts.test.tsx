import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  fetchAvailableProducts,
  getApiErrorMessage,
  searchAvailableProducts,
} from "@/lib/api/healthdekho-api";
import AvailableProducts from "./AvailableProducts";

vi.mock("@/lib/api/healthdekho-api", () => ({
  fetchAvailableProducts: vi.fn(),
  getApiErrorMessage: vi.fn(() => "Request failed"),
  searchAvailableProducts: vi.fn(),
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
  vi.mocked(fetchAvailableProducts).mockResolvedValue([product]);
  vi.mocked(searchAvailableProducts).mockResolvedValue({
    content: [product],
    page: 0,
    size: 20,
    totalElements: 1,
    totalPages: 1,
    last: true,
  });
});

describe("available products", () => {
  it("shows product information and starts registration for the selected product", async () => {
    const onRegister = vi.fn();
    render(<AvailableProducts onRegister={onRegister} />);

    expect(await screen.findByText("HealthDekho Secure Term")).toBeVisible();
    expect(screen.getByRole("cell", { name: "₹1,250.00" })).toBeVisible();
    expect(screen.getByRole("columnheader", { name: "Product code" })).toBeVisible();
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

  it("searches products by name and displays paginated results", async () => {
    vi.mocked(searchAvailableProducts).mockResolvedValueOnce({
      content: [product],
      page: 0,
      size: 1,
      totalElements: 2,
      totalPages: 2,
      last: false,
    });
    render(<AvailableProducts onRegister={vi.fn()} />);
    await screen.findByText("HealthDekho Secure Term");
    fireEvent.change(screen.getByLabelText("Policy name"), {
      target: { value: "Secure" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Search products" }));

    await waitFor(() => {
      expect(searchAvailableProducts).toHaveBeenCalledWith(
        { productCode: "", policyName: "Secure", policyType: "" },
        0
      );
    });
    expect(await screen.findByText("Page 1 of 2")).toBeVisible();
    fireEvent.click(screen.getByRole("button", { name: "Next" }));
    await waitFor(() => {
      expect(searchAvailableProducts).toHaveBeenLastCalledWith(
        { productCode: "", policyName: "Secure", policyType: "" },
        1
      );
    });
  });

  it("requires at least one product search criterion", async () => {
    render(<AvailableProducts onRegister={vi.fn()} />);
    await screen.findByText("HealthDekho Secure Term");
    fireEvent.click(screen.getByRole("button", { name: "Search products" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Enter a product code, policy name, or policy type to search."
    );
    expect(searchAvailableProducts).not.toHaveBeenCalled();
  });
});
