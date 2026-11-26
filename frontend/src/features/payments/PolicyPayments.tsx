"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import {
  fetchPaymentHistory,
  fetchPolicyDetails,
  fetchPolicyInstallments,
  getApiErrorMessage,
  isUnauthorized,
  PaymentHistoryItem,
  PolicyDetails,
  PolicyInstallment,
  recordPolicyPayments,
} from "@/lib/api/healthdekho-api";
import { useRouter } from "next/navigation";

interface PolicyPaymentsProps {
  policyNumber: string;
  onClose?: () => void;
  onPaymentsRecorded?: () => Promise<void>;
}

interface PolicySnapshot {
  details: PolicyDetails;
  installments: PolicyInstallment[];
  history: PaymentHistoryItem[];
}

export default function PolicyPayments({
  policyNumber,
  onClose,
  onPaymentsRecorded,
}: PolicyPaymentsProps) {
  const router = useRouter();
  const [details, setDetails] = useState<PolicyDetails | null>(null);
  const [installments, setInstallments] = useState<PolicyInstallment[]>([]);
  const [history, setHistory] = useState<PaymentHistoryItem[]>([]);
  const [selectedInstallments, setSelectedInstallments] = useState<number[]>([]);
  const [amounts, setAmounts] = useState<Record<number, string>>({});
  const [paymentMethod, setPaymentMethod] = useState("UPI");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const loadPolicy = useCallback(async (): Promise<PolicySnapshot> => {
    const [policyDetails, policyInstallments, paymentHistory] = await Promise.all([
      fetchPolicyDetails(policyNumber),
      fetchPolicyInstallments(policyNumber),
      fetchPaymentHistory(policyNumber),
    ]);
    return { details: policyDetails, installments: policyInstallments, history: paymentHistory };
  }, [policyNumber]);

  const showPolicy = useCallback((snapshot: PolicySnapshot) => {
    setDetails(snapshot.details);
    setInstallments(snapshot.installments);
    setHistory(snapshot.history);
    setSelectedInstallments([]);
    setAmounts(
      Object.fromEntries(
        snapshot.installments
          .filter((installment) => installment.remainingBalance > 0)
          .map((installment) => [
            installment.installmentNumber,
            installment.estimatedBalance.toFixed(2),
          ])
      )
    );
  }, []);

  useEffect(() => {
    let active = true;
    loadPolicy()
      .then((snapshot) => {
        if (active) {
          showPolicy(snapshot);
        }
      })
      .catch((requestError: unknown) => {
        if (active) {
          if (isUnauthorized(requestError)) {
            router.replace("/login");
          } else {
            setError(getApiErrorMessage(requestError));
          }
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
  }, [loadPolicy, router, showPolicy]);

  function toggleInstallment(installmentNumber: number) {
    setSelectedInstallments((selected) =>
      selected.includes(installmentNumber)
        ? selected.filter((number) => number !== installmentNumber)
        : [...selected, installmentNumber]
    );
  }

  async function handlePayment(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError("");
    setSuccess("");
    try {
      const result = await recordPolicyPayments(
        policyNumber,
        paymentMethod,
        selectedInstallments.map((installmentNumber) => ({
          installmentNumber,
          amountPaid: Number(amounts[installmentNumber]),
        }))
      );
      setSuccess(`${result.confirmation} ${result.payments.length} payment(s) recorded.`);
      showPolicy(await loadPolicy());
      await onPaymentsRecorded?.();
    } catch (requestError) {
      setError(getApiErrorMessage(requestError));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="panel payments-panel" aria-labelledby="policy-payments-title">
      <div className="section-heading">
        <div>
          <p className="eyebrow">Policy payment management</p>
          <h2 id="policy-payments-title">Policy {policyNumber}</h2>
        </div>
        {onClose && (
          <button className="button button-secondary" type="button" onClick={onClose}>
            Back to dashboard
          </button>
        )}
      </div>
      {error && <p className="alert" role="alert">{error}</p>}
      {success && <p className="success" role="status">{success}</p>}
      {loading ? (
        <p className="loading" role="status">Loading policy payment details…</p>
      ) : details ? (
        <>
          <dl className="payment-summary">
            <div><dt>Policy</dt><dd>{details.policyName}</dd></div>
            <div><dt>Total premium</dt><dd>{formatMoney(details.totalPremiumPayable)}</dd></div>
            <div><dt>Premium paid</dt><dd>{formatMoney(details.totalPremiumPaid)}</dd></div>
            <div><dt>Premium remaining</dt><dd>{formatMoney(details.remainingPremium)}</dd></div>
            <div><dt>Penalty outstanding</dt><dd>{formatMoney(details.remainingPenalty)}</dd></div>
            <div><dt>Total balance</dt><dd>{formatMoney(details.remainingBalance)}</dd></div>
            <div><dt>Estimated late penalty</dt><dd>{formatMoney(details.estimatedPenalty)}</dd></div>
            <div><dt>Estimated full amount due</dt><dd>{formatMoney(details.estimatedBalance)}</dd></div>
          </dl>

          <h3>Installments</h3>
          <form onSubmit={(event) => void handlePayment(event)}>
            <div className="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>Select</th>
                    <th>Installment</th>
                    <th>Due date</th>
                    <th>Grace ends</th>
                    <th>Remaining</th>
                    <th>Estimated full amount</th>
                    <th>Status</th>
                    <th>Amount to collect</th>
                  </tr>
                </thead>
                <tbody>
                  {installments.map((installment) => (
                    <tr key={installment.installmentNumber}>
                      <td>
                        <input
                          aria-label={`Select installment ${installment.installmentNumber}`}
                          type="checkbox"
                          disabled={installment.estimatedBalance <= 0 || submitting}
                          checked={selectedInstallments.includes(installment.installmentNumber)}
                          onChange={() => toggleInstallment(installment.installmentNumber)}
                        />
                      </td>
                      <td>{installment.installmentNumber}</td>
                      <td>{installment.dueDate}</td>
                      <td>{installment.gracePeriodEndDate}</td>
                      <td>{formatMoney(installment.remainingBalance)}</td>
                      <td>{formatMoney(installment.estimatedBalance)}</td>
                      <td><span className="status">{installment.paymentStatus}</span></td>
                      <td>
                        <input
                          aria-label={`Amount for installment ${installment.installmentNumber}`}
                          type="number"
                          min="0.01"
                          max={installment.estimatedBalance}
                          step="0.01"
                          required={selectedInstallments.includes(installment.installmentNumber)}
                          value={amounts[installment.installmentNumber] ?? ""}
                          disabled={installment.remainingBalance <= 0 || submitting}
                          onChange={(event) => setAmounts((current) => ({
                            ...current,
                            [installment.installmentNumber]: event.target.value,
                          }))}
                        />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <label htmlFor="payment-method">Payment method</label>
            <input
              id="payment-method"
              maxLength={40}
              required
              value={paymentMethod}
              disabled={submitting}
              onChange={(event) => setPaymentMethod(event.target.value)}
            />
            <button
              className="button"
              type="submit"
              disabled={submitting || selectedInstallments.length === 0 || paymentMethod.trim() === ""}
            >
              {submitting ? "Recording payment…" : "Record selected payments"}
            </button>
          </form>

          <h3>Payment history</h3>
          {history.length === 0 ? (
            <p className="muted">No payments have been recorded for this policy.</p>
          ) : (
            <div className="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>Transaction</th>
                    <th>Date</th>
                    <th>Installment</th>
                    <th>Amount</th>
                    <th>Penalty</th>
                    <th>Method</th>
                  </tr>
                </thead>
                <tbody>
                  {history.map((payment) => (
                    <tr key={payment.transactionReference}>
                      <td>{payment.transactionReference}</td>
                      <td>{new Date(payment.paymentDate).toLocaleString()}</td>
                      <td>{payment.installmentNumber}</td>
                      <td>{formatMoney(payment.amountPaid)}</td>
                      <td>{formatMoney(payment.penaltyAmount)}</td>
                      <td>{payment.paymentMethod}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      ) : (
        <p className="muted">Policy details are unavailable.</p>
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
