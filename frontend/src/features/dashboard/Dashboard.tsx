"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  DashboardData,
  fetchDashboard,
  isUnauthorized,
} from "@/lib/api/healthdekho-api";
import PolicySearch from "@/features/policies/PolicySearch";

export default function Dashboard() {
  const router = useRouter();
  const [dashboard, setDashboard] = useState<DashboardData | null>(null);
  const [loading, setLoading] = useState(true);
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

  return (
    <div className="dashboard-content">
      {error && <p className="alert" role="alert">{error}</p>}

      {loading ? (
        <p className="loading" role="status">Loading your dashboard…</p>
      ) : dashboard ? (
        <>
          <section className="dashboard-welcome" aria-labelledby="dashboard-title">
            <div>
              <p className="eyebrow">Your account</p>
              <h2 id="dashboard-title">Welcome back, {dashboard.customer.fullName}</h2>
              <p className="muted">{dashboard.customer.email}</p>
            </div>
            <Link className="button" href="/products">Explore available plans</Link>
          </section>
          <section className="dashboard-metrics" aria-label="Policy overview">
            <article className="metric-card">
              <span className="metric-label">Registered policies</span>
              <strong>{dashboard.policies.length}</strong>
            </article>
            <article className="metric-card">
              <span className="metric-label">Active policies</span>
              <strong>{dashboard.policies.filter((policy) => policy.status === "ACTIVE").length}</strong>
            </article>
            <article className="metric-card">
              <span className="metric-label">Cover types</span>
              <strong>{new Set(dashboard.policies.map((policy) => policy.policyType)).size}</strong>
            </article>
          </section>
          <section className="panel" aria-labelledby="dashboard-policies-title">
            <div className="section-heading">
              <div>
                <p className="eyebrow">Policy portfolio</p>
                <h2 id="dashboard-policies-title">Your registered policies</h2>
              </div>
              <div className="section-actions">
                <span className="count">{dashboard.policies.length}</span>
              </div>
            </div>
            {dashboard.policies.length === 0 ? (
              <div className="empty-state">
                <h3>No policies yet</h3>
                <p>Explore the available plans to find a policy for your needs.</p>
                <Link className="button" href="/products">Browse available plans</Link>
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
          <PolicySearch />
        </>
      ) : (
        <p className="muted">Redirecting to sign in…</p>
      )}
    </div>
  );
}

function formatMoney(amount: number): string {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 2,
  }).format(amount);
}
