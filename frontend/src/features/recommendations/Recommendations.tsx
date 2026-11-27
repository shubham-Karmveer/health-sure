"use client";

import { FormEvent, useState } from "react";
import {
  createRecommendation,
  getApiErrorMessage,
  RecommendationResult,
} from "@/lib/api/healthdekho-api";

interface RecommendationsProps {
  dateOfBirth: string;
  onRegister: (productCode: string) => void;
}

export default function Recommendations({ dateOfBirth, onRegister }: RecommendationsProps) {
  const [insuranceGoals, setInsuranceGoals] = useState("");
  const [preferredTerm, setPreferredTerm] = useState("");
  const [preferredPolicyType, setPreferredPolicyType] = useState("");
  const [result, setResult] = useState<RecommendationResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError("");
    setResult(null);
    try {
      setResult(await createRecommendation({
        dateOfBirth,
        insuranceGoals: insuranceGoals.trim() || undefined,
        preferredTerm: preferredTerm.trim() || undefined,
        preferredPolicyType: preferredPolicyType || undefined,
      }));
    } catch (requestError: unknown) {
      setError(getApiErrorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="panel recommendations-section" aria-labelledby="recommendations-title">
      <div className="section-heading">
        <div>
          <p className="eyebrow">Personalized discovery</p>
          <h2 id="recommendations-title">Get product recommendations</h2>
        </div>
      </div>
      <p className="muted">
        We use the date of birth saved in your account profile to check eligibility against active products.
      </p>
      <form className="recommendation-form" onSubmit={(event) => void handleSubmit(event)}>
        <label className="recommendation-field recommendation-field-wide" htmlFor="recommendation-goals">
          Insurance goals or preferences
          <textarea
            id="recommendation-goals"
            maxLength={2000}
            rows={3}
            value={insuranceGoals}
            onChange={(event) => setInsuranceGoals(event.target.value)}
          />
        </label>
        <label className="recommendation-field" htmlFor="recommendation-term">
          Preferred policy term
          <input
            id="recommendation-term"
            maxLength={100}
            placeholder="For example, 20 years"
            value={preferredTerm}
            onChange={(event) => setPreferredTerm(event.target.value)}
          />
        </label>
        <label className="recommendation-field" htmlFor="recommendation-type">
          Insurance type
          <select
            id="recommendation-type"
            value={preferredPolicyType}
            onChange={(event) => setPreferredPolicyType(event.target.value)}
          >
            <option value="">Any type</option>
            <option value="TERM">Term</option>
            <option value="HEALTH">Health</option>
            <option value="SAVINGS">Savings</option>
          </select>
        </label>
        <div className="recommendation-form-actions">
          <button className="button" type="submit" disabled={loading}>
            {loading ? "Finding recommendations…" : "Get recommendations"}
          </button>
        </div>
      </form>

      {error && <p className="alert" role="alert">{error}</p>}
      {loading && <p className="loading" role="status">Finding eligible products…</p>}
      {result && (
        <div aria-live="polite">
          {result.recommendations.length === 0 ? (
            <div className="empty-state">
              <h3>No matching recommendations</h3>
              <p>No eligible active products matched this request.</p>
            </div>
          ) : (
            <>
              <p className="muted">
                Based on age {result.calculatedAge}. Recommendations are advisory and use eligible
                products from the catalog.
              </p>
              <div className="table-scroll">
                <table>
                  <thead>
                    <tr>
                      <th>Product code</th>
                      <th>Policy name</th>
                      <th>Type</th>
                      <th>Term</th>
                      <th>Sum insured</th>
                      <th>Premium frequency</th>
                      <th>Premium amount</th>
                      <th>Reason</th>
                      <th>Suitability</th>
                      <th>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {result.recommendations.map((recommendation) => (
                      <tr key={recommendation.productId}>
                        <td>{recommendation.productCode}</td>
                        <td>{recommendation.policyName}</td>
                        <td>{recommendation.policyType}</td>
                        <td>{recommendation.termMonths} months</td>
                        <td>{formatMoney(recommendation.sumInsured)}</td>
                        <td>{recommendation.premiumFrequency}</td>
                        <td>{formatMoney(recommendation.premiumAmount)}</td>
                        <td>{recommendation.recommendationReason}</td>
                        <td>{recommendation.suitability}</td>
                        <td>
                          <button
                            className="button"
                            type="button"
                            aria-label={`Register ${recommendation.policyName}`}
                            onClick={() => onRegister(recommendation.productCode)}
                          >
                            Register
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </>
          )}
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
