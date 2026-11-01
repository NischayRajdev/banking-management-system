export type AccountType = 'SAVINGS' | 'CURRENT';
export type AccountStatus = 'ACTIVE' | 'FROZEN' | 'CLOSED';
export type TransactionType =
  | 'DEPOSIT'
  | 'WITHDRAWAL'
  | 'TRANSFER_IN'
  | 'TRANSFER_OUT'
  | 'INTEREST_CREDIT'
  | 'FEE_DEBIT';
export type TransactionStatus = 'SUCCESS' | 'FAILED' | 'REVERSED';

export interface Account {
  accountNumber: string;
  customerId: string;
  accountHolderName: string;
  balance: number;
  accountType: AccountType;
  accountTypeDisplay: string;
  status: AccountStatus;
  createdAt: string;
  // Savings Account specific
  interestRate?: number;
  minimumBalance?: number;
  maxWithdrawalPerTransaction?: number;
  // Current Account specific
  overdraftLimit?: number;
  overdraftInterestRate?: number;
  utilizedOverdraft?: number;
  availableOverdraft?: number;
  totalAvailableLiquidity?: number;
  annualInterestAccrual?: number;
}

export interface Customer {
  customerId: string;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string;
  phoneNumber: string;
  address: string;
  createdAt: string;
  accountCount: number;
  totalPortfolioBalance: number;
  accounts?: Account[];
}

export interface Transaction {
  transactionId: string;
  accountNumber: string;
  relatedAccountNumber: string | null;
  type: TransactionType;
  typeDescription: string;
  amount: number;
  balanceAfter: number;
  status: TransactionStatus;
  remarks: string;
  timestamp: string;
}

export interface DashboardSummary {
  totalCustomers: number;
  totalAccounts: number;
  totalTransactions: number;
  totalDeposits: number;
  totalNetWorth: number;
  activeAccountsCount: number;
  frozenAccountsCount: number;
  closedAccountsCount: number;
  savingsAccountsCount: number;
  currentAccountsCount: number;
  totalOverdraftSanctioned: number;
  totalOverdraftUtilized: number;
  recentTransactions: Transaction[];
}

export interface AnalyticsData {
  savingsCount: number;
  currentCount: number;
  savingsBalanceTotal: number;
  currentBalanceTotal: number;
  successTransactions: number;
  failedTransactions: number;
  reversedTransactions: number;
  totalDepositsVolume: number;
  totalWithdrawalsVolume: number;
  totalTransfersVolume: number;
  overdrawnAccounts: Account[];
  topAccounts: Account[];
}

export interface DatabaseTableInfo {
  name: string;
  rowCount: number;
  description: string;
}

export interface DatabaseStatus {
  databaseUrl: string;
  tables: DatabaseTableInfo[];
}

export interface HealthInfo {
  status: string;
  bankName: string;
  branchCode: string;
  serverTime: string;
  currency: string;
  currencySymbol: string;
}

