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
