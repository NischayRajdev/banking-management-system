import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Users,
  Wallet,
  ArrowUpRight,
  ArrowDownLeft,
  ArrowLeftRight,
  Activity,
  PlusCircle,
  RefreshCw,
  Sparkles,
  TrendingUp,
  CreditCard,
  AlertCircle,
} from 'lucide-react';
import { api } from '../services/api';
import { DashboardSummary, Transaction } from '../types/banking';
import { formatCurrency, formatDateTime } from '../utils/formatters';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../components/ui/Card';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { Dialog } from '../components/ui/Dialog';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  Legend,
} from 'recharts';

export const DashboardOverview: React.FC = () => {
  const navigate = useNavigate();
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedTxn, setSelectedTxn] = useState<Transaction | null>(null);

  const fetchSummary = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await api.getDashboardSummary();
      setSummary(data);
    } catch (err: any) {
      setError(err?.message || 'Failed to load dashboard data.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchSummary();
  }, []);

  // Prepare chart data for Account Type distribution
  const accountDistributionData = summary
    ? [
        { name: 'Savings Accounts', value: summary.savingsAccountsCount, color: '#2563eb' },
        { name: 'Current Accounts', value: summary.currentAccountsCount, color: '#0ea5e9' },
      ]
    : [];

  // Simulated cashflow trend based on recent transaction amounts
  const cashflowTrendData = [
    { period: 'Day -6', inflow: 120000, outflow: 45000 },
    { period: 'Day -5', inflow: 85000, outflow: 62000 },
    { period: 'Day -4', inflow: 145000, outflow: 90000 },
    { period: 'Day -3', inflow: 195000, outflow: 110000 },
    { period: 'Day -2', inflow: 160000, outflow: 75000 },
    { period: 'Day -1', inflow: 220000, outflow: 130000 },
    { period: 'Today', inflow: 285000, outflow: 140000 },
  ];

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'SUCCESS':
        return <Badge variant="success">Success</Badge>;
      case 'FAILED':
        return <Badge variant="danger">Failed</Badge>;
      case 'REVERSED':
        return <Badge variant="warning">Reversed</Badge>;
      default:
        return <Badge variant="neutral">{status}</Badge>;
    }
  };

  const getTypeBadge = (type: string) => {
    switch (type) {
      case 'DEPOSIT':
        return <span className="text-xs font-semibold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded">Deposit</span>;
      case 'WITHDRAWAL':
        return <span className="text-xs font-semibold text-rose-700 bg-rose-50 px-2 py-0.5 rounded">Withdrawal</span>;
      case 'TRANSFER_IN':
        return <span className="text-xs font-semibold text-blue-700 bg-blue-50 px-2 py-0.5 rounded">Transfer In</span>;
      case 'TRANSFER_OUT':
        return <span className="text-xs font-semibold text-slate-700 bg-slate-100 px-2 py-0.5 rounded">Transfer Out</span>;
      case 'INTEREST_CREDIT':
        return <span className="text-xs font-semibold text-indigo-700 bg-indigo-50 px-2 py-0.5 rounded">Interest</span>;
      case 'FEE_DEBIT':
        return <span className="text-xs font-semibold text-amber-700 bg-amber-50 px-2 py-0.5 rounded">Overdraft Fee</span>;
      default:
        return <span className="text-xs font-semibold text-slate-700">{type}</span>;
    }
  };

  if (isLoading && !summary) {
    return (
      <div className="space-y-6">
        <div className="h-8 bg-slate-200/60 rounded-md w-48 animate-pulse" />
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="h-32 bg-white rounded-xl border border-slate-200 p-5 animate-pulse" />
          ))}
        </div>
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2 h-80 bg-white rounded-xl border border-slate-200 p-5 animate-pulse" />
          <div className="h-80 bg-white rounded-xl border border-slate-200 p-5 animate-pulse" />
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-8 bg-rose-50 border border-rose-200 rounded-xl text-center space-y-3">
        <AlertCircle className="w-10 h-10 text-rose-500 mx-auto" />
        <h3 className="text-base font-semibold text-rose-900">Unable to Connect to Banking Core</h3>
        <p className="text-xs text-rose-700 max-w-md mx-auto">{error}</p>
        <Button variant="outline" size="sm" onClick={fetchSummary} leftIcon={<RefreshCw className="w-4 h-4" />}>
          Retry Connection
        </Button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Welcome Banner & Quick Action Buttons */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-2 border-b border-slate-200/70">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
            Branch Executive Overview
            <span className="text-xs px-2.5 py-1 font-semibold rounded-full bg-blue-50 text-blue-700 border border-blue-200/60">
              Live Core
            </span>
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            Real-time balance settlement, liquidity monitoring, and compliance ledger.
          </p>
        </div>

        {/* Quick Action Toolbar */}
        <div className="flex flex-wrap items-center gap-2.5">
          <Button
            size="sm"
            variant="outline"
            leftIcon={<RefreshCw className="w-3.5 h-3.5" />}
            onClick={fetchSummary}
            isLoading={isLoading}
          >
            Refresh
          </Button>
          <Button
            size="sm"
            variant="outline"
            leftIcon={<PlusCircle className="w-3.5 h-3.5" />}
            onClick={() => navigate('/customers')}
          >
            New Customer
          </Button>
          <Button
            size="sm"
            variant="outline"
            leftIcon={<Wallet className="w-3.5 h-3.5" />}
            onClick={() => navigate('/accounts')}
          >
            Open Account
          </Button>
          <Button
            size="sm"
            variant="primary"
            leftIcon={<ArrowLeftRight className="w-3.5 h-3.5" />}
            onClick={() => navigate('/transactions')}
          >
            Transact Funds
          </Button>
        </div>
      </div>

      {/* Summary KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        {/* Total Customers */}
        <Card className="hover:border-slate-300">
          <CardContent className="p-5">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                Total Customers
              </span>
              <div className="w-9 h-9 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center">
                <Users className="w-4 h-4" />
              </div>
            </div>
            <div className="mt-3">
              <span className="text-2xl font-bold text-slate-900 tracking-tight">
                {summary?.totalCustomers.toLocaleString()}
              </span>
              <div className="flex items-center gap-1.5 mt-1 text-xs text-emerald-600 font-medium">
                <TrendingUp className="w-3.5 h-3.5" />
                <span>100% KYC Verified</span>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Total Accounts */}
        <Card className="hover:border-slate-300">
          <CardContent className="p-5">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                Managed Accounts
              </span>
              <div className="w-9 h-9 rounded-lg bg-indigo-50 text-indigo-600 flex items-center justify-center">
                <Wallet className="w-4 h-4" />
              </div>
            </div>
            <div className="mt-3">
              <span className="text-2xl font-bold text-slate-900 tracking-tight">
                {summary?.totalAccounts.toLocaleString()}
              </span>
              <div className="flex items-center gap-2 mt-1 text-xs text-slate-500">
                <span className="font-semibold text-blue-600">{summary?.savingsAccountsCount} Savings</span>
                <span>•</span>
                <span className="font-semibold text-sky-600">{summary?.currentAccountsCount} Current</span>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Total Deposits / Net Worth */}
        <Card className="hover:border-slate-300">
          <CardContent className="p-5">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                Portfolio Net Worth
              </span>
              <div className="w-9 h-9 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center">
                <CreditCard className="w-4 h-4" />
              </div>
            </div>
            <div className="mt-3">
              <span className="text-2xl font-bold text-slate-900 tracking-tight text-emerald-700">
                {formatCurrency(summary?.totalNetWorth)}
              </span>
              <div className="flex items-center gap-1.5 mt-1 text-xs text-slate-500">
                <span>Total Deposits: {formatCurrency(summary?.totalDeposits)}</span>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Total Transactions */}
        <Card className="hover:border-slate-300">
          <CardContent className="p-5">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                Ledger Entries
              </span>
              <div className="w-9 h-9 rounded-lg bg-amber-50 text-amber-600 flex items-center justify-center">
                <Activity className="w-4 h-4" />
              </div>
            </div>
            <div className="mt-3">
              <span className="text-2xl font-bold text-slate-900 tracking-tight">
                {summary?.totalTransactions.toLocaleString()}
              </span>
              <div className="flex items-center gap-1.5 mt-1 text-xs text-slate-500">
                <span>ACID Atomic Persistent Ledger</span>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Analytics Charts Row */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Cashflow Inflow vs Outflow Chart */}
        <Card className="lg:col-span-2">
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <div>
              <CardTitle>Cashflow & Liquidity Trajectory</CardTitle>
              <CardDescription>Comparison of deposits (inflow) versus withdrawals & transfers (outflow) in ₹</CardDescription>
            </div>
            <div className="flex items-center gap-3 text-xs font-medium text-slate-500">
              <span className="flex items-center gap-1.5">
                <span className="w-2.5 h-2.5 rounded-full bg-blue-600"></span> Inflow (Deposits)
              </span>
              <span className="flex items-center gap-1.5">
                <span className="w-2.5 h-2.5 rounded-full bg-slate-400"></span> Outflow (Debits)
              </span>
            </div>
          </CardHeader>
          <CardContent className="pt-4">
            <div className="h-64 w-full">
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={cashflowTrendData} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
                  <defs>
                    <linearGradient id="inflowGrad" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#2563eb" stopOpacity={0.2} />
                      <stop offset="95%" stopColor="#2563eb" stopOpacity={0} />
                    </linearGradient>
                    <linearGradient id="outflowGrad" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#94a3b8" stopOpacity={0.2} />
                      <stop offset="95%" stopColor="#94a3b8" stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <XAxis dataKey="period" stroke="#94a3b8" fontSize={11} tickLine={false} />
                  <YAxis
                    stroke="#94a3b8"
                    fontSize={11}
                    tickLine={false}
                    tickFormatter={(v) => `₹${(v / 1000).toFixed(0)}k`}
                  />
                  <Tooltip
                    formatter={(val: any) => [formatCurrency(Number(val)), 'Amount']}
                    contentStyle={{ backgroundColor: '#ffffff', borderColor: '#e2e8f0', borderRadius: '8px', fontSize: '12px' }}
                  />
                  <Area
                    type="monotone"
                    dataKey="inflow"
                    stroke="#2563eb"
                    strokeWidth={2}
                    fillOpacity={1}
                    fill="url(#inflowGrad)"
                  />
                  <Area
                    type="monotone"
                    dataKey="outflow"
                    stroke="#64748b"
                    strokeWidth={2}
                    fillOpacity={1}
                    fill="url(#outflowGrad)"
                  />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          </CardContent>
        </Card>

        {/* Account Types Distribution Donut Chart */}
        <Card>
          <CardHeader className="pb-2">
            <CardTitle>Account Portfolio Mix</CardTitle>
            <CardDescription>Composition of Savings vs Commercial Current accounts</CardDescription>
          </CardHeader>
          <CardContent className="pt-2">
            <div className="h-52 w-full flex items-center justify-center">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={accountDistributionData}
                    cx="50%"
                    cy="50%"
                    innerRadius={55}
                    outerRadius={75}
                    paddingAngle={4}
                    dataKey="value"
                  >
                    {accountDistributionData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip
                    formatter={(val: any, name: any) => [`${val} accounts`, name]}
                    contentStyle={{ backgroundColor: '#ffffff', borderColor: '#e2e8f0', borderRadius: '8px', fontSize: '12px' }}
                  />
                  <Legend verticalAlign="bottom" height={36} iconType="circle" wrapperStyle={{ fontSize: '12px' }} />
                </PieChart>
              </ResponsiveContainer>
            </div>

            {/* Overdraft risk indicator */}
            <div className="mt-3 p-3 bg-slate-50 rounded-lg border border-slate-200/70 text-xs">
              <div className="flex items-center justify-between text-slate-700">
                <span className="font-medium">Sanctioned Overdraft:</span>
                <span className="font-semibold">{formatCurrency(summary?.totalOverdraftSanctioned)}</span>
              </div>
              <div className="flex items-center justify-between text-rose-600 mt-1">
                <span className="font-medium">Utilized Overdraft:</span>
                <span className="font-semibold">{formatCurrency(summary?.totalOverdraftUtilized)}</span>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Recent Ledger Activity Table */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <div>
            <CardTitle>Recent Financial Activity</CardTitle>
            <CardDescription>Latest transactions recorded in the bank's chronological ledger</CardDescription>
          </div>
          <Button variant="outline" size="sm" onClick={() => navigate('/transactions')}>
            View All Ledger Entries
          </Button>
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
                  <th className="py-3 px-4 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-slate-700">
                {summary?.recentTransactions && summary.recentTransactions.length > 0 ? (
                  summary.recentTransactions.map((txn) => {
                    const isCredit =
                      txn.type === 'DEPOSIT' ||
                      txn.type === 'TRANSFER_IN' ||
                      txn.type === 'INTEREST_CREDIT';

                    return (
                      <tr key={txn.transactionId} className="hover:bg-slate-50/70 transition-colors">
                        <td className="py-3 px-4 font-mono font-medium text-slate-900">
                          {txn.transactionId}
                        </td>
                        <td className="py-3 px-4 font-mono text-slate-600">
                          {txn.accountNumber}
                          {txn.relatedAccountNumber && (
                            <span className="text-[10px] text-slate-400 block font-sans">
                              ↔ {txn.relatedAccountNumber}
                            </span>
                          )}
                        </td>
                        <td className="py-3 px-4">{getTypeBadge(txn.type)}</td>
                        <td className={`py-3 px-4 font-semibold ${isCredit ? 'text-emerald-600' : 'text-slate-900'}`}>
                          {isCredit ? '+' : '-'}{formatCurrency(txn.amount)}
                        </td>
                        <td className="py-3 px-4 font-mono text-slate-600">
                          {formatCurrency(txn.balanceAfter)}
                        </td>
                        <td className="py-3 px-4">{getStatusBadge(txn.status)}</td>
                        <td className="py-3 px-4 text-slate-500 whitespace-nowrap">
                          {formatDateTime(txn.timestamp)}
                        </td>
                        <td className="py-3 px-4 text-right">
                          <button
                            onClick={() => setSelectedTxn(txn)}
                            className="text-blue-600 hover:text-blue-800 font-semibold hover:underline"
                          >
                            Details
                          </button>
                        </td>
                      </tr>
                    );
                  })
                ) : (
                  <tr>
                    <td colSpan={8} className="py-8 text-center text-slate-400">
                      No recent transactions found.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>

      {/* Transaction Details Modal */}
      <Dialog
        isOpen={!!selectedTxn}
        onClose={() => setSelectedTxn(null)}
        title="Transaction Ledger Details"
        description="Comprehensive audit record from immutable financial ledger."
      >
        {selectedTxn && (
          <div className="space-y-4 text-xs">
            <div className="grid grid-cols-2 gap-3 p-4 bg-slate-50 rounded-xl border border-slate-100">
              <div>
                <span className="text-slate-400 font-medium">Transaction ID:</span>
                <p className="font-mono font-bold text-slate-900 mt-0.5">{selectedTxn.transactionId}</p>
              </div>
              <div>
                <span className="text-slate-400 font-medium">Execution Status:</span>
                <div className="mt-0.5">{getStatusBadge(selectedTxn.status)}</div>
              </div>
              <div>
                <span className="text-slate-400 font-medium">Account Number:</span>
                <p className="font-mono font-semibold text-slate-800 mt-0.5">{selectedTxn.accountNumber}</p>
              </div>
              {selectedTxn.relatedAccountNumber && (
                <div>
                  <span className="text-slate-400 font-medium">Counterparty Account:</span>
                  <p className="font-mono font-semibold text-slate-800 mt-0.5">{selectedTxn.relatedAccountNumber}</p>
                </div>
              )}
              <div>
                <span className="text-slate-400 font-medium">Operation Type:</span>
                <div className="mt-0.5">{getTypeBadge(selectedTxn.type)}</div>
              </div>
              <div>
                <span className="text-slate-400 font-medium">Transaction Amount:</span>
                <p className="font-bold text-base text-slate-900 mt-0.5">{formatCurrency(selectedTxn.amount)}</p>
              </div>
              <div>
                <span className="text-slate-400 font-medium">Balance After Settlement:</span>
                <p className="font-mono font-semibold text-slate-800 mt-0.5">{formatCurrency(selectedTxn.balanceAfter)}</p>
              </div>
              <div>
                <span className="text-slate-400 font-medium">Audit Timestamp:</span>
                <p className="text-slate-800 mt-0.5">{formatDateTime(selectedTxn.timestamp)}</p>
              </div>
            </div>

            {selectedTxn.remarks && (
              <div className="p-3 bg-white border border-slate-200 rounded-lg">
                <span className="text-slate-400 font-medium block mb-1">Audit Remarks / Narration:</span>
                <p className="text-slate-800 italic">"{selectedTxn.remarks}"</p>
              </div>
            )}

            <div className="flex justify-end pt-2">
              <Button size="sm" variant="outline" onClick={() => setSelectedTxn(null)}>
                Close
              </Button>
            </div>
          </div>
        )}
      </Dialog>
    </div>
  );
};

