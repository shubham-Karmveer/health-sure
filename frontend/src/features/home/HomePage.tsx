import Link from "next/link";

const highlights = [
  {
    title: "Keep your cover together",
    description: "See your registered policies, key dates, and payment actions in one place.",
  },
  {
    title: "Compare available plans",
    description: "Browse the active products available to your account before registering.",
  },
  {
    title: "Get a useful starting point",
    description: "Explore AI-assisted suggestions grounded in eligible products from the catalog.",
  },
];

export default function HomePage() {
  return (
    <main className="marketing-page">
      <header className="marketing-header">
        <Link className="auth-brand" href="/" aria-label="HealthDekho home">
          <span className="brand-mark" aria-hidden="true">H</span>
          HealthDekho
        </Link>
        <nav className="marketing-nav" aria-label="Main navigation">
          <a className="text-link" href="#how-it-works">How it works</a>
          <Link className="text-link" href="/login">Sign in</Link>
          <Link className="button" href="/signup">Get started</Link>
        </nav>
      </header>

      <section className="marketing-hero" aria-labelledby="hero-title">
        <div className="marketing-copy">
          <p className="eyebrow">Insurance, made clearer</p>
          <h1 id="hero-title">Make confident choices about your cover.</h1>
          <p className="marketing-lede">
            Explore available policies, manage the ones you own, and get recommendations based on
            your profile—all from one simple customer space.
          </p>
          <div className="marketing-actions">
            <Link className="button" href="/login">Sign in to your account</Link>
            <Link className="button button-outline" href="/signup">Create an account</Link>
          </div>
          <p className="marketing-footnote">
            Account creation is not available yet. You can still sign in with an existing account.
          </p>
        </div>
        <div className="hero-illustration" aria-label="Illustration of protected insurance coverage" role="img">
          <div className="orbit orbit-one" />
          <div className="orbit orbit-two" />
          <div className="shield-illustration"><span aria-hidden="true">✓</span></div>
          <div className="floating-tile tile-policy"><span aria-hidden="true">▣</span><span>My cover</span></div>
          <div className="floating-tile tile-plans"><span aria-hidden="true">⌕</span><span>Explore</span></div>
          <div className="floating-tile tile-advice"><span aria-hidden="true">✦</span><span>Guidance</span></div>
        </div>
      </section>

      <section className="marketing-highlights" id="how-it-works" aria-label="What you can do">
        {highlights.map((highlight, index) => (
          <article className="highlight-card" key={highlight.title}>
            <span className="highlight-number">0{index + 1}</span>
            <h2>{highlight.title}</h2>
            <p>{highlight.description}</p>
          </article>
        ))}
      </section>
      <footer className="marketing-footer">
        <span>HealthDekho</span>
        <span>Clear information for better-informed insurance decisions.</span>
      </footer>
    </main>
  );
}
