import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { getApiErrorMessage, signIn } from "@/lib/api/healthdekho-api";
import LoginForm from "./LoginForm";

const { replace } = vi.hoisted(() => ({ replace: vi.fn() }));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace }),
}));

vi.mock("@/lib/api/healthdekho-api", () => ({
  getApiErrorMessage: vi.fn(() => "Request failed"),
  signIn: vi.fn(),
}));

afterEach(cleanup);

beforeEach(() => {
  vi.clearAllMocks();
  vi.mocked(signIn).mockResolvedValue();
});

describe("login form", () => {
  it("signs in and redirects to the dashboard", async () => {
    render(<LoginForm />);
    fireEvent.change(screen.getByLabelText("Email"), {
      target: { value: "customer@example.com" },
    });
    fireEvent.change(screen.getByLabelText("Password"), {
      target: { value: "correct-password" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Sign in" }));

    await waitFor(() => {
      expect(signIn).toHaveBeenCalledWith("customer@example.com", "correct-password");
      expect(replace).toHaveBeenCalledWith("/dashboard");
    });
  });

  it("shows an authentication error without navigating", async () => {
    vi.mocked(signIn).mockRejectedValueOnce(new Error("invalid credentials"));
    vi.mocked(getApiErrorMessage).mockReturnValueOnce("Invalid email or password.");
    render(<LoginForm />);
    fireEvent.change(screen.getByLabelText("Email"), {
      target: { value: "customer@example.com" },
    });
    fireEvent.change(screen.getByLabelText("Password"), {
      target: { value: "wrong-password" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Sign in" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Invalid email or password.");
    expect(replace).not.toHaveBeenCalled();
  });
});
