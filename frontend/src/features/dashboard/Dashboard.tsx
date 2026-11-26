"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  DashboardData,
  fetchDashboard,
  getApiErrorMessage,
  isUnauthorized,
  signOut,
} from "@/lib/api/healthdekho-api";
import AvailableProducts from "@/features/products/AvailableProducts";
import Recommendations from "@/features/recommendations/Recommendations";

export default function Dashboard() {
  const router = useRouter();
  const [dashboard, setDashboard] = useState<DashboardData | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    fetchDashboard()
      .then((data) => {
        if (active) {
          setDashboard(data);
        }
      })
      .catch((requestError: unknown) => {
        if (!active) {
          return;
        }
        if (isUnauthorized(requestError)) {
          router.replace("/login");
        } else {
          setError("HealthDekho could not connect to the API. Check that the local backend is running.");
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
  }, [router]);

  async function handleSignOut() {
    setSubmitting(true);
    setError("");
    try {
      await signOut();
      router.replace("/login");
    } catch (requestError) {
      setError(getApiErrorMessage(requestError));
    } finally {
      setSubmitting(false);
    }
  }

  function handleRegistration(productCode: string) {
    router.push(`/register?productCode=${encodeURIComponent(productCode)}`);
  }

  return (
    <main className="shell">
      <header className="topbar">
        <div>
          <Link className="text-link" href="/">HealthDekho</Link>
          <p className="eyebrow">Customer dashboard</p>
        </div>
        <button className="button button-secondary" disabled={submitting} onClick={handleSignOut}>
          Sign out
        </button>
      </header>

      {error && <p className="alert" role="alert">{error}</p>}

      {loading ? (
        <p className="loading" role="status">Loading your dashboard…</p>
      ) : dashboard ? (
        <>
          <section className="panel" aria-labelledby="dashboard-title">
            <div className="section-heading">
              <div>
                <p className="eyebrow">Your account</p>
                <h1 id="dashboard-title">Welcome, {dashboard.customer.fullName}</h1>
                <p className="muted">{dashboard.customer.email}</p>
              </div>
            </div>

            <div className="section-heading">
              <h2>Your policies</h2>
              <div className="section-actions">
                <Link className="text-link" href="/policy">Find a policy by number</Link>
                <span className="count">{dashboard.policies.length}</span>
              </div>
            </div>
            {dashboard.policies.length === 0 ? (
              <div className="empty-state">
                <h3>No policies yet</h3>
                <p>Browse available products below to register your first policy.</p>
              </div>
            ) : (
              <div className="table-scroll">
                <table>
                  <thead>
                    <tr>
                      <th>Policy</th>
                      <th>Type</th>
                      <th>Term</th>
                      <th>Start date</th>
                      <th>Maturity date</th>
                      <th>Premium</th>
                      <th>Coverage</th>
                      <th>Status</th>
                      <th>Payments</th>
                    </tr>
                  </thead>
                  <tbody>
                    {dashboard.policies.map((policy) => (
                      <tr key={policy.policyNumber}>
                        <td>
                          <strong>{policy.policyName}</strong>
                          <span className="table-subtitle">{policy.policyNumber}</span>
                        </td>
                        <td>{policy.policyType}</td>
                        <td>{policy.termMonths} months</td>
                        <td>{policy.startDate}</td>
                        <td>{policy.maturityDate}</td>
                        <td>{formatMoney(policy.premiumAmount)} / {policy.premiumFrequency.toLowerCase()}</td>
                        <td>{formatMoney(policy.sumInsured)}</td>
                        <td><span className="status">{policy.status}</span></td>
                        <td>
                          <Link
                            className="button button-secondary"
                            href={`/policy?policyNumber=${encodeURIComponent(policy.policyNumber)}`}
                          >
                            Manage payments
                          </Link>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
          <AvailableProducts onRegister={handleRegistration} />
          <Recommendations
            dateOfBirth={dashboard.customer.dateOfBirth}
            onRegister={handleRegistration}
          />
        </>
      ) : (
        <p className="muted">Redirecting to sign in…</p>
      )}
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
