"use client";

import { useRouter, useSearchParams } from "next/navigation";
import PolicyPayments from "@/features/payments/PolicyPayments";
import PolicyNumberLookup from "@/features/payments/PolicyNumberLookup";

export default function PolicyDetails() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const policyNumber = searchParams.get("policyNumber") ?? "";

  return (
    <div className="payments-page">
      {policyNumber ? (
        <PolicyPayments
          policyNumber={policyNumber}
          onClose={() => router.push("/dashboard")}
        />
      ) : (
        <PolicyNumberLookup />
      )}
    </div>
  );
}
