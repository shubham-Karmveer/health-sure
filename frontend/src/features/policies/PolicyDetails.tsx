"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import PolicyPayments from "@/features/payments/PolicyPayments";
import PolicyNumberLookup from "@/features/payments/PolicyNumberLookup";

export default function PolicyDetails() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const policyNumber = searchParams.get("policyNumber") ?? "";

  return (
    <main className="shell">
      <header className="topbar">
        <div>
          <Link className="text-link" href="/dashboard">HealthDekho</Link>
          <p className="eyebrow">Policy details</p>
        </div>
        <Link className="text-link" href="/dashboard">Back to dashboard</Link>
      </header>
      {policyNumber ? (
        <PolicyPayments
          policyNumber={policyNumber}
          onClose={() => router.push("/dashboard")}
        />
      ) : (
        <PolicyNumberLookup />
      )}
    </main>
  );
}
