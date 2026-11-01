import React, { useEffect, useState } from 'react';
import {
  Wallet,
  Search,
  PlusCircle,
  RefreshCw,
  Lock,
  Unlock,
  Percent,
  AlertCircle,
  CheckCircle2,
  TrendingDown,
  ArrowUpDown,
  CreditCard,
  FileText,
  DollarSign,
} from 'lucide-react';
import { api } from '../services/api';
import { Account, Customer, AccountType, AccountStatus, Transaction } from '../types/banking';
import { formatCurrency, formatDateTime } from '../utils/formatters';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Select } from '../components/ui/Select';
import { Dialog } from '../components/ui/Dialog';
import { Badge } from '../components/ui/Badge';
import { useToast } from '../components/ui/Toast';

export const AccountManagement: React.FC = () => {
  const toast = useToast();
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // Filters
  const [searchQuery, setSearchQuery] = useState('');
  const [typeFilter, setTypeFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  // Modals
  const [isSavingsModalOpen, setIsSavingsModalOpen] = useState(false);
  const [isCurrentModalOpen, setIsCurrentModalOpen] = useState(false);
  const [selectedAccount, setSelectedAccount] = useState<Account | null>(null);
  const [accountStatement, setAccountStatement] = useState<Transaction[]>([]);
  const [isStatementLoading, setIsStatementLoading] = useState(false);
  const [isActionLoading, setIsActionLoading] = useState(false);

  // Savings form
  const [savingsForm, setSavingsForm] = useState({
    customerId: '',
    initialDeposit: '5000',
    interestRate: '0.045',
    minimumBalance: '500',
    maxWithdrawalLimit: '25000',
  });

  // Current form
  const [currentForm, setCurrentForm] = useState({
    customerId: '',
    initialDeposit: '10000',
    overdraftLimit: '25000',
    overdraftInterestRate: '0.10',
  });

  const fetchAccounts = async () => {
    setIsLoading(true);
    try {
      const data = await api.getAccounts({
        q: searchQuery,
        type: typeFilter,
        status: statusFilter,
      });
      setAccounts(data);
    } catch (err: any) {
      toast.error('Failed to load accounts', err.message);
    } finally {
      setIsLoading(false);
    }
  };

  const fetchCustomers = async () => {
    try {
      const data = await api.getCustomers();
      setCustomers(data);
    } catch {}
  };

  useEffect(() => {
    fetchAccounts();
    fetchCustomers();
  }, [typeFilter, statusFilter]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    fetchAccounts();
  };

  const handleOpenAccountDetails = async (acc: Account) => {
    setSelectedAccount(acc);
    setIsStatementLoading(true);
    try {
      const txns = await api.getAccountTransactions(acc.accountNumber);
      setAccountStatement(txns);
    } catch {
      setAccountStatement([]);
    } finally {
      setIsStatementLoading(false);
    }
  };

  const handleCreateSavings = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!savingsForm.customerId) {
      toast.error('Validation Error', 'Please select a customer.');
      return;
    }
    const deposit = parseFloat(savingsForm.initialDeposit);
    const minBal = parseFloat(savingsForm.minimumBalance);
    if (deposit < minBal) {
      toast.error('Validation Error', `Initial deposit must be at least the minimum balance of ${formatCurrency(minBal)}.`);
      return;
    }

    setIsActionLoading(true);
    try {
      const created = await api.createSavingsAccount({
        customerId: savingsForm.customerId,
        initialDeposit: deposit,
        interestRate: parseFloat(savingsForm.interestRate),
        minimumBalance: minBal,
        maxWithdrawalLimit: parseFloat(savingsForm.maxWithdrawalLimit),
      });
      toast.success('Savings Account Created', `Account ${created.accountNumber} opened with ${formatCurrency(created.balance)}`);
      setIsSavingsModalOpen(false);
      fetchAccounts();
    } catch (err: any) {
      toast.error('Account Opening Failed', err.message);
    } finally {
      setIsActionLoading(false);
    }
  };

  const handleCreateCurrent = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentForm.customerId) {
      toast.error('Validation Error', 'Please select a customer.');
      return;
    }

    setIsActionLoading(true);
    try {
      const created = await api.createCurrentAccount({
        customerId: currentForm.customerId,
        initialDeposit: parseFloat(currentForm.initialDeposit),
        overdraftLimit: parseFloat(currentForm.overdraftLimit),
        overdraftInterestRate: parseFloat(currentForm.overdraftInterestRate),
      });
      toast.success('Current Account Created', `Account ${created.accountNumber} opened with ${formatCurrency(created.balance)}`);
      setIsCurrentModalOpen(false);
      fetchAccounts();
    } catch (err: any) {
      toast.error('Account Opening Failed', err.message);
    } finally {
      setIsActionLoading(false);
    }
  };

  const handleStatusToggle = async (acc: Account) => {
    const newStatus: AccountStatus = acc.status === 'ACTIVE' ? 'FROZEN' : 'ACTIVE';
    setIsActionLoading(true);
    try {
      const updated = await api.updateAccountStatus(acc.accountNumber, newStatus);
      toast.success(`Account Status Updated`, `${acc.accountNumber} is now marked ${newStatus}`);
      fetchAccounts();
      if (selectedAccount && selectedAccount.accountNumber === acc.accountNumber) {
        setSelectedAccount(updated);
      }
    } catch (err: any) {
      toast.error('Status Change Failed', err.message);
    } finally {
      setIsActionLoading(false);
    }
  };

  const handleApplyInterest = async (acc: Account) => {
    setIsActionLoading(true);
    try {
      const txn = await api.applyInterest(acc.accountNumber);
      toast.success(
        'Interest / Fee Applied',
        `${txn.typeDescription}: ${formatCurrency(txn.amount)}. New Balance: ${formatCurrency(txn.balanceAfter)}`
      );
      fetchAccounts();
      handleOpenAccountDetails(acc);
    } catch (err: any) {
      toast.error('Operation Failed', err.message);
    } finally {
      setIsActionLoading(false);
    }
  };

  const totalSavingsBalance = accounts
    .filter((a) => a.accountType === 'SAVINGS')
    .reduce((sum, a) => sum + a.balance, 0);

  const totalCurrentBalance = accounts
    .filter((a) => a.accountType === 'CURRENT')
    .reduce((sum, a) => sum + a.balance, 0);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-2 border-b border-slate-200/70">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">Account Management</h1>
          <p className="text-xs text-slate-500 mt-1">
            Polymorphic savings & commercial accounts with overdraft liquidity controls.
          </p>
        </div>
        <div className="flex items-center gap-2.5">
          <Button
            size="sm"
            variant="outline"
            leftIcon={<RefreshCw className="w-3.5 h-3.5" />}
            onClick={fetchAccounts}
            isLoading={isLoading}
          >
            Refresh
          </Button>
          <Button
            size="sm"
            variant="outline"
            leftIcon={<PlusCircle className="w-4 h-4" />}
            onClick={() => {
              if (customers.length > 0 && !savingsForm.customerId) {
                setSavingsForm({ ...savingsForm, customerId: customers[0].customerId });
              }
              setIsSavingsModalOpen(true);
            }}
          >
            Open Savings
          </Button>
          <Button
            size="sm"
            variant="primary"
            leftIcon={<PlusCircle className="w-4 h-4" />}
            onClick={() => {
              if (customers.length > 0 && !currentForm.customerId) {
                setCurrentForm({ ...currentForm, customerId: customers[0].customerId });
              }
              setIsCurrentModalOpen(true);
            }}
          >
            Open Current
          </Button>
        </div>
      </div>

      {/* Aggregate Balance Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-5">
        <Card>
          <CardContent className="p-4 flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Total Managed Accounts</p>
              <p className="text-xl font-bold text-slate-900 mt-1">{accounts.length} Active Accounts</p>
            </div>
            <div className="w-10 h-10 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center">
              <Wallet className="w-5 h-5" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4 flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Savings Volume (APY 4.5%)</p>
              <p className="text-xl font-bold text-blue-700 mt-1">{formatCurrency(totalSavingsBalance)}</p>
            </div>
            <div className="w-10 h-10 rounded-lg bg-indigo-50 text-indigo-600 flex items-center justify-center">
              <Percent className="w-5 h-5" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4 flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Current / Commercial Volume</p>
              <p className="text-xl font-bold text-sky-700 mt-1">{formatCurrency(totalCurrentBalance)}</p>
            </div>
            <div className="w-10 h-10 rounded-lg bg-sky-50 text-sky-600 flex items-center justify-center">
              <CreditCard className="w-5 h-5" />
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Filter and Table Card */}
      <Card>
        <CardHeader className="p-4">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-3">
            <form onSubmit={handleSearch} className="flex-1 max-w-sm">
              <div className="relative">
                <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="Search account no, customer ID, or holder..."
                  className="w-full bg-slate-50 focus:bg-white text-xs text-slate-900 pl-9 pr-4 py-2 rounded-lg border border-slate-200 focus:outline-none focus:border-blue-500 transition-colors"
                />
              </div>
            </form>

            <div className="flex flex-wrap items-center gap-2.5">
              <select
                value={typeFilter}
                onChange={(e) => setTypeFilter(e.target.value)}
                className="text-xs bg-slate-50 text-slate-700 border border-slate-200 rounded-lg px-3 py-2 focus:outline-none focus:border-blue-500"
              >
                <option value="">All Account Types</option>
                <option value="SAVINGS">Savings Accounts</option>
                <option value="CURRENT">Current Accounts</option>
              </select>

              <select
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
                className="text-xs bg-slate-50 text-slate-700 border border-slate-200 rounded-lg px-3 py-2 focus:outline-none focus:border-blue-500"
              >
                <option value="">All Statuses</option>
                <option value="ACTIVE">ACTIVE</option>
                <option value="FROZEN">FROZEN</option>
                <option value="CLOSED">CLOSED</option>
              </select>
            </div>
          </div>
        </CardHeader>

        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 text-slate-600 uppercase tracking-wider font-semibold border-y border-slate-100">
                <tr>
                  <th className="py-3 px-4">Account Number</th>
                  <th className="py-3 px-4">Account Holder</th>
                  <th className="py-3 px-4">Type</th>
                  <th className="py-3 px-4">Balance (₹)</th>
                  <th className="py-3 px-4">Key Constraints</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-slate-700">
                {accounts.length > 0 ? (
                  accounts.map((acc) => {
                    const isOverdrawn = acc.balance < 0;
                    return (
                      <tr key={acc.accountNumber} className="hover:bg-slate-50/70 transition-colors">
                        <td className="py-3 px-4 font-mono font-medium text-slate-900">
                          {acc.accountNumber}
                        </td>
                        <td className="py-3 px-4">
                          <span className="font-semibold text-slate-900">{acc.accountHolderName}</span>
                          <span className="text-[10px] text-slate-400 block font-mono">{acc.customerId}</span>
                        </td>
                        <td className="py-3 px-4">
                          <span
                            className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                              acc.accountType === 'SAVINGS'
                                ? 'bg-blue-50 text-blue-700'
                                : 'bg-sky-50 text-sky-700'
                            }`}
                          >
                            {acc.accountType}
                          </span>
                        </td>
                        <td className="py-3 px-4 font-mono">
                          <span className={`font-bold ${isOverdrawn ? 'text-rose-600' : 'text-slate-900'}`}>
                            {formatCurrency(acc.balance)}
                          </span>
                        </td>
                        <td className="py-3 px-4 text-[11px] text-slate-500">
                          {acc.accountType === 'SAVINGS' ? (
                            <span>
                              Min: {formatCurrency(acc.minimumBalance)} • Max/Txn: {formatCurrency(acc.maxWithdrawalPerTransaction)}
                            </span>
                          ) : (
                            <span>
                              Overdraft Limit: {formatCurrency(acc.overdraftLimit)} • Available: {formatCurrency(acc.availableOverdraft)}
                            </span>
                          )}
                        </td>
                        <td className="py-3 px-4">
                          <Badge
                            variant={
                              acc.status === 'ACTIVE'
                                ? 'success'
                                : acc.status === 'FROZEN'
                                ? 'warning'
                                : 'danger'
                            }
                            size="sm"
                          >
                            {acc.status}
                          </Badge>
                        </td>
                        <td className="py-3 px-4 text-right space-x-2">
                          <button
                            onClick={() => handleOpenAccountDetails(acc)}
                            className="text-blue-600 hover:text-blue-800 font-semibold hover:underline"
                          >
                            Details
                          </button>
                          <button
                            onClick={() => handleStatusToggle(acc)}
                            className={`font-semibold hover:underline ${
                              acc.status === 'ACTIVE' ? 'text-amber-600 hover:text-amber-800' : 'text-emerald-600 hover:text-emerald-800'
                            }`}
                          >
                            {acc.status === 'ACTIVE' ? 'Freeze' : 'Unfreeze'}
                          </button>
                        </td>
                      </tr>
                    );
                  })
                ) : (
                  <tr>
                    <td colSpan={7} className="py-8 text-center text-slate-400">
                      No accounts matched the criteria.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>

      {/* Account Details & Statement Modal */}
      <Dialog
        isOpen={!!selectedAccount}
        onClose={() => setSelectedAccount(null)}
        title="Account Details & Financial Statement"
        description="Polymorphic parameters, balance compliance, and chronological ledger."
        maxWidth="xl"
      >
        {selectedAccount && (
          <div className="space-y-5 text-xs">
            {/* Top Specs Card */}
            <div className="p-4 bg-slate-50 rounded-xl border border-slate-200/80 space-y-3">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-mono text-base font-bold text-slate-900">
                      {selectedAccount.accountNumber}
                    </span>
                    <Badge variant={selectedAccount.status === 'ACTIVE' ? 'success' : 'warning'}>
                      {selectedAccount.status}
                    </Badge>
                  </div>
                  <p className="text-slate-600 mt-0.5">
                    Holder: <strong className="text-slate-900">{selectedAccount.accountHolderName}</strong> ({selectedAccount.customerId})
                  </p>
                </div>

                <div className="text-right">
                  <span className="text-slate-400 font-medium">Ledger Balance:</span>
                  <p
                    className={`text-xl font-mono font-bold ${
                      selectedAccount.balance < 0 ? 'text-rose-600' : 'text-slate-900'
                    }`}
                  >
                    {formatCurrency(selectedAccount.balance)}
                  </p>
                </div>
              </div>

              {/* Polymorphic Attributes Grid */}
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 pt-3 border-t border-slate-200/60">
                <div>
                  <span className="text-slate-400 font-medium">Account Type:</span>
                  <p className="font-bold text-blue-700 mt-0.5">{selectedAccount.accountTypeDisplay}</p>
                </div>

                {selectedAccount.accountType === 'SAVINGS' ? (
                  <>
                    <div>
                      <span className="text-slate-400 font-medium">Interest Rate (APY):</span>
                      <p className="font-semibold text-slate-800 mt-0.5">
                        {((selectedAccount.interestRate || 0) * 100).toFixed(2)}%
                      </p>
                    </div>
                    <div>
                      <span className="text-slate-400 font-medium">Minimum Balance:</span>
                      <p className="font-semibold text-slate-800 mt-0.5">
                        {formatCurrency(selectedAccount.minimumBalance)}
                      </p>
                    </div>
                    <div>
                      <span className="text-slate-400 font-medium">Max Withdrawal/Txn:</span>
                      <p className="font-semibold text-slate-800 mt-0.5">
                        {formatCurrency(selectedAccount.maxWithdrawalPerTransaction)}
                      </p>
                    </div>
                  </>
                ) : (
                  <>
                    <div>
                      <span className="text-slate-400 font-medium">Sanctioned Overdraft:</span>
                      <p className="font-semibold text-slate-800 mt-0.5">
                        {formatCurrency(selectedAccount.overdraftLimit)}
                      </p>
                    </div>
                    <div>
                      <span className="text-slate-400 font-medium">Available Overdraft:</span>
                      <p className="font-semibold text-emerald-700 mt-0.5">
                        {formatCurrency(selectedAccount.availableOverdraft)}
                      </p>
                    </div>
                    <div>
                      <span className="text-slate-400 font-medium">Total Liquidity:</span>
                      <p className="font-semibold text-slate-900 mt-0.5">
                        {formatCurrency(selectedAccount.totalAvailableLiquidity)}
                      </p>
                    </div>
                  </>
                )}
              </div>
            </div>

            {/* Quick Actions Bar */}
            <div className="flex items-center justify-between p-3 bg-white rounded-lg border border-slate-200">
              <div className="flex items-center gap-2">
                <Button
                  size="sm"
                  variant="outline"
                  leftIcon={<Percent className="w-3.5 h-3.5" />}
                  onClick={() => handleApplyInterest(selectedAccount)}
                  isLoading={isActionLoading}
                >
                  {selectedAccount.accountType === 'SAVINGS' ? 'Post Annual Interest' : 'Charge Overdraft Fee'}
                </Button>
                <Button
                  size="sm"
                  variant="outline"
                  leftIcon={selectedAccount.status === 'ACTIVE' ? <Lock className="w-3.5 h-3.5" /> : <Unlock className="w-3.5 h-3.5" />}
                  onClick={() => handleStatusToggle(selectedAccount)}
                  isLoading={isActionLoading}
                >
                  {selectedAccount.status === 'ACTIVE' ? 'Freeze Account' : 'Unfreeze Account'}
                </Button>
              </div>

              <span className="text-[11px] text-slate-400 font-mono">
                Opened: {formatDateTime(selectedAccount.createdAt)}
              </span>
            </div>

            {/* Chronological Statement Table */}
            <div>
              <h5 className="font-bold text-slate-900 uppercase tracking-wider text-[11px] mb-2 flex items-center gap-1.5">
                <FileText className="w-3.5 h-3.5 text-slate-400" />
                Ledger Statement ({accountStatement.length} Entries)
              </h5>

              <div className="border border-slate-200 rounded-lg overflow-hidden max-h-56 overflow-y-auto">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-50 text-slate-600 uppercase tracking-wider font-semibold border-b border-slate-200 sticky top-0">
                    <tr>
                      <th className="py-2 px-3">Date</th>
                      <th className="py-2 px-3">Type</th>
                      <th className="py-2 px-3">Remarks</th>
                      <th className="py-2 px-3 text-right">Amount (₹)</th>
                      <th className="py-2 px-3 text-right">Balance After</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 text-slate-700">
                    {accountStatement.length > 0 ? (
                      accountStatement.map((txn) => {
                        const isCredit =
                          txn.type === 'DEPOSIT' ||
                          txn.type === 'TRANSFER_IN' ||
                          txn.type === 'INTEREST_CREDIT';
                        return (
                          <tr key={txn.transactionId} className="hover:bg-slate-50/50">
                            <td className="py-2 px-3 text-slate-500 whitespace-nowrap">
                              {formatDateTime(txn.timestamp)}
                            </td>
                            <td className="py-2 px-3 font-semibold text-[11px]">{txn.type}</td>
                            <td className="py-2 px-3 text-slate-600 truncate max-w-[150px]">{txn.remarks}</td>
                            <td
                              className={`py-2 px-3 text-right font-semibold font-mono ${
                                isCredit ? 'text-emerald-600' : 'text-slate-900'
                              }`}
                            >
                              {isCredit ? '+' : '-'}{formatCurrency(txn.amount)}
                            </td>
                            <td className="py-2 px-3 text-right font-mono text-slate-600">
                              {formatCurrency(txn.balanceAfter)}
                            </td>
                          </tr>
                        );
                      })
                    ) : (
                      <tr>
                        <td colSpan={5} className="py-6 text-center text-slate-400">
                          {isStatementLoading ? 'Loading statement...' : 'No ledger entries for this account.'}
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>

            <div className="flex justify-end pt-2 border-t border-slate-100">
              <Button size="sm" variant="outline" onClick={() => setSelectedAccount(null)}>
                Close Statement
              </Button>
            </div>
          </div>
        )}
      </Dialog>

      {/* Open Savings Account Modal */}
      <Dialog
        isOpen={isSavingsModalOpen}
        onClose={() => setIsSavingsModalOpen(false)}
        title="Open Savings Account"
        description="Creates a polymorphic SavingsAccount entity with minimum balance enforcement."
      >
        <form onSubmit={handleCreateSavings} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1.5">
              Select Customer <span className="text-rose-500">*</span>
            </label>
            <select
              value={savingsForm.customerId}
              onChange={(e) => setSavingsForm({ ...savingsForm, customerId: e.target.value })}
              className="block w-full rounded-lg border border-slate-200 text-xs text-slate-900 px-3.5 py-2.5 bg-white focus:outline-none focus:border-blue-500"
              required
            >
              <option value="">-- Choose Existing Customer --</option>
              {customers.map((c) => (
                <option key={c.customerId} value={c.customerId}>
                  {c.fullName} ({c.customerId}) - {c.email}
                </option>
              ))}
            </select>
          </div>

          <Input
            label="Initial Opening Deposit (₹)"
            type="number"
            step="0.01"
            required
            value={savingsForm.initialDeposit}
            onChange={(e) => setSavingsForm({ ...savingsForm, initialDeposit: e.target.value })}
            helperText="Must be greater than or equal to minimum balance."
          />

          <div className="grid grid-cols-3 gap-2.5">
            <Input
              label="Interest Rate (Annual)"
              type="number"
              step="0.001"
              value={savingsForm.interestRate}
              onChange={(e) => setSavingsForm({ ...savingsForm, interestRate: e.target.value })}
              helperText="Default: 0.045 (4.5%)"
            />
            <Input
              label="Min Balance (₹)"
              type="number"
              step="1"
              value={savingsForm.minimumBalance}
              onChange={(e) => setSavingsForm({ ...savingsForm, minimumBalance: e.target.value })}
              helperText="Default: ₹500"
            />
            <Input
              label="Max Per Txn (₹)"
              type="number"
              step="1"
              value={savingsForm.maxWithdrawalLimit}
              onChange={(e) => setSavingsForm({ ...savingsForm, maxWithdrawalLimit: e.target.value })}
              helperText="Default: ₹25,000"
            />
          </div>

          <div className="flex justify-end gap-2.5 pt-3 border-t border-slate-100">
            <Button size="sm" variant="outline" type="button" onClick={() => setIsSavingsModalOpen(false)}>
              Cancel
            </Button>
            <Button size="sm" variant="primary" type="submit" isLoading={isActionLoading}>
              Create Savings Account
            </Button>
          </div>
        </form>
      </Dialog>

      {/* Open Current Account Modal */}
      <Dialog
        isOpen={isCurrentModalOpen}
        onClose={() => setIsCurrentModalOpen(false)}
        title="Open Commercial Current Account"
        description="Creates a CurrentAccount entity with revolving overdraft credit line."
      >
        <form onSubmit={handleCreateCurrent} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1.5">
              Select Customer <span className="text-rose-500">*</span>
            </label>
            <select
              value={currentForm.customerId}
              onChange={(e) => setCurrentForm({ ...currentForm, customerId: e.target.value })}
              className="block w-full rounded-lg border border-slate-200 text-xs text-slate-900 px-3.5 py-2.5 bg-white focus:outline-none focus:border-blue-500"
              required
            >
              <option value="">-- Choose Existing Customer --</option>
              {customers.map((c) => (
                <option key={c.customerId} value={c.customerId}>
                  {c.fullName} ({c.customerId}) - {c.email}
                </option>
              ))}
            </select>
          </div>

          <Input
            label="Initial Opening Deposit (₹)"
            type="number"
            step="0.01"
            required
            value={currentForm.initialDeposit}
            onChange={(e) => setCurrentForm({ ...currentForm, initialDeposit: e.target.value })}
            helperText="Can be zero or positive."
          />

          <div className="grid grid-cols-2 gap-3">
            <Input
              label="Sanctioned Overdraft Limit (₹)"
              type="number"
              step="1000"
              value={currentForm.overdraftLimit}
              onChange={(e) => setCurrentForm({ ...currentForm, overdraftLimit: e.target.value })}
              helperText="Default: ₹25,000 credit buffer"
            />
            <Input
              label="Overdraft APR Rate"
              type="number"
              step="0.005"
              value={currentForm.overdraftInterestRate}
              onChange={(e) => setCurrentForm({ ...currentForm, overdraftInterestRate: e.target.value })}
              helperText="Default: 0.10 (10.0%)"
            />
          </div>

          <div className="flex justify-end gap-2.5 pt-3 border-t border-slate-100">
            <Button size="sm" variant="outline" type="button" onClick={() => setIsCurrentModalOpen(false)}>
              Cancel
            </Button>
            <Button size="sm" variant="primary" type="submit" isLoading={isActionLoading}>
              Create Current Account
            </Button>
          </div>
        </form>
      </Dialog>
    </div>
  );
};

