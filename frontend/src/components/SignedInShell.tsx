"use client";

import type { ReactNode } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useState } from "react";
import { getApiErrorMessage, signOut } from "@/lib/api/healthdekho-api";

const navigation = [
  { label: "Dashboard", href: "/dashboard" },
  { label: "Explore plans", href: "/products" },
  { label: "AI recommendations", href: "/recommendations" },
  { label: "Search policies", href: "/dashboard#policy-search" },
  { label: "Payments", href: "/policy" },
  {label: "Profile", href: "/dashboard" }
];

interface SignedInShellProps {
  pageTitle: string;
  children: ReactNode;
}

export default function SignedInShell({ pageTitle, children }: SignedInShellProps) {
  const pathname = usePathname();
  const router = useRouter();
  const [signingOut, setSigningOut] = useState(false);
  const [error, setError] = useState("");

  async function handleSignOut() {
    setSigningOut(true);
    setError("");
    try {
      await signOut();
      router.replace("/login");
    } catch (requestError: unknown) {
      setError(getApiErrorMessage(requestError));
    } finally {
      setSigningOut(false);
    }
  }

  return (
    <div className="app-frame">
      <aside className="app-sidebar">
        <Link className="app-brand" href="/dashboard" aria-label="HealthDekho dashboard">
          <span className="brand-mark" aria-hidden="true">H</span>
          <span>HealthDekho</span>
        </Link>
        <nav className="app-navigation" aria-label="Customer navigation">
          {navigation.map((item) => {
            const hrefPath = item.href.split("#")[0];
            const active = !item.href.includes("#") && pathname === hrefPath;
            return (
              <Link
                key={item.label}
                className={`app-navigation-link${active ? " is-active" : ""}`}
                href={item.href}
                aria-current={active ? "page" : undefined}
              >
                {item.label}
              </Link>
            );
          })}
        </nav>
        <button
          className="sidebar-signout"
          type="button"
          disabled={signingOut}
          onClick={() => void handleSignOut()}
        >
          {signingOut ? "Signing out…" : "Sign out"}
        </button>
      </aside>
      <div className="app-main">
        <header className="app-topbar">
          <div>
            <p className="eyebrow">HealthDekho customer space</p>
            <h1>{pageTitle}</h1>
          </div>
          {/* <Link className="app-topbar-home " href="/recommendations">AI Recommendations</Link> */}
        </header>
        {error && <p className="alert app-shell-alert" role="alert">{error}</p>}
        <div className="app-content">{children}</div>
      </div>
    </div>
  );
}
