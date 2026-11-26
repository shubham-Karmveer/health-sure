import { Suspense } from "react";
import PolicyDetails from "@/features/policies/PolicyDetails";

export default function PolicyPage() {
  return (
    <Suspense fallback={<main className="shell"><p className="loading">Loading policy…</p></main>}>
      <PolicyDetails />
    </Suspense>
  );
}
