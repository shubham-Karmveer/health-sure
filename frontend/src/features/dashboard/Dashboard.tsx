"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import {
  DashboardData,
  fetchDashboard,
  getApiErrorMessage,
  isUnauthorized,
  signIn,
  signOut,
} from "@/lib/api/healthdekho-api";

export default function Dashboard() {
  const [dashboard, setDashboard] = useState<DashboardData | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  const loadDashboard = useCallback(async () => {
    try {
      setDashboard(await fetchDashboard());
    } catch (requestError) {
      if (isUnauthorized(requestError)) {
        setDashboard(null);
      } else {
        setError("HealthDekho could not connect to the API. Check that the local backend is running.");
      }
    } finally {
      setLoading(false);
    }
  }, []);

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
          setDashboard(null);
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
  }, []);

  async function handleSignIn(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    setSubmitting(true);
    setLoading(true);
    setError("");
    try {
      await signIn(String(formData.get("email")), String(formData.get("password")));
      await loadDashboard();
    } catch (requestError) {
      setLoading(false);
      setError(getApiErrorMessage(requestError));
    } finally {
      setSubmitting(false);
    }
  }

  async function handleSignOut() {
    setSubmitting(true);
    setError("");
    try {
      await signOut();
      setDashboard(null);
    } catch (requestError) {
      setError(getApiErrorMessage(requestError));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="shell">
      <header className="topbar">
        <div>
          <span className="eyebrow">Insurance, made clearer</span>
          <h1>HealthDekho</h1>
        </div>
        {dashboard && (
          <button className="button button-secondary" disabled={submitting} onClick={handleSignOut}>
            Sign out
          </button>
        )}
      </header>

      {error && <p className="alert" role="alert">{error}</p>}

      {loading ? (
        <p className="loading" role="status">Checking your account…</p>
      ) : dashboard ? (
        <section className="panel" aria-labelledby="dashboard-title">
          <div className="section-heading">
            <div>
              <p className="eyebrow">Customer dashboard</p>
              <h2 id="dashboard-title">Welcome, {dashboard.customer.fullName}</h2>
              <p className="muted">{dashboard.customer.email}</p>
            </div>
          </div>

          <div className="section-heading">
            <h3>Your policies</h3>
            <span className="count">{dashboard.policies.length}</span>
          </div>
          {dashboard.policies.length === 0 ? (
            <div className="empty-state">
              <h4>No policies yet</h4>
              <p>Available insurance products will appear here when you are ready to explore them.</p>
            </div>
          ) : (
            <div className="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>Policy</th>
                    <th>Type</th>
                    <th>Term</th>
                    <th>Premium</th>
                    <th>Coverage</th>
                    <th>Status</th>
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
                      <td>{formatMoney(policy.premiumAmount)} / {policy.premiumFrequency.toLowerCase()}</td>
                      <td>{formatMoney(policy.sumInsured)}</td>
                      <td><span className="status">{policy.status}</span></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      ) : (
        <section className="panel sign-in-panel" aria-labelledby="sign-in-title">
          <p className="eyebrow">Customer access</p>
          <h2 id="sign-in-title">Sign in to your account</h2>
          <p className="muted">Use your HealthDekho customer credentials to continue.</p>
          <form onSubmit={handleSignIn}>
            <label htmlFor="email">Email</label>
            <input id="email" name="email" type="email" autoComplete="username" required />
            <label htmlFor="password">Password</label>
            <input id="password" name="password" type="password" autoComplete="current-password" required />
            <button className="button" type="submit" disabled={submitting}>
              {submitting ? "Signing in…" : "Sign in"}
            </button>
          </form>
        </section>
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
