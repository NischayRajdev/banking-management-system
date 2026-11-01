import React, { useEffect, useState } from 'react';
import {
  ArrowDownLeft,
  ArrowUpRight,
  ArrowLeftRight,
  History,
  Search,
  CheckCircle2,
  AlertCircle,
  HelpCircle,
  RefreshCw,
  Wallet,
  ShieldCheck,
  FileText,
  DollarSign,
} from 'lucide-react';
import { api } from '../services/api';
import { Account, Transaction, TransactionType } from '../types/banking';
import { formatCurrency, formatDateTime } from '../utils/formatters';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Dialog } from '../components/ui/Dialog';
import { Badge } from '../components/ui/Badge';
import { useToast } from '../components/ui/Toast';

export const TransactionsPage: React.FC = () => {
  const toast = useToast();
  const [activeTab, setActiveTab] = useState<'deposit' | 'withdraw' | 'transfer' | 'history'>('deposit');
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Form states
  const [depositForm, setDepositForm] = useState({
    accountNumber: '',
    amount: '',
    remarks: '',
  });

  const [withdrawForm, setWithdrawForm] = useState({
    accountNumber: '',
    amount: '',
    remarks: '',
  });

  const [transferForm, setTransferForm] = useState({
    sourceAccountNumber: '',
    destinationAccountNumber: '',
    amount: '',
    remarks: '',
  });

  // History filters
  const [historySearch, setHistorySearch] = useState('');
  const [historyType, setHistoryType] = useState('');
  const [historyStatus, setHistoryStatus] = useState('');

  // Confirmation Modal state
  const [confirmModal, setConfirmModal] = useState<{
    isOpen: boolean;
    title: string;
    description: string;
    onConfirm: () => Promise<void>;
    details: { label: string; value: string }[];
  }>({
    isOpen: false,
    title: '',
    description: '',
    onConfirm: async () => {},
    details: [],
  });

  // Details Modal
  const [selectedTxn, setSelectedTxn] = useState<Transaction | null>(null);

  const loadData = async () => {
    setIsLoading(true);
    try {
      const [accs, txns] = await Promise.all([
        api.getAccounts(),
        api.getTransactions({ limit: 100 }),
      ]);
      setAccounts(accs);
      setTransactions(txns);

      // Default select first active account if not set
      const activeAccs = accs.filter((a) => a.status === 'ACTIVE');
      if (activeAccs.length > 0) {
        if (!depositForm.accountNumber) setDepositForm((prev) => ({ ...prev, accountNumber: activeAccs[0].accountNumber }));
        if (!withdrawForm.accountNumber) setWithdrawForm((prev) => ({ ...prev, accountNumber: activeAccs[0].accountNumber }));
        if (!transferForm.sourceAccountNumber && activeAccs.length >= 2) {
          setTransferForm((prev) => ({
            ...prev,
            sourceAccountNumber: activeAccs[0].accountNumber,
            destinationAccountNumber: activeAccs[1].accountNumber,
          }));
        }
      }
    } catch (err: any) {
      toast.error('Failed to load transaction data', err.message);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const getAccountByNo = (num: string) => accounts.find((a) => a.accountNumber === num);

  // Quick Amount presets
  const quickAmounts = [1000, 5000, 10000, 25000, 50000];

  // Deposit handler
  const handleDepositSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const amount = parseFloat(depositForm.amount);
    if (isNaN(amount) || amount <= 0) {
      toast.error('Validation Error', 'Deposit amount must be strictly greater than zero.');
      return;
    }
    const acc = getAccountByNo(depositForm.accountNumber);
    if (!acc) {
      toast.error('Validation Error', 'Please select a valid account.');
      return;
    }
    if (acc.status !== 'ACTIVE') {
      toast.error('Account Restricted', `Account is currently ${acc.status}. Only ACTIVE accounts accept deposits.`);
      return;
    }

    setConfirmModal({
      isOpen: true,
      title: 'Confirm Deposit Transaction',
      description: 'You are crediting funds into the customer account ledger.',
      details: [
        { label: 'Target Account', value: `${acc.accountNumber} (${acc.accountHolderName})` },
        { label: 'Account Type', value: acc.accountTypeDisplay },
        { label: 'Current Balance', value: formatCurrency(acc.balance) },
        { label: 'Deposit Credit', value: `+${formatCurrency(amount)}` },
        { label: 'Estimated New Balance', value: formatCurrency(acc.balance + amount) },
        { label: 'Audit Narration', value: depositForm.remarks || 'Standard Deposit' },
      ],
      onConfirm: async () => {
        setIsSubmitting(true);
        try {
          const res = await api.deposit({
            accountNumber: depositForm.accountNumber,
            amount,
            remarks: depositForm.remarks || 'Web Portal Deposit',
          });
          toast.success('Deposit Successful', `Credit of ${formatCurrency(res.amount)} recorded. Txn ID: ${res.transactionId}`);
          setDepositForm((p) => ({ ...p, amount: '', remarks: '' }));
          setConfirmModal((p) => ({ ...p, isOpen: false }));
          loadData();
        } catch (err: any) {
          toast.error('Deposit Failed', err.message);
        } finally {
          setIsSubmitting(false);
        }
      },
    });
  };

  // Withdraw handler
  const handleWithdrawSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const amount = parseFloat(withdrawForm.amount);
    if (isNaN(amount) || amount <= 0) {
      toast.error('Validation Error', 'Withdrawal amount must be strictly positive.');
      return;
    }
    const acc = getAccountByNo(withdrawForm.accountNumber);
    if (!acc) {
      toast.error('Validation Error', 'Please select a valid account.');
      return;
    }
    if (acc.status !== 'ACTIVE') {
      toast.error('Account Restricted', `Cannot withdraw: Account is ${acc.status}.`);
      return;
    }

    setConfirmModal({
      isOpen: true,
      title: 'Confirm Debit Withdrawal',
      description: 'Review debit limits and minimum balance/overdraft compliance.',
      details: [
        { label: 'Debiting Account', value: `${acc.accountNumber} (${acc.accountHolderName})` },
        { label: 'Account Type', value: acc.accountTypeDisplay },
        { label: 'Current Balance', value: formatCurrency(acc.balance) },
        { label: 'Withdrawal Debit', value: `-${formatCurrency(amount)}` },
        { label: 'Estimated New Balance', value: formatCurrency(acc.balance - amount) },
        { label: 'Audit Narration', value: withdrawForm.remarks || 'Standard Withdrawal' },
      ],
      onConfirm: async () => {
        setIsSubmitting(true);
        try {
          const res = await api.withdraw({
            accountNumber: withdrawForm.accountNumber,
            amount,
            remarks: withdrawForm.remarks || 'Web Portal Withdrawal',
          });
          toast.success('Withdrawal Executed', `Debit of ${formatCurrency(res.amount)} recorded. Txn ID: ${res.transactionId}`);
          setWithdrawForm((p) => ({ ...p, amount: '', remarks: '' }));
          setConfirmModal((p) => ({ ...p, isOpen: false }));
          loadData();
        } catch (err: any) {
          toast.error('Withdrawal Denied by Backend', err.message);
        } finally {
          setIsSubmitting(false);
        }
      },
    });
  };

  // Transfer handler
  const handleTransferSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const amount = parseFloat(transferForm.amount);
    if (isNaN(amount) || amount <= 0) {
      toast.error('Validation Error', 'Transfer amount must be strictly greater than zero.');
      return;
    }
    if (transferForm.sourceAccountNumber === transferForm.destinationAccountNumber) {
      toast.error('Validation Error', 'Source and destination accounts must be distinct.');
      return;
    }

    const src = getAccountByNo(transferForm.sourceAccountNumber);
    const dest = getAccountByNo(transferForm.destinationAccountNumber);

    if (!src || !dest) {
      toast.error('Validation Error', 'Select valid source and destination accounts.');
      return;
    }

    setConfirmModal({
      isOpen: true,
      title: 'Confirm Atomic Fund Transfer',
      description: 'ACID transaction with atomic debit from source and credit to destination.',
      details: [
        { label: 'Source Account', value: `${src.accountNumber} (${src.accountHolderName})` },
        { label: 'Destination Account', value: `${dest.accountNumber} (${dest.accountHolderName})` },
        { label: 'Transfer Amount', value: formatCurrency(amount) },
        { label: 'Source New Balance', value: formatCurrency(src.balance - amount) },
        { label: 'Destination New Balance', value: formatCurrency(dest.balance + amount) },
        { label: 'Audit Remarks', value: transferForm.remarks || 'Direct Inter-Account Transfer' },
      ],
      onConfirm: async () => {
        setIsSubmitting(true);
        try {
          const txns = await api.transfer({
            sourceAccountNumber: transferForm.sourceAccountNumber,
            destinationAccountNumber: transferForm.destinationAccountNumber,
            amount,
            remarks: transferForm.remarks || 'Inter-Account Transfer',
          });
          toast.success(
            'Atomic Transfer Completed',
            `Transferred ${formatCurrency(amount)}. Source Txn: ${txns[0].transactionId}, Dest Txn: ${txns[1].transactionId}`
          );
          setTransferForm((p) => ({ ...p, amount: '', remarks: '' }));
          setConfirmModal((p) => ({ ...p, isOpen: false }));
          loadData();
        } catch (err: any) {
          toast.error('Fund Transfer Aborted & Rolled Back', err.message);
        } finally {
          setIsSubmitting(false);
        }
      },
    });
  };

  // Filtered transactions for History tab
  const filteredTransactions = transactions.filter((t) => {
    const matchSearch =
      !historySearch ||
      t.transactionId.toLowerCase().includes(historySearch.toLowerCase()) ||
      t.accountNumber.toLowerCase().includes(historySearch.toLowerCase()) ||
      (t.relatedAccountNumber && t.relatedAccountNumber.toLowerCase().includes(historySearch.toLowerCase())) ||
      t.remarks.toLowerCase().includes(historySearch.toLowerCase());

    const matchType = !historyType || t.type === historyType;
    const matchStatus = !historyStatus || t.status === historyStatus;

    return matchSearch && matchType && matchStatus;
  });

  const selectedDepositAcc = getAccountByNo(depositForm.accountNumber);
  const selectedWithdrawAcc = getAccountByNo(withdrawForm.accountNumber);
  const selectedSrcAcc = getAccountByNo(transferForm.sourceAccountNumber);
  const selectedDestAcc = getAccountByNo(transferForm.destinationAccountNumber);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-2 border-b border-slate-200/70">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">Financial Transactions</h1>
          <p className="text-xs text-slate-500 mt-1">
            Execute deposits, withdrawals, and ACID 2-party transfers directly into the relational core.
          </p>
        </div>
        <Button size="sm" variant="outline" leftIcon={<RefreshCw className="w-3.5 h-3.5" />} onClick={loadData} isLoading={isLoading}>
          Refresh Ledger
        </Button>
      </div>

      {/* Tabs Bar */}
      <div className="flex border-b border-slate-200 text-xs font-semibold gap-6">
        <button
          onClick={() => setActiveTab('deposit')}
          className={`pb-3 flex items-center gap-2 border-b-2 transition-all ${
            activeTab === 'deposit'
              ? 'border-blue-600 text-blue-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <ArrowDownLeft className="w-4 h-4 text-emerald-600" />
          Deposit Funds
        </button>
        <button
          onClick={() => setActiveTab('withdraw')}
          className={`pb-3 flex items-center gap-2 border-b-2 transition-all ${
            activeTab === 'withdraw'
              ? 'border-blue-600 text-blue-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <ArrowUpRight className="w-4 h-4 text-rose-600" />
          Withdraw Funds
        </button>
        <button
          onClick={() => setActiveTab('transfer')}
          className={`pb-3 flex items-center gap-2 border-b-2 transition-all ${
            activeTab === 'transfer'
              ? 'border-blue-600 text-blue-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <ArrowLeftRight className="w-4 h-4 text-blue-600" />
          Atomic Fund Transfer
        </button>
        <button
          onClick={() => setActiveTab('history')}
          className={`pb-3 flex items-center gap-2 border-b-2 transition-all ${
            activeTab === 'history'
              ? 'border-blue-600 text-blue-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <History className="w-4 h-4 text-slate-500" />
          Ledger History ({transactions.length})
        </button>
      </div>

      {/* Tab 1: Deposit */}
      {activeTab === 'deposit' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <Card className="lg:col-span-2">
            <CardHeader>
              <CardTitle>Credit Funds to Account</CardTitle>
              <CardDescription>
                Deposits increase account balance and are logged as immutable credit entries in INR.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleDepositSubmit} className="space-y-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                    Target Account <span className="text-rose-500">*</span>
                  </label>
                  <select
                    value={depositForm.accountNumber}
                    onChange={(e) => setDepositForm({ ...depositForm, accountNumber: e.target.value })}
                    className="block w-full rounded-lg border border-slate-200 text-xs text-slate-900 px-3.5 py-2.5 bg-white focus:outline-none focus:border-blue-500"
                    required
                  >
                    {accounts.map((acc) => (
                      <option key={acc.accountNumber} value={acc.accountNumber}>
                        {acc.accountNumber} - {acc.accountHolderName} ({acc.accountType}) [Bal: {formatCurrency(acc.balance)}]
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <Input
                    label="Deposit Amount (₹)"
                    type="number"
                    step="0.01"
                    required
                    placeholder="Enter deposit amount..."
                    value={depositForm.amount}
                    onChange={(e) => setDepositForm({ ...depositForm, amount: e.target.value })}
                  />
                  {/* Preset Buttons */}
                  <div className="flex flex-wrap items-center gap-2 mt-2">
                    <span className="text-[11px] text-slate-400 font-medium">Presets:</span>
                    {quickAmounts.map((amt) => (
                      <button
                        type="button"
                        key={amt}
                        onClick={() => setDepositForm({ ...depositForm, amount: amt.toString() })}
                        className="px-2.5 py-1 text-xs bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-md font-medium transition-colors"
                      >
                        +{formatCurrency(amt)}
                      </button>
                    ))}
                  </div>
                </div>

                <Input
                  label="Narration / Remarks"
                  placeholder="e.g. Payroll credit, Cash deposit at branch, Client invoice payment"
                  value={depositForm.remarks}
                  onChange={(e) => setDepositForm({ ...depositForm, remarks: e.target.value })}
                />

                <div className="pt-2">
                  <Button type="submit" size="md" variant="primary" leftIcon={<ArrowDownLeft className="w-4 h-4" />}>
                    Proceed to Confirm Deposit
                  </Button>
                </div>
              </form>
            </CardContent>
          </Card>

          {/* Account Live Snapshot Sidebar */}
          <Card>
            <CardHeader>
              <CardTitle>Selected Account Snapshot</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4 text-xs">
              {selectedDepositAcc ? (
                <>
                  <div className="p-3 bg-slate-50 rounded-lg border border-slate-100 space-y-2">
                    <div>
                      <span className="text-slate-400 font-medium">Account Number:</span>
                      <p className="font-mono font-bold text-slate-900 mt-0.5">{selectedDepositAcc.accountNumber}</p>
                    </div>
                    <div>
                      <span className="text-slate-400 font-medium">Holder Name:</span>
                      <p className="font-semibold text-slate-800 mt-0.5">{selectedDepositAcc.accountHolderName}</p>
                    </div>
                    <div>
                      <span className="text-slate-400 font-medium">Current Balance:</span>
                      <p className="text-lg font-mono font-bold text-slate-900 mt-0.5">
                        {formatCurrency(selectedDepositAcc.balance)}
                      </p>
                    </div>
                  </div>

                  <div className="p-3 bg-blue-50/60 rounded-lg border border-blue-100 text-blue-900">
                    <div className="flex items-center gap-1.5 font-semibold">
                      <ShieldCheck className="w-4 h-4 text-blue-600" />
                      <span>Defensive Domain Enforced</span>
                    </div>
                    <p className="text-[11px] text-blue-700 mt-1 leading-relaxed">
                      Deposits pass through strict business validation before committing to the SQLite database.
                    </p>
                  </div>
                </>
              ) : (
                <p className="text-slate-400">Select an account to view live parameters.</p>
              )}
            </CardContent>
          </Card>
        </div>
      )}

      {/* Tab 2: Withdraw */}
      {activeTab === 'withdraw' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <Card className="lg:col-span-2">
            <CardHeader>
              <CardTitle>Debit Withdrawal from Account</CardTitle>
              <CardDescription>
                Withdrawals enforce mandatory minimum balance for Savings and overdraft limits for Current accounts.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleWithdrawSubmit} className="space-y-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                    Source Account <span className="text-rose-500">*</span>
                  </label>
                  <select
                    value={withdrawForm.accountNumber}
                    onChange={(e) => setWithdrawForm({ ...withdrawForm, accountNumber: e.target.value })}
                    className="block w-full rounded-lg border border-slate-200 text-xs text-slate-900 px-3.5 py-2.5 bg-white focus:outline-none focus:border-blue-500"
                    required
                  >
                    {accounts.map((acc) => (
                      <option key={acc.accountNumber} value={acc.accountNumber}>
                        {acc.accountNumber} - {acc.accountHolderName} ({acc.accountType}) [Bal: {formatCurrency(acc.balance)}]
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <Input
                    label="Withdrawal Amount (₹)"
                    type="number"
                    step="0.01"
                    required
                    placeholder="Enter debit amount..."
                    value={withdrawForm.amount}
                    onChange={(e) => setWithdrawForm({ ...withdrawForm, amount: e.target.value })}
                  />
                  {/* Presets */}
                  <div className="flex flex-wrap items-center gap-2 mt-2">
                    <span className="text-[11px] text-slate-400 font-medium">Presets:</span>
                    {quickAmounts.map((amt) => (
                      <button
                        type="button"
                        key={amt}
                        onClick={() => setWithdrawForm({ ...withdrawForm, amount: amt.toString() })}
                        className="px-2.5 py-1 text-xs bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-md font-medium transition-colors"
                      >
                        {formatCurrency(amt)}
                      </button>
                    ))}
                  </div>
                </div>

                <Input
                  label="Narration / Remarks"
                  placeholder="e.g. ATM cash withdrawal, Merchant payment, Office equipment purchase"
                  value={withdrawForm.remarks}
                  onChange={(e) => setWithdrawForm({ ...withdrawForm, remarks: e.target.value })}
                />

                <div className="pt-2">
                  <Button type="submit" size="md" variant="danger" leftIcon={<ArrowUpRight className="w-4 h-4" />}>
                    Proceed to Confirm Withdrawal
                  </Button>
                </div>
              </form>
            </CardContent>
          </Card>

          {/* Account Live Snapshot Sidebar */}
          <Card>
            <CardHeader>
              <CardTitle>Debit Compliance Check</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4 text-xs">
              {selectedWithdrawAcc ? (
                <>
                  <div className="p-3 bg-slate-50 rounded-lg border border-slate-100 space-y-2">
                    <div>
                      <span className="text-slate-400 font-medium">Account:</span>
                      <p className="font-mono font-bold text-slate-900 mt-0.5">{selectedWithdrawAcc.accountNumber}</p>
                    </div>
                    <div>
                      <span className="text-slate-400 font-medium">Type:</span>
                      <p className="font-semibold text-blue-700 mt-0.5">{selectedWithdrawAcc.accountTypeDisplay}</p>
                    </div>
                    <div>
                      <span className="text-slate-400 font-medium">Available Balance:</span>
                      <p className="text-lg font-mono font-bold text-slate-900 mt-0.5">
                        {formatCurrency(selectedWithdrawAcc.balance)}
                      </p>
                    </div>

                    {selectedWithdrawAcc.accountType === 'SAVINGS' ? (
                      <div className="pt-2 border-t border-slate-200/60 text-slate-600">
                        <p>Mandatory Min Balance: <strong>{formatCurrency(selectedWithdrawAcc.minimumBalance)}</strong></p>
                        <p className="mt-0.5">Max Limit/Txn: <strong>{formatCurrency(selectedWithdrawAcc.maxWithdrawalPerTransaction)}</strong></p>
                      </div>
                    ) : (
                      <div className="pt-2 border-t border-slate-200/60 text-slate-600">
                        <p>Overdraft Buffer: <strong>{formatCurrency(selectedWithdrawAcc.overdraftLimit)}</strong></p>
                        <p className="mt-0.5 text-emerald-700 font-semibold">Total Liquidity: {formatCurrency(selectedWithdrawAcc.totalAvailableLiquidity)}</p>
                      </div>
                    )}
                  </div>

                  <div className="p-3 bg-amber-50/70 rounded-lg border border-amber-200/80 text-amber-900">
                    <p className="font-semibold">Backend Constraint Guarantee</p>
                    <p className="text-[11px] text-amber-700 mt-1">
                      If the debit breaches the account's minimum balance or overdraft limit, the backend will reject the transaction with an exact mathematical audit message.
                    </p>
                  </div>
                </>
              ) : null}
            </CardContent>
          </Card>
        </div>
      )}

      {/* Tab 3: Transfer */}
      {activeTab === 'transfer' && (
        <Card>
          <CardHeader>
            <CardTitle>Atomic Inter-Account Fund Transfer</CardTitle>
            <CardDescription>
              Executes a 2-phase ACID atomic transfer between two distinct accounts using JDBC transactional locking.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleTransferSubmit} className="space-y-4 max-w-2xl">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                    Source Account (Debit) <span className="text-rose-500">*</span>
                  </label>
                  <select
                    value={transferForm.sourceAccountNumber}
                    onChange={(e) => setTransferForm({ ...transferForm, sourceAccountNumber: e.target.value })}
                    className="block w-full rounded-lg border border-slate-200 text-xs text-slate-900 px-3.5 py-2.5 bg-white focus:outline-none focus:border-blue-500"
                    required
                  >
                    {accounts.map((acc) => (
                      <option key={acc.accountNumber} value={acc.accountNumber}>
                        {acc.accountNumber} - {acc.accountHolderName} ({formatCurrency(acc.balance)})
                      </option>
                    ))}
                  </select>
                  {selectedSrcAcc && (
                    <span className="text-[11px] text-slate-500 mt-1 block">
                      Available: {formatCurrency(selectedSrcAcc.balance)}
                    </span>
                  )}
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                    Destination Account (Credit) <span className="text-rose-500">*</span>
                  </label>
                  <select
                    value={transferForm.destinationAccountNumber}
                    onChange={(e) => setTransferForm({ ...transferForm, destinationAccountNumber: e.target.value })}
                    className="block w-full rounded-lg border border-slate-200 text-xs text-slate-900 px-3.5 py-2.5 bg-white focus:outline-none focus:border-blue-500"
                    required
                  >
                    {accounts.map((acc) => (
                      <option key={acc.accountNumber} value={acc.accountNumber}>
                        {acc.accountNumber} - {acc.accountHolderName} ({formatCurrency(acc.balance)})
                      </option>
                    ))}
                  </select>
                  {selectedDestAcc && (
                    <span className="text-[11px] text-slate-500 mt-1 block">
                      Current Bal: {formatCurrency(selectedDestAcc.balance)}
                    </span>
                  )}
                </div>
              </div>

              <div>
                <Input
                  label="Transfer Amount (₹)"
                  type="number"
                  step="0.01"
                  required
                  placeholder="Enter amount to transfer..."
                  value={transferForm.amount}
                  onChange={(e) => setTransferForm({ ...transferForm, amount: e.target.value })}
                />
                <div className="flex flex-wrap items-center gap-2 mt-2">
                  <span className="text-[11px] text-slate-400 font-medium">Presets:</span>
                  {quickAmounts.map((amt) => (
                    <button
                      type="button"
                      key={amt}
                      onClick={() => setTransferForm({ ...transferForm, amount: amt.toString() })}
                      className="px-2.5 py-1 text-xs bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-md font-medium transition-colors"
                    >
                      {formatCurrency(amt)}
                    </button>
                  ))}
                </div>
              </div>

              <Input
                label="Audit Narration / Memo"
                placeholder="e.g. Inter-account sweep, Settlement of consulting fee, Vendor invoice #402"
                value={transferForm.remarks}
                onChange={(e) => setTransferForm({ ...transferForm, remarks: e.target.value })}
              />

              <div className="p-3 bg-slate-50 rounded-lg border border-slate-200 text-xs text-slate-600 flex items-start gap-2">
                <ShieldCheck className="w-4 h-4 text-emerald-600 flex-shrink-0 mt-0.5" />
                <div>
                  <strong className="text-slate-800">Atomic Rollback Guarantee:</strong>
                  <p className="text-[11px] text-slate-500 mt-0.5">
                    If either the source debit or destination credit fails for any reason (insufficient funds, constraint breach, or network failure), the entire transaction rolls back cleanly with zero balance corruption.
                  </p>
                </div>
              </div>

              <div className="pt-2">
                <Button type="submit" size="md" variant="primary" leftIcon={<ArrowLeftRight className="w-4 h-4" />}>
                  Proceed to Confirm Atomic Transfer
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>
      )}

      {/* Tab 4: Ledger History */}
      {activeTab === 'history' && (
        <Card>
          <CardHeader className="p-4">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-3">
              <div className="flex-1 max-w-sm relative">
                <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                <input
                  type="text"
                  value={historySearch}
                  onChange={(e) => setHistorySearch(e.target.value)}
                  placeholder="Filter by Txn ID, account or remarks..."
                  className="w-full bg-slate-50 focus:bg-white text-xs text-slate-900 pl-9 pr-4 py-2 rounded-lg border border-slate-200 focus:outline-none focus:border-blue-500 transition-colors"
                />
              </div>

              <div className="flex flex-wrap items-center gap-2.5">
                <select
                  value={historyType}
                  onChange={(e) => setHistoryType(e.target.value)}
                  className="text-xs bg-slate-50 text-slate-700 border border-slate-200 rounded-lg px-3 py-2 focus:outline-none focus:border-blue-500"
                >
                  <option value="">All Transaction Types</option>
                  <option value="DEPOSIT">Deposit</option>
                  <option value="WITHDRAWAL">Withdrawal</option>
                  <option value="TRANSFER_IN">Transfer In</option>
                  <option value="TRANSFER_OUT">Transfer Out</option>
                  <option value="INTEREST_CREDIT">Interest Credit</option>
                  <option value="FEE_DEBIT">Overdraft Fee</option>
                </select>

                <select
                  value={historyStatus}
                  onChange={(e) => setHistoryStatus(e.target.value)}
                  className="text-xs bg-slate-50 text-slate-700 border border-slate-200 rounded-lg px-3 py-2 focus:outline-none focus:border-blue-500"
                >
                  <option value="">All Execution Statuses</option>
                  <option value="SUCCESS">SUCCESS</option>
                  <option value="FAILED">FAILED</option>
                  <option value="REVERSED">REVERSED</option>
                </select>
              </div>
            </div>
          </CardHeader>

          <CardContent className="p-0">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-50 text-slate-600 uppercase tracking-wider font-semibold border-y border-slate-100">
                  <tr>
                    <th className="py-3 px-4">Txn ID</th>
                    <th className="py-3 px-4">Account Number</th>
                    <th className="py-3 px-4">Type</th>
                    <th className="py-3 px-4">Amount (₹)</th>
                    <th className="py-3 px-4">Balance After</th>
                    <th className="py-3 px-4">Status</th>
                    <th className="py-3 px-4">Date & Time</th>
                    <th className="py-3 px-4">Remarks</th>
                    <th className="py-3 px-4 text-right">Receipt</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 text-slate-700">
                  {filteredTransactions.length > 0 ? (
                    filteredTransactions.map((txn) => {
                      const isCredit =
                        txn.type === 'DEPOSIT' ||
                        txn.type === 'TRANSFER_IN' ||
                        txn.type === 'INTEREST_CREDIT';
                      return (
                        <tr key={txn.transactionId} className="hover:bg-slate-50/70 transition-colors">
                          <td className="py-3 px-4 font-mono font-medium text-slate-900">{txn.transactionId}</td>
                          <td className="py-3 px-4 font-mono text-slate-700">
                            {txn.accountNumber}
                            {txn.relatedAccountNumber && (
                              <span className="text-[10px] text-slate-400 block font-sans">
                                ↔ {txn.relatedAccountNumber}
                              </span>
                            )}
                          </td>
                          <td className="py-3 px-4">
                            <span
                              className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                                isCredit ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-700'
                              }`}
                            >
                              {txn.type}
                            </span>
                          </td>
                          <td className={`py-3 px-4 font-semibold font-mono ${isCredit ? 'text-emerald-600' : 'text-slate-900'}`}>
                            {isCredit ? '+' : '-'}{formatCurrency(txn.amount)}
                          </td>
                          <td className="py-3 px-4 font-mono text-slate-600">{formatCurrency(txn.balanceAfter)}</td>
                          <td className="py-3 px-4">
                            <Badge variant={txn.status === 'SUCCESS' ? 'success' : 'danger'} size="sm">
                              {txn.status}
                            </Badge>
                          </td>
                          <td className="py-3 px-4 text-slate-500 whitespace-nowrap">{formatDateTime(txn.timestamp)}</td>
                          <td className="py-3 px-4 text-slate-600 truncate max-w-[150px]">{txn.remarks}</td>
                          <td className="py-3 px-4 text-right">
                            <button
                              onClick={() => setSelectedTxn(txn)}
                              className="text-blue-600 hover:text-blue-800 font-semibold hover:underline"
                            >
                              View
                            </button>
                          </td>
                        </tr>
                      );
                    })
                  ) : (
                    <tr>
                      <td colSpan={9} className="py-8 text-center text-slate-400">
                        No transactions found matching criteria.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Confirmation Dialog */}
      <Dialog
        isOpen={confirmModal.isOpen}
        onClose={() => setConfirmModal((p) => ({ ...p, isOpen: false }))}
        title={confirmModal.title}
        description={confirmModal.description}
      >
        <div className="space-y-4 text-xs">
          <div className="p-4 bg-slate-50 rounded-xl border border-slate-200/80 space-y-2.5">
            {confirmModal.details.map((d, i) => (
              <div key={i} className="flex justify-between items-center py-1 border-b border-slate-100 last:border-b-0">
                <span className="text-slate-500 font-medium">{d.label}:</span>
                <span className="font-semibold text-slate-900 text-right">{d.value}</span>
              </div>
            ))}
          </div>

          <div className="p-3 bg-amber-50 rounded-lg border border-amber-200 text-amber-800 text-[11px] leading-relaxed">
            Please verify the transaction details. This operation will atomically commit to the database ledger upon confirmation.
          </div>

          <div className="flex justify-end gap-2.5 pt-2 border-t border-slate-100">
            <Button
              size="sm"
              variant="outline"
              onClick={() => setConfirmModal((p) => ({ ...p, isOpen: false }))}
              disabled={isSubmitting}
            >
              Cancel
            </Button>
            <Button
              size="sm"
              variant="primary"
              onClick={confirmModal.onConfirm}
              isLoading={isSubmitting}
            >
              Confirm & Execute Transaction
            </Button>
          </div>
        </div>
      </Dialog>

      {/* Transaction Receipt Modal */}
      <Dialog
        isOpen={!!selectedTxn}
        onClose={() => setSelectedTxn(null)}
        title="Official Transaction Receipt"
        description="India Bank cryptographic financial ledger receipt."
      >
        {selectedTxn && (
          <div className="space-y-4 text-xs">
            <div className="p-5 bg-slate-50 rounded-xl border border-slate-200 space-y-3 font-mono">
              <div className="text-center pb-2 border-b border-slate-200">
                <p className="font-bold text-sm tracking-wide text-slate-900">INDIA BANK CORE</p>
                <p className="text-[10px] text-slate-500">Branch: INDB-001 • Transaction Receipt</p>
              </div>

              <div className="space-y-1.5 text-[11px]">
                <div className="flex justify-between">
                  <span className="text-slate-400">TXN REFERENCE:</span>
                  <span className="font-bold text-slate-900">{selectedTxn.transactionId}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">TIMESTAMP:</span>
                  <span>{formatDateTime(selectedTxn.timestamp)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">ACCOUNT:</span>
                  <span>{selectedTxn.accountNumber}</span>
                </div>
                {selectedTxn.relatedAccountNumber && (
                  <div className="flex justify-between">
                    <span className="text-slate-400">COUNTERPARTY:</span>
                    <span>{selectedTxn.relatedAccountNumber}</span>
                  </div>
                )}
                <div className="flex justify-between">
                  <span className="text-slate-400">OPERATION:</span>
                  <span className="font-bold text-blue-700">{selectedTxn.type}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">STATUS:</span>
                  <span className="font-bold text-emerald-700">{selectedTxn.status}</span>
                </div>
                <div className="flex justify-between pt-2 border-t border-slate-200 text-sm">
                  <span className="font-bold text-slate-900">AMOUNT:</span>
                  <span className="font-bold text-slate-900">{formatCurrency(selectedTxn.amount)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">BALANCE AFTER:</span>
                  <span className="font-semibold text-slate-800">{formatCurrency(selectedTxn.balanceAfter)}</span>
                </div>
              </div>

              {selectedTxn.remarks && (
                <div className="pt-2 border-t border-slate-200 text-[10px] text-slate-500 italic">
                  Narration: {selectedTxn.remarks}
                </div>
              )}
            </div>

            <div className="flex justify-end pt-2">
              <Button size="sm" variant="outline" onClick={() => setSelectedTxn(null)}>
                Close Receipt
              </Button>
            </div>
          </div>
        )}
      </Dialog>
    </div>
  );
};

