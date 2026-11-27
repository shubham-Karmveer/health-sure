import { Suspense } from "react";
import SignedInShell from "@/components/SignedInShell";
import PolicyDetails from "@/features/policies/PolicyDetails";

export default function PolicyPage() {
  return (
    <SignedInShell pageTitle="Payments and policy details">
      <Suspense fallback={<p className="loading">Loading policy…</p>}>
        <PolicyDetails />
      </Suspense>
    </SignedInShell>
  );
}
