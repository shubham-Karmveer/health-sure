"use client";

import { FormEvent, useEffect, useState } from "react";
import {
  AvailableProduct,
  fetchAvailableProducts,
  getApiErrorMessage,
  PageResponse,
  SearchFilters,
  searchAvailableProducts,
} from "@/lib/api/healthdekho-api";

interface AvailableProductsProps {
  onRegister: (productCode: string) => void;
}

export default function AvailableProducts({ onRegister }: AvailableProductsProps) {
  const [products, setProducts] = useState<AvailableProduct[]>([]);
  const [filters, setFilters] = useState<SearchFilters>({
    productCode: "",
    policyName: "",
    policyType: "",
  });
  const [resultsPage, setResultsPage] = useState<PageResponse<AvailableProduct> | null>(null);
  const [loading, setLoading] = useState(true);
  const [searching, setSearching] = useState(false);
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

  async function runSearch(searchFilters: SearchFilters, page: number) {
    setSearching(true);
    setError("");
    try {
      const results = await searchAvailableProducts(searchFilters, page);
      setResultsPage(results);
      setProducts(results.content);
    } catch (requestError: unknown) {
      setError(getApiErrorMessage(requestError));
    } finally {
      setSearching(false);
    }
  }

  function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!filters.productCode.trim() && !filters.policyName.trim() && !filters.policyType.trim()) {
      setError("Enter a product code, policy name, or policy type to search.");
      return;
    }
    void runSearch(filters, 0);
  }

  async function clearSearch() {
    setFilters({ productCode: "", policyName: "", policyType: "" });
    setResultsPage(null);
    setError("");
    setLoading(true);
    try {
      setProducts(await fetchAvailableProducts());
    } catch (requestError: unknown) {
      setError(getApiErrorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }

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
        <span className="count">{resultsPage?.totalElements ?? products.length}</span>
      </div>
      {error && <p className="alert" role="alert">{error}</p>}
      <form className="product-search-form" onSubmit={handleSearch}>
        <label>
          Product code
          <input
            value={filters.productCode}
            onChange={(event) => setFilters({ ...filters, productCode: event.target.value })}
            maxLength={40}
          />
        </label>
        <label>
          Policy name
          <input
            value={filters.policyName}
            onChange={(event) => setFilters({ ...filters, policyName: event.target.value })}
            maxLength={160}
          />
        </label>
        <label>
          Policy type
          <input
            value={filters.policyType}
            onChange={(event) => setFilters({ ...filters, policyType: event.target.value })}
            maxLength={40}
          />
        </label>
        <div className="search-actions">
          <button className="button" type="submit" disabled={searching}>
            {searching ? "Searching…" : "Search products"}
          </button>
          {resultsPage && (
            <button className="button button-secondary" type="button" onClick={() => void clearSearch()}>
              Clear search
            </button>
          )}
        </div>
      </form>
      {products.length === 0 ? (
        <div className="empty-state">
          <h3>{resultsPage ? "No matching products" : "No products available"}</h3>
          <p>{resultsPage ? "Try changing the product search criteria." : "There are no products to show right now."}</p>
        </div>
      ) : (
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>Product code</th>
                <th>Policy name</th>
                <th>Policy type</th>
                <th>Term</th>
                <th>Sum insured</th>
                <th>Premium frequency</th>
                <th>Premium amount</th>
                <th>Entry age</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {products.map((product) => (
                <tr key={product.productCode}>
                  <td>{product.productCode}</td>
                  <td>{product.policyName}</td>
                  <td>{product.policyType}</td>
                  <td>{product.termMonths} months</td>
                  <td>{formatMoney(product.sumInsured)}</td>
                  <td>{product.premiumFrequency}</td>
                  <td>{formatMoney(product.premiumAmount)}</td>
                  <td>{product.minEntryAge}–{product.maxEntryAge} years</td>
                  <td>
                    <button
                      className="button"
                      type="button"
                      aria-label={`Register ${product.policyName}`}
                      onClick={() => onRegister(product.productCode)}
                    >
                      Register
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {resultsPage && resultsPage.totalPages > 1 && (
        <div className="search-actions">
          <button
            className="button button-secondary"
            type="button"
            disabled={searching || resultsPage.page === 0}
            onClick={() => void runSearch(filters, resultsPage.page - 1)}
          >
            Previous
          </button>
          <span aria-live="polite">Page {resultsPage.page + 1} of {resultsPage.totalPages}</span>
          <button
            className="button button-secondary"
            type="button"
            disabled={searching || resultsPage.last}
            onClick={() => void runSearch(filters, resultsPage.page + 1)}
          >
            Next
          </button>
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
