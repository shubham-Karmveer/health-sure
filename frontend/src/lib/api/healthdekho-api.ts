import axios from "axios";
import apiClient from "./client";

export interface CustomerProfile {
  customerId: string;
  email: string;
  fullName: string;
  dateOfBirth: string;
  phone: string | null;
}

export interface PolicySummary {
  policyNumber: string;
  policyName: string;
  policyType: string;
  termMonths: number;
  startDate: string;
  maturityDate: string;
  premiumFrequency: string;
  premiumAmount: number;
  sumInsured: number;
  status: string;
}

export interface DashboardData {
  customer: CustomerProfile;
  policies: PolicySummary[];
}

export interface AvailableProduct {
  productId: string;
  productCode: string;
  policyName: string;
  policyType: string;
  termMonths: number;
  premiumFrequency: string;
  premiumAmount: number;
  sumInsured: number;
  minEntryAge: number;
  maxEntryAge: number;
}

export interface PolicyRegistrationResult {
  policyNumber: string;
  confirmation: string;
  maturityDate: string;
  totalPremiumPayable: number;
}

export interface PolicyDetails {
  policyNumber: string;
  customerName: string;
  dateOfBirth: string;
  phone: string | null;
  policyName: string;
  policyType: string;
  termMonths: number;
  startDate: string;
  maturityDate: string;
  premiumFrequency: string;
  premiumAmount: number;
  sumInsured: number;
  totalPremiumPayable: number;
  totalPremiumPaid: number;
  remainingPremium: number;
  totalPenaltyAssessed: number;
  totalPenaltyPaid: number;
  remainingPenalty: number;
  remainingBalance: number;
  estimatedPenalty: number;
  estimatedBalance: number;
  status: string;
}

export interface PolicyInstallment {
  installmentNumber: number;
  dueDate: string;
  gracePeriodEndDate: string;
  scheduledAmount: number;
  paidAmount: number;
  remainingPremium: number;
  penaltyAmount: number;
  paidPenaltyAmount: number;
  remainingPenalty: number;
  estimatedPenalty: number;
  remainingBalance: number;
  estimatedBalance: number;
  paymentStatus: string;
}

export interface PaymentHistoryItem {
  transactionReference: string;
  paymentDate: string;
  installmentNumber: number;
  amountPaid: number;
  premiumAmount: number;
  penaltyAmount: number;
  paymentMethod: string;
}

export interface PaymentBatchResult {
  policyNumber: string;
  confirmation: string;
  totalPremiumPayable: number;
  totalPremiumPaid: number;
  remainingPremium: number;
  totalPenaltyAssessed: number;
  totalPenaltyPaid: number;
  remainingPenalty: number;
  remainingBalance: number;
  payments: Array<{
    transactionReference: string;
    installmentNumber: number;
    paymentDate: string;
    amountPaid: number;
    premiumAmount: number;
    penaltyAmount: number;
    paymentMethod: string;
  }>;
}

interface ApiError {
  message?: string;
}

interface CsrfResponse {
  token: string;
  headerName: string;
}

async function prepareCsrfToken(): Promise<void> {
  await apiClient.get<CsrfResponse>("/api/auth/csrf");
}

export async function fetchDashboard(): Promise<DashboardData> {
  const [customer, policies] = await Promise.all([
    apiClient.get<CustomerProfile>("/api/customers/me"),
    apiClient.get<PolicySummary[]>("/api/me/policies"),
  ]);
  return { customer: customer.data, policies: policies.data };
}

export async function fetchAvailableProducts(): Promise<AvailableProduct[]> {
  const response = await apiClient.get<AvailableProduct[]>("/api/products/available");
  return response.data;
}

export async function registerPolicy(
  productCode: string,
  startDate: string
): Promise<PolicyRegistrationResult> {
  const response = await apiClient.post<PolicyRegistrationResult>("/api/policies", {
    productCode,
    startDate,
  });
  return response.data;
}

export async function fetchPolicyDetails(policyNumber: string): Promise<PolicyDetails> {
  const response = await apiClient.get<PolicyDetails>(
    `/api/policies/${encodeURIComponent(policyNumber)}`
  );
  return response.data;
}

export async function fetchPolicyInstallments(policyNumber: string): Promise<PolicyInstallment[]> {
  const response = await apiClient.get<PolicyInstallment[]>(
    `/api/policies/${encodeURIComponent(policyNumber)}/installments`
  );
  return response.data;
}

export async function fetchPaymentHistory(policyNumber: string): Promise<PaymentHistoryItem[]> {
  const response = await apiClient.get<PaymentHistoryItem[]>(
    `/api/policies/${encodeURIComponent(policyNumber)}/payments`
  );
  return response.data;
}

export async function recordPolicyPayments(
  policyNumber: string,
  paymentMethod: string,
  payments: Array<{ installmentNumber: number; amountPaid: number }>
): Promise<PaymentBatchResult> {
  await prepareCsrfToken();
  const response = await apiClient.post<PaymentBatchResult>(
    `/api/policies/${encodeURIComponent(policyNumber)}/payments`,
    { paymentMethod, payments },
    { headers: { "Idempotency-Key": crypto.randomUUID() } }
  );
  return response.data;
}

export async function signIn(email: string, password: string): Promise<void> {
  await prepareCsrfToken();
  await apiClient.post("/api/auth/login", { email, password });
}

export async function signOut(): Promise<void> {
  await prepareCsrfToken();
  await apiClient.post("/api/auth/logout");
}

export function getApiErrorMessage(error: unknown): string {
  if (axios.isAxiosError<ApiError>(error)) {
    return error.response?.data.message ?? "The request could not be completed.";
  }
  return "The request could not be completed.";
}

export function isUnauthorized(error: unknown): boolean {
  return axios.isAxiosError(error) && error.response?.status === 401;
}
