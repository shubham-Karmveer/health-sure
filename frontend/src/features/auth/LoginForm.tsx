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
    <main className="auth-page">
      <Link className="auth-brand" href="/">
        <span className="brand-mark" aria-hidden="true">H</span>
        HealthDekho
      </Link>
      <section className="auth-card" aria-labelledby="sign-in-title">
        <p className="eyebrow">Customer access</p>
        <h1 id="sign-in-title">Welcome back</h1>
        <p className="muted">Sign in to view and manage your policies.</p>
        {error && <p className="alert" role="alert">{error}</p>}
        <form className="auth-form" onSubmit={(event) => void handleSubmit(event)}>
          <label htmlFor="email">Email</label>
          <input id="email" name="email" type="email" autoComplete="username" required />
          <label htmlFor="password">Password</label>
          <input id="password" name="password" type="password" autoComplete="current-password" required />
          <button className="button" type="submit" disabled={submitting}>
            {submitting ? "Signing in…" : "Sign in"}
          </button>
        </form>
        <p className="auth-footer">
          New to HealthDekho? <Link className="text-link" href="/signup">Create an account</Link>
        </p>
        <Link className="auth-back-link" href="/">Back to home</Link>
      </section>
    </main>
  );
}
