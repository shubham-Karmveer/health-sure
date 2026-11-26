"use client";

import { FormEvent, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import {
  AvailableProduct,
  CustomerProfile,
  fetchAvailableProducts,
  fetchDashboard,
  getApiErrorMessage,
  isUnauthorized,
  registerPolicy,
} from "@/lib/api/healthdekho-api";

export default function PolicyRegistration() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const productCode = searchParams.get("productCode") ?? "";
  const [startDate, setStartDate] = useState(() => getLocalDateString(new Date()));
  const [customer, setCustomer] = useState<CustomerProfile | null>(null);
  const [product, setProduct] = useState<AvailableProduct | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const totalPremium = product ? calculateTotalPremium(product) : null;

  useEffect(() => {
    let active = true;
    if (!productCode) {
      return () => {
        active = false;
      };
    }

    Promise.all([fetchDashboard(), fetchAvailableProducts()])
      .then(([dashboard, products]) => {
        if (active) {
          setCustomer(dashboard.customer);
          setProduct(products.find((candidate) => candidate.productCode === productCode) ?? null);
        }
      })
      .catch((requestError: unknown) => {
        if (active) {
          if (isUnauthorized(requestError)) {
            router.replace("/login");
          } else {
            setError(getApiErrorMessage(requestError));
          }
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [productCode, router]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!product) {
      return;
    }
    const formData = new FormData(event.currentTarget);
    setSubmitting(true);
    setError("");
    try {
      const registration = await registerPolicy(product.productCode, String(formData.get("startDate")));
      router.push(`/policy?policyNumber=${encodeURIComponent(registration.policyNumber)}`);
    } catch (requestError) {
      if (isUnauthorized(requestError)) {
        router.replace("/login");
      } else {
        setError(getApiErrorMessage(requestError));
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="shell">
      <header className="topbar">
        <div>
          <Link className="text-link" href="/dashboard">HealthDekho</Link>
          <p className="eyebrow">Policy registration</p>
        </div>
        <Link className="text-link" href="/dashboard">Back to dashboard</Link>
      </header>

      <section className="panel registration-panel" aria-labelledby="registration-title">
        <p className="eyebrow">Review your details</p>
        <h1 id="registration-title">Register for a policy</h1>
        {!productCode ? (
          <p className="alert" role="alert">Choose an available product before registering a policy.</p>
        ) : error ? (
          <p className="alert" role="alert">{error}</p>
        ) : loading ? (
          <p className="loading" role="status">Loading registration details…</p>
        ) : product && customer ? (
          <form onSubmit={(event) => void handleSubmit(event)}>
            <h2>{product.policyName}</h2>
            <p className="muted">{product.policyType} · {product.productCode}</p>
            <div className="registration-facts">
              <p><strong>Term:</strong> {product.termMonths} months</p>
              <p><strong>Premium:</strong> {formatMoney(product.premiumAmount)} / {product.premiumFrequency.toLowerCase()}</p>
              <p><strong>Sum insured:</strong> {formatMoney(product.sumInsured)}</p>
              <p><strong>Maturity date:</strong> {calculateMaturityDate(startDate, product.termMonths) ?? "Select a start date"}</p>
              <p>
                <strong>Total premium payable:</strong>{" "}
                {totalPremium === null
                  ? "Unavailable for this product schedule"
                  : formatMoney(totalPremium)}
              </p>
            </div>
            <label htmlFor="customer-name">Customer name</label>
            <input id="customer-name" value={customer.fullName} readOnly />
            <label htmlFor="customer-dob">Date of birth</label>
            <input id="customer-dob" value={customer.dateOfBirth} readOnly />
            <label htmlFor="customer-phone">Contact number</label>
            <input id="customer-phone" value={customer.phone ?? "Not provided"} readOnly />
            <label htmlFor="customer-email">Email</label>
            <input id="customer-email" value={customer.email} readOnly />
            <label htmlFor="start-date">Policy start date</label>
            <input
              id="start-date"
              name="startDate"
              type="date"
              min={getLocalDateString(new Date())}
              value={startDate}
              onChange={(event) => setStartDate(event.target.value)}
              required
            />
            <button className="button" type="submit" disabled={submitting}>
              {submitting ? "Registering…" : "Confirm policy registration"}
            </button>
          </form>
        ) : (
          <div className="empty-state">
            <h2>Product unavailable</h2>
            <p>This product is no longer available for your account.</p>
          </div>
        )}
      </section>
    </main>
  );
}

function formatMoney(amount: number): string {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 2,
  }).format(amount);
}

function getLocalDateString(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function calculateTotalPremium(product: AvailableProduct): number | null {
  const intervalMonths = {
    MONTHLY: 1,
    QUARTERLY: 3,
    HALF_YEARLY: 6,
    YEARLY: 12,
  }[product.premiumFrequency];
  if (!intervalMonths || product.termMonths % intervalMonths !== 0) {
    return null;
  }
  return product.premiumAmount * (product.termMonths / intervalMonths);
}

function calculateMaturityDate(startDate: string, termMonths: number): string | null {
  if (!startDate) {
    return null;
  }
  const [year, month, day] = startDate.split("-").map(Number);
  const targetMonthIndex = month - 1 + termMonths;
  const targetYear = year + Math.floor(targetMonthIndex / 12);
  const normalizedMonth = targetMonthIndex % 12;
  const lastDay = new Date(Date.UTC(targetYear, normalizedMonth + 1, 0)).getUTCDate();
  const maturity = new Date(Date.UTC(targetYear, normalizedMonth, Math.min(day, lastDay)));
  maturity.setUTCDate(maturity.getUTCDate() - 1);
  return `${maturity.getUTCFullYear()}-${String(maturity.getUTCMonth() + 1).padStart(2, "0")}-${String(maturity.getUTCDate()).padStart(2, "0")}`;
}
