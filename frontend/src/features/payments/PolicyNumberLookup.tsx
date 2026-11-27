"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";

export default function PolicyNumberLookup() {
  const router = useRouter();
  const [policyNumber, setPolicyNumber] = useState("");
  const [error, setError] = useState("");

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const normalizedPolicyNumber = policyNumber.trim();
    if (!normalizedPolicyNumber) {
      setError("Enter a policy identifier to continue.");
      return;
    }
    router.push(`/policy?policyNumber=${encodeURIComponent(normalizedPolicyNumber)}`);
  }

  return (
    <section className="panel policy-lookup" aria-labelledby="policy-lookup-title">
      <h2 id="policy-lookup-title">Find a policy by number</h2>
      <p className="muted">Only policies owned by your account can be opened.</p>
      <form className="policy-lookup-form" onSubmit={handleSubmit}>
        <label htmlFor="policy-number-search">
          Policy identifier
          <input
            id="policy-number-search"
            value={policyNumber}
            maxLength={60}
            onChange={(event) => {
              setPolicyNumber(event.target.value);
              setError("");
            }}
          />
        </label>
        {error && <p className="alert" role="alert">{error}</p>}
        <button className="button" type="submit">Find policy</button>
      </form>
    </section>
  );
}
