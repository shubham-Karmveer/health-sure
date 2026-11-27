import Link from "next/link";

export default function SignupPage() {
  return (
    <main className="auth-page">
      <Link className="auth-brand" href="/">
        <span className="brand-mark" aria-hidden="true">H</span>
        HealthDekho
      </Link>
      <section className="auth-card" aria-labelledby="signup-title">
        <p className="eyebrow">Join HealthDekho</p>
        <h1 id="signup-title">Create your account</h1>
        <p className="muted">
          We’re preparing customer registration. Account creation is not available yet.
        </p>
        <div className="notice" role="status">
          You can sign in with an account that has already been set up. No information entered here
          will be submitted or stored.
        </div>
        <form className="auth-form" aria-label="Sign-up preview">
          <label htmlFor="signup-name">Full name</label>
          <input id="signup-name" name="name" autoComplete="name" disabled />
          <label htmlFor="signup-email">Email</label>
          <input id="signup-email" name="email" type="email" autoComplete="email" disabled />
          <label htmlFor="signup-password">Password</label>
          <input id="signup-password" name="password" type="password" autoComplete="new-password" disabled />
          <button className="button" type="button" disabled>
            Sign-up is not available yet
          </button>
        </form>
        <p className="auth-footer">
          Already have an account? <Link className="text-link" href="/login">Sign in</Link>
        </p>
      </section>
    </main>
  );
}
