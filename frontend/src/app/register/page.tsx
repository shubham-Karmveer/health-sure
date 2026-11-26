import { Suspense } from "react";
import PolicyRegistration from "@/features/policies/PolicyRegistration";

export default function RegisterPage() {
  return (
    <Suspense fallback={<main className="shell"><p className="loading">Loading registration…</p></main>}>
      <PolicyRegistration />
    </Suspense>
  );
}
