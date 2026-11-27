"use client";

import { FormEvent, useState } from "react";
import Link from "next/link";
import {
  getApiErrorMessage,
  PageResponse,
  PolicySearchFilters,
  PolicySearchResult,
  searchCustomerPolicies,
} from "@/lib/api/healthdekho-api";

const EMPTY_FILTERS: PolicySearchFilters = {
  policyNumber: "",
  customerName: "",
  dateOfBirth: "",
};

export default function PolicySearch() {
  const [filters, setFilters] = useState<PolicySearchFilters>(EMPTY_FILTERS);
  const [activeFilters, setActiveFilters] = useState<PolicySearchFilters | null>(null);
  const [resultsPage, setResultsPage] = useState<PageResponse<PolicySearchResult> | null>(null);
  const [searching, setSearching] = useState(false);
  const [error, setError] = useState("");

  async function runSearch(searchFilters: PolicySearchFilters, page: number) {
    setSearching(true);
    setError("");
    try {
      setResultsPage(await searchCustomerPolicies(searchFilters, page));
    } catch (requestError: unknown) {
      setResultsPage(null);
      setError(getApiErrorMessage(requestError));
    } finally {
      setSearching(false);
    }
  }

  function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const normalizedFilters = {
      policyNumber: filters.policyNumber.trim(),
      customerName: filters.customerName.trim(),
      dateOfBirth: filters.dateOfBirth,
    };
    const hasPolicyNumber = Boolean(normalizedFilters.policyNumber);
    const hasCustomerName = Boolean(normalizedFilters.customerName);
    const hasDateOfBirth = Boolean(normalizedFilters.dateOfBirth);
    const supportedSearch =
      (hasPolicyNumber && !hasCustomerName && !hasDateOfBirth)
      || (!hasPolicyNumber && hasCustomerName && !hasDateOfBirth)
      || (hasPolicyNumber && !hasCustomerName && hasDateOfBirth);

    if (!supportedSearch) {
      setResultsPage(null);
      setError(
        "Search by policy identifier, customer name, or policy identifier with date of birth."
      );
      return;
    }

    setActiveFilters(normalizedFilters);
    void runSearch(normalizedFilters, 0);
  }

  function clearSearch() {
    setFilters(EMPTY_FILTERS);
    setActiveFilters(null);
    setResultsPage(null);
    setError("");
  }

  return (
    <section id="policy-search" className="panel policy-search" aria-labelledby="policy-search-title">
      <div className="section-heading">
        <div>
          <p className="eyebrow">Your account</p>
          <h2 id="policy-search-title">Search your policies</h2>
        </div>
      </div>
      <p className="muted">
        Search by policy identifier, partial customer name, or policy identifier and date of birth.
        Results only include policies owned by your account.
      </p>
      <form className="policy-search-form" onSubmit={handleSearch}>
        <label htmlFor="policy-search-number">
          Policy identifier
          <input
            id="policy-search-number"
            value={filters.policyNumber}
            maxLength={50}
            onChange={(event) => setFilters({ ...filters, policyNumber: event.target.value })}
          />
        </label>
        <label htmlFor="policy-search-customer">
          Customer name
          <input
            id="policy-search-customer"
            value={filters.customerName}
            maxLength={120}
            onChange={(event) => setFilters({ ...filters, customerName: event.target.value })}
          />
        </label>
        <label htmlFor="policy-search-dob">
          Date of birth
          <input
            id="policy-search-dob"
            type="date"
            value={filters.dateOfBirth}
            onChange={(event) => setFilters({ ...filters, dateOfBirth: event.target.value })}
          />
        </label>
        <div className="search-actions">
          <button className="button" type="submit" disabled={searching}>
            {searching ? "Searching…" : "Search policies"}
          </button>
          {activeFilters && (
            <button className="button button-secondary" type="button" onClick={clearSearch}>
              Clear search
            </button>
          )}
        </div>
      </form>

      {error && <p className="alert" role="alert">{error}</p>}
      {searching && <p className="loading" role="status">Searching your policies…</p>}
      {!resultsPage && !searching && !error && (
        <p className="muted">Enter a supported search criterion to find your policies.</p>
      )}
      {resultsPage && resultsPage.content.length === 0 && (
        <div className="empty-state">
          <h3>No matching policies</h3>
          <p>Try changing the search criteria.</p>
        </div>
      )}
      {resultsPage && resultsPage.content.length > 0 && (
        <>
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>Policy identifier</th>
                  <th>Customer name</th>
                  <th>Date of birth</th>
                  <th>Policy name</th>
                  <th>Policy type</th>
                  <th>Start date</th>
                  <th>Maturity date</th>
                  <th>Total premium payable</th>
                  <th>Premium payment status</th>
                  <th>Total premium paid</th>
                  <th>Remaining premium</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {resultsPage.content.map((policy) => (
                  <tr key={policy.policyNumber}>
                    <td>{policy.policyNumber}</td>
                    <td>{policy.customerName}</td>
                    <td>{policy.dateOfBirth}</td>
                    <td>{policy.policyName}</td>
                    <td>{policy.policyType}</td>
                    <td>{policy.startDate}</td>
                    <td>{policy.maturityDate}</td>
                    <td>{formatMoney(policy.totalPremiumPayable)}</td>
                    <td><span className="status">{policy.premiumPaymentStatus}</span></td>
                    <td>{formatMoney(policy.totalPremiumPaid)}</td>
                    <td>{formatMoney(policy.remainingPremium)}</td>
                    <td>
                      <Link
                        className="button button-secondary"
                        href={`/policy?policyNumber=${encodeURIComponent(policy.policyNumber)}`}
                      >
                        Manage payments
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {resultsPage.totalPages > 1 && (
            <div className="search-actions">
              <button
                className="button button-secondary"
                type="button"
                disabled={searching || resultsPage.page === 0}
                onClick={() => {
                  if (activeFilters) {
                    void runSearch(activeFilters, resultsPage.page - 1);
                  }
                }}
              >
                Previous
              </button>
              <span aria-live="polite">
                Page {resultsPage.page + 1} of {resultsPage.totalPages}
              </span>
              <button
                className="button button-secondary"
                type="button"
                disabled={searching || resultsPage.last}
                onClick={() => {
                  if (activeFilters) {
                    void runSearch(activeFilters, resultsPage.page + 1);
                  }
                }}
              >
                Next
              </button>
            </div>
          )}
        </>
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
