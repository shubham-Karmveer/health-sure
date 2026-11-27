"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import SignedInShell from "@/components/SignedInShell";
import Recommendations from "@/features/recommendations/Recommendations";
import {
  CustomerProfile,
  fetchDashboard,
  getApiErrorMessage,
  isUnauthorized,
} from "@/lib/api/healthdekho-api";

export default function RecommendationsPage() {
  const router = useRouter();
  const [customer, setCustomer] = useState<CustomerProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    fetchDashboard()
      .then((dashboard) => {
        if (active) {
          setCustomer(dashboard.customer);
        }
      })
      .catch((requestError: unknown) => {
        if (!active) {
          return;
        }
        if (isUnauthorized(requestError)) {
          router.replace("/login");
        } else {
          setError(getApiErrorMessage(requestError));
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

  function handleRegistration(productCode: string) {
    router.push(`/register?productCode=${encodeURIComponent(productCode)}`);
  }

  return (
    <SignedInShell pageTitle="AI recommendations">
      <section className="page-intro">
        <p className="eyebrow">Personalized discovery</p>
        <h2>Find a plan for your needs</h2>
        <p className="muted">
          Recommendations use your profile and eligible active products. Review each suggestion before registering.
        </p>
      </section>
      {error && <p className="alert" role="alert">{error}</p>}
      {loading ? (
        <p className="loading" role="status">Loading your profile…</p>
      ) : customer ? (
        <Recommendations
          dateOfBirth={customer.dateOfBirth}
          onRegister={handleRegistration}
        />
      ) : error ? null : (
        <p className="muted">Redirecting to sign in…</p>
      )}
    </SignedInShell>
  );
}
