"use client";

import { FormEvent, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { getApiErrorMessage, signIn } from "@/lib/api/healthdekho-api";

export default function LoginForm() {
  const router = useRouter();
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    setSubmitting(true);
    setError("");
    try {
      await signIn(String(formData.get("email")), String(formData.get("password")));
      router.replace("/dashboard");
    } catch (requestError) {
      setError(getApiErrorMessage(requestError));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="shell">
      <header className="topbar">
        <Link className="text-link" href="/">HealthDekho</Link>
        <Link className="text-link" href="/">Home</Link>
      </header>
      <section className="panel sign-in-panel" aria-labelledby="sign-in-title">
        <p className="eyebrow">Customer access</p>
        <h1 id="sign-in-title">Sign in to your account</h1>
        <p className="muted">Use your HealthDekho customer credentials to continue.</p>
        {error && <p className="alert" role="alert">{error}</p>}
        <form onSubmit={(event) => void handleSubmit(event)}>
          <label htmlFor="email">Email</label>
          <input id="email" name="email" type="email" autoComplete="username" required />
          <label htmlFor="password">Password</label>
          <input id="password" name="password" type="password" autoComplete="current-password" required />
          <button className="button" type="submit" disabled={submitting}>
            {submitting ? "Signing in…" : "Sign in"}
          </button>
        </form>
      </section>
    </main>
  );
}
