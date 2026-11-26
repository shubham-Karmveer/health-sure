"use client";

import { useEffect, useState } from "react";
import {
  AvailableProduct,
  fetchAvailableProducts,
  getApiErrorMessage,
} from "@/lib/api/healthdekho-api";

interface AvailableProductsProps {
  onRegister: (productCode: string) => void;
}

export default function AvailableProducts({ onRegister }: AvailableProductsProps) {
  const [products, setProducts] = useState<AvailableProduct[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    fetchAvailableProducts()
      .then((availableProducts) => {
        if (active) {
          setProducts(availableProducts);
        }
      })
      .catch((requestError: unknown) => {
        if (active) {
          setError(getApiErrorMessage(requestError));
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });
    return () => {
      active = false;
    };
  }, []);

  if (loading) {
    return <p className="loading" role="status">Loading available products…</p>;
  }

  return (
    <section className="products-section" aria-labelledby="available-products-title">
      <div className="section-heading">
        <div>
          <p className="eyebrow">Explore the catalog</p>
          <h2 id="available-products-title">Available products</h2>
        </div>
        <span className="count">{products.length}</span>
      </div>
      {error && <p className="alert" role="alert">{error}</p>}
      {products.length === 0 ? (
        <div className="empty-state">
          <h3>No products available</h3>
          <p>There are no products to show right now.</p>
        </div>
      ) : (
        <div className="product-grid">
          {products.map((product) => (
            <article className="panel product-card" key={product.productCode}>
              <p className="eyebrow">{product.policyType} · {product.productCode}</p>
              <h3>{product.policyName}</h3>
              <dl className="product-facts">
                <div><dt>Term</dt><dd>{product.termMonths} months</dd></div>
                <div><dt>Premium</dt><dd>{formatMoney(product.premiumAmount)} / {product.premiumFrequency.toLowerCase()}</dd></div>
                <div><dt>Coverage</dt><dd>{formatMoney(product.sumInsured)}</dd></div>
                <div><dt>Entry age</dt><dd>{product.minEntryAge}–{product.maxEntryAge} years</dd></div>
              </dl>
              <button
                className="button"
                type="button"
                aria-label={`Register ${product.policyName}`}
                onClick={() => onRegister(product.productCode)}
              >
                Register
              </button>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}

function formatMoney(amount: number): string {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 2,
  }).format(amount);
}
