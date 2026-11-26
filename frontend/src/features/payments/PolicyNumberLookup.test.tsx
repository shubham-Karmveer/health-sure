import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import PolicyNumberLookup from "./PolicyNumberLookup";

const { push } = vi.hoisted(() => ({ push: vi.fn() }));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push }),
}));

afterEach(cleanup);

describe("policy number lookup", () => {
  it("opens the owner-scoped policy view for the entered policy number", () => {
    render(<PolicyNumberLookup />);
    fireEvent.change(screen.getByLabelText("Policy identifier"), {
      target: { value: "HD-POL-0001" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Find policy" }));

    expect(push).toHaveBeenCalledWith("/policy?policyNumber=HD-POL-0001");
  });

  it("requires a policy number", () => {
    render(<PolicyNumberLookup />);
    fireEvent.click(screen.getByRole("button", { name: "Find policy" }));

    expect(screen.getByRole("alert")).toHaveTextContent("Enter a policy identifier to continue.");
    expect(push).not.toHaveBeenCalled();
  });
});
