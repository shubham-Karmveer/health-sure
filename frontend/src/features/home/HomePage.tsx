import Link from "next/link";

export default function HomePage() {
  return (
    <main className="shell home-shell">
      <header className="topbar">
        <div>
          <span className="eyebrow">Insurance, made clearer</span>
          <h1>HealthDekho</h1>
        </div>
        <nav className="home-nav" aria-label="Main navigation">
          <Link className="text-link" href="/login">Sign in</Link>
          <Link className="button" href="/dashboard">Customer dashboard</Link>
        </nav>
      </header>
      <section className="home-hero">
        <p className="eyebrow">Understand your cover</p>
        <h2>Insurance decisions, made simpler.</h2>
        <p>
          Explore available products, manage your policy schedule, and keep track of premium
          payments in one place.
        </p>
        <div className="home-actions">
          <Link className="button" href="/login">Sign in to continue</Link>
          <Link className="text-link" href="/dashboard">Go to dashboard</Link>
        </div>
      </section>
    </main>
  );
}
