import {
  Account,
  AccountStatus,
  AnalyticsData,
  Customer,
  DashboardSummary,
  DatabaseStatus,
  HealthInfo,
  Transaction,
} from '../types/banking';

const API_BASE = '/api';

class ApiError extends Error {
  code?: string;
  statusCode: number;

  constructor(message: string, statusCode: number, code?: string) {
    super(message);
    this.name = 'ApiError';
    this.statusCode = statusCode;
    this.code = code;
  }
}

async function request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const url = `${API_BASE}${endpoint}`;
  const headers = {
    'Content-Type': 'application/json',
    Accept: 'application/json',
    ...options.headers,
  };

  const response = await fetch(url, { ...options, headers });

  let data;
  const contentType = response.headers.get('content-type');
  if (contentType && contentType.includes('application/json')) {
    data = await response.json();
  } else {
    data = await response.text();
  }

  if (!response.ok) {
    const errorMsg = data?.error || data?.message || `Request failed with status ${response.status}`;
    const errorCode = data?.code;
    throw new ApiError(errorMsg, response.status, errorCode);
  }

  return data as T;
}

export const api = {
  // System Health
  getHealth: () => request<HealthInfo>('/health'),

  // Dashboard & Analytics
  getDashboardSummary: () => request<DashboardSummary>('/dashboard/summary'),
  getAnalytics: () => request<AnalyticsData>('/analytics'),

  // Customers
  getCustomers: (q?: string) => {
    const query = q ? `?q=${encodeURIComponent(q)}` : '';
    return request<Customer[]>(`/customers${query}`);
  },
  getCustomer: (id: string) => request<Customer>(`/customers/${id}`),
  getCustomerAccounts: (id: string) => request<Account[]>(`/customers/${id}/accounts`),
  createCustomer: (data: {
    firstName: string;
    lastName: string;
    email: string;
    phone?: string;
    address?: string;
  }) =>
    request<Customer>('/customers', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
  updateCustomer: (
    id: string,
    data: { firstName?: string; lastName?: string; phone?: string; address?: string }
  ) =>
    request<Customer>(`/customers/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),

  // Accounts
  getAccounts: (params?: { type?: string; status?: string; q?: string }) => {
    const qParts = [];
    if (params?.type) qParts.push(`type=${encodeURIComponent(params.type)}`);
    if (params?.status) qParts.push(`status=${encodeURIComponent(params.status)}`);
    if (params?.q) qParts.push(`q=${encodeURIComponent(params.q)}`);
    const query = qParts.length ? `?${qParts.join('&')}` : '';
    return request<Account[]>(`/accounts${query}`);
  },
  getAccount: (accountNumber: string) => request<Account>(`/accounts/${accountNumber}`),
  createSavingsAccount: (data: {
    customerId: string;
    initialDeposit: number;
    interestRate?: number;
    minimumBalance?: number;
    maxWithdrawalLimit?: number;
  }) =>
    request<Account>('/accounts/savings', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
  createCurrentAccount: (data: {
    customerId: string;
    initialDeposit: number;
    overdraftLimit?: number;
    overdraftInterestRate?: number;
  }) =>
    request<Account>('/accounts/current', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
  updateAccountStatus: (accountNumber: string, status: AccountStatus) =>
    request<Account>(`/accounts/${accountNumber}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status }),
    }),
  getAccountTransactions: (accountNumber: string) =>
    request<Transaction[]>(`/accounts/${accountNumber}/transactions`),
  applyInterest: (accountNumber: string) =>
    request<Transaction>(`/accounts/${accountNumber}/apply-interest`, {
      method: 'POST',
    }),

  // Transactions
  getTransactions: (params?: {
    accountNumber?: string;
    type?: string;
    status?: string;
    limit?: number;
  }) => {
    const qParts = [];
    if (params?.accountNumber) qParts.push(`accountNumber=${encodeURIComponent(params.accountNumber)}`);
    if (params?.type) qParts.push(`type=${encodeURIComponent(params.type)}`);
    if (params?.status) qParts.push(`status=${encodeURIComponent(params.status)}`);
    if (params?.limit) qParts.push(`limit=${params.limit}`);
    const query = qParts.length ? `?${qParts.join('&')}` : '';
    return request<Transaction[]>(`/transactions${query}`);
  },
  getTransaction: (id: string) => request<Transaction>(`/transactions/${id}`),
  deposit: (data: { accountNumber: string; amount: number; remarks?: string }) =>
    request<Transaction>('/transactions/deposit', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
  withdraw: (data: { accountNumber: string; amount: number; remarks?: string }) =>
    request<Transaction>('/transactions/withdraw', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
  transfer: (data: {
    sourceAccountNumber: string;
    destinationAccountNumber: string;
    amount: number;
    remarks?: string;
  }) =>
    request<Transaction[]>('/transactions/transfer', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  // Database Management
  getDatabaseTables: () => request<DatabaseStatus>('/database/tables'),
  reseedDatabase: () =>
    request<{ success: boolean; message: string }>('/database/reseed', {
      method: 'POST',
    }),
  reloadDatabase: () =>
    request<{ success: boolean; message: string }>('/database/reload', {
      method: 'POST',
    }),
};

