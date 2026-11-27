"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import SignedInShell from "@/components/SignedInShell";
import AvailableProducts from "@/features/products/AvailableProducts";
import { fetchDashboard, getApiErrorMessage, isUnauthorized } from "@/lib/api/healthdekho-api";

export default function ProductsPage() {
  const router = useRouter();
  const [checkingAccess, setCheckingAccess] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    fetchDashboard()
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
          setCheckingAccess(false);
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
    <SignedInShell pageTitle="Explore available plans">
      <section className="page-intro">
        <p className="eyebrow">Find cover that fits</p>
        <h2>Available policies</h2>
        <p className="muted">
          Compare the active plans available to your account and start a registration when you’re ready.
        </p>
      </section>
      {error && <p className="alert" role="alert">{error}</p>}
      {checkingAccess ? (
        <p className="loading" role="status">Checking your account…</p>
      ) : error ? null : (
        <AvailableProducts onRegister={handleRegistration} />
      )}
    </SignedInShell>
  );
}
