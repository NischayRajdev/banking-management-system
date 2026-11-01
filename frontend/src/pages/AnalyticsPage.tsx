import React, { useEffect, useState } from 'react';
import {
  BarChart3,
  TrendingUp,
  TrendingDown,
  PieChart as PieIcon,
  ShieldAlert,
  Award,
  RefreshCw,
  Wallet,
  Activity,
  ArrowUpRight,
  ArrowDownLeft,
  ArrowLeftRight,
} from 'lucide-react';
import { api } from '../services/api';
import { AnalyticsData, Account } from '../types/banking';
import { formatCurrency } from '../utils/formatters';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  Legend,
} from 'recharts';

export const AnalyticsPage: React.FC = () => {
  const [data, setData] = useState<AnalyticsData | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const fetchAnalytics = async () => {
    setIsLoading(true);
    try {
      const res = await api.getAnalytics();
      setData(res);
    } catch {
      setData(null);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchAnalytics();
  }, []);

  const volumeComparisonData = data
    ? [
        { name: 'Deposits', volume: data.totalDepositsVolume, fill: '#10b981' },
        { name: 'Withdrawals', volume: data.totalWithdrawalsVolume, fill: '#ef4444' },
        { name: 'Transfers', volume: data.totalTransfersVolume, fill: '#2563eb' },
      ]
    : [];

  const accountDistributionData = data
    ? [
        { name: 'Savings Balance', value: data.savingsBalanceTotal, color: '#2563eb' },
        { name: 'Commercial Current', value: Math.max(0, data.currentBalanceTotal), color: '#0ea5e9' },
      ]
    : [];

  const statusDistributionData = data
    ? [
        { name: 'Success', value: data.successTransactions, color: '#10b981' },
        { name: 'Failed', value: data.failedTransactions, color: '#ef4444' },
        { name: 'Reversed', value: data.reversedTransactions, color: '#f59e0b' },
      ]
    : [];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-2 border-b border-slate-200/70">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">Financial Analytics & Risk</h1>
          <p className="text-xs text-slate-500 mt-1">
            Aggregated institutional financial indicators, volume distributions, and credit exposures.
          </p>
        </div>
        <Button size="sm" variant="outline" leftIcon={<RefreshCw className="w-3.5 h-3.5" />} onClick={fetchAnalytics} isLoading={isLoading}>
          Refresh Analytics
        </Button>
      </div>

      {/* Aggregate Volume KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-5">
        <Card>
          <CardContent className="p-4 flex items-center justify-between">
            <div>
              <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Deposits Inflow</span>
              <p className="text-xl font-bold text-emerald-700 mt-1">
                {formatCurrency(data?.totalDepositsVolume)}
              </p>
              <span className="text-[11px] text-slate-400 mt-0.5 block">Total recorded credit volume</span>
            </div>
            <div className="w-10 h-10 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center">
              <ArrowDownLeft className="w-5 h-5" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4 flex items-center justify-between">
            <div>
              <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Withdrawals Outflow</span>
              <p className="text-xl font-bold text-slate-900 mt-1">
                {formatCurrency(data?.totalWithdrawalsVolume)}
              </p>
              <span className="text-[11px] text-slate-400 mt-0.5 block">Total customer cash/ATM debits</span>
            </div>
            <div className="w-10 h-10 rounded-lg bg-rose-50 text-rose-600 flex items-center justify-center">
              <ArrowUpRight className="w-5 h-5" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4 flex items-center justify-between">
            <div>
              <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Inter-Account Transfers</span>
              <p className="text-xl font-bold text-blue-700 mt-1">
                {formatCurrency(data?.totalTransfersVolume)}
              </p>
              <span className="text-[11px] text-slate-400 mt-0.5 block">Atomic 2-party transfer volume</span>
            </div>
            <div className="w-10 h-10 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center">
              <ArrowLeftRight className="w-5 h-5" />
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Main Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Transaction Volume by Operation */}
        <Card>
          <CardHeader>
            <CardTitle>Cumulative Transaction Volume by Type</CardTitle>
            <CardDescription>Comparison of financial capital moved through deposits, debits, and transfers</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="h-64 w-full">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={volumeComparisonData} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
                  <XAxis dataKey="name" stroke="#94a3b8" fontSize={11} tickLine={false} />
                  <YAxis
                    stroke="#94a3b8"
                    fontSize={11}
                    tickLine={false}
                    tickFormatter={(v) => `₹${(v / 1000).toFixed(0)}k`}
                  />
                  <Tooltip
                    formatter={(val: any) => [formatCurrency(Number(val)), 'Volume']}
                    contentStyle={{ backgroundColor: '#ffffff', borderColor: '#e2e8f0', borderRadius: '8px', fontSize: '12px' }}
                  />
                  <Bar dataKey="volume" radius={[6, 6, 0, 0]}>
                    {volumeComparisonData.map((entry, index) => (
                      <Cell key={`bar-${index}`} fill={entry.fill} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>
          </CardContent>
        </Card>

        {/* Status Distribution */}
        <Card>
          <CardHeader>
            <CardTitle>Transaction Execution Reliability</CardTitle>
            <CardDescription>Ratio of successful versus failed or reversed ledger attempts</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="h-64 w-full flex items-center justify-center">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={statusDistributionData}
                    cx="50%"
                    cy="50%"
                    innerRadius={55}
                    outerRadius={80}
                    paddingAngle={4}
                    dataKey="value"
                  >
                    {statusDistributionData.map((entry, index) => (
                      <Cell key={`status-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip
                    formatter={(val: any, name: any) => [`${val} transactions`, name]}
                    contentStyle={{ backgroundColor: '#ffffff', borderColor: '#e2e8f0', borderRadius: '8px', fontSize: '12px' }}
                  />
                  <Legend verticalAlign="bottom" height={36} iconType="circle" wrapperStyle={{ fontSize: '12px' }} />
                </PieChart>
              </ResponsiveContainer>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Credit Risk & Overdraft Utilization Monitoring */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <div>
            <CardTitle className="flex items-center gap-2">
              <ShieldAlert className="w-4 h-4 text-amber-600" />
              Credit Facility & Overdraft Exposure Monitoring
            </CardTitle>
            <CardDescription>Commercial Current accounts currently utilizing sanctioned credit lines</CardDescription>
          </div>
          <Badge variant="warning">{data?.overdrawnAccounts?.length || 0} Overdrawn Accounts</Badge>
        </CardHeader>
        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 text-slate-600 uppercase tracking-wider font-semibold border-y border-slate-100">
                <tr>
                  <th className="py-3 px-4">Account Number</th>
                  <th className="py-3 px-4">Holder Name</th>
                  <th className="py-3 px-4">Negative Balance</th>
                  <th className="py-3 px-4">Sanctioned Overdraft</th>
                  <th className="py-3 px-4">Remaining Credit Buffer</th>
                  <th className="py-3 px-4">APR Fee Rate</th>
                  <th className="py-3 px-4">Accrued Overdraft Fee</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-slate-700">
                {data?.overdrawnAccounts && data.overdrawnAccounts.length > 0 ? (
                  data.overdrawnAccounts.map((acc) => (
                    <tr key={acc.accountNumber} className="hover:bg-slate-50/70">
                      <td className="py-3 px-4 font-mono font-medium text-slate-900">{acc.accountNumber}</td>
                      <td className="py-3 px-4 font-semibold text-slate-800">{acc.accountHolderName}</td>
                      <td className="py-3 px-4 font-mono font-bold text-rose-600">
                        {formatCurrency(acc.balance)}
                      </td>
                      <td className="py-3 px-4 font-mono">{formatCurrency(acc.overdraftLimit)}</td>
                      <td className="py-3 px-4 font-mono text-emerald-700 font-medium">
                        {formatCurrency(acc.availableOverdraft)}
                      </td>
                      <td className="py-3 px-4">{((acc.overdraftInterestRate || 0) * 100).toFixed(2)}%</td>
                      <td className="py-3 px-4 font-mono font-semibold text-amber-700">
                        {formatCurrency(acc.annualInterestAccrual)}
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={7} className="py-6 text-center text-slate-400">
                      No overdrawn accounts. All commercial credit lines are operating within positive balances.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>

      {/* Top Accounts Leaderboard */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Award className="w-4 h-4 text-blue-600" />
            Top Institutional & High-Net-Worth Accounts
          </CardTitle>
          <CardDescription>Accounts maintaining highest positive liquidity balances</CardDescription>
        </CardHeader>
        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 text-slate-600 uppercase tracking-wider font-semibold border-y border-slate-100">
                <tr>
                  <th className="py-3 px-4">Rank</th>
                  <th className="py-3 px-4">Account Number</th>
                  <th className="py-3 px-4">Holder</th>
                  <th className="py-3 px-4">Type</th>
                  <th className="py-3 px-4">Balance (₹)</th>
                  <th className="py-3 px-4">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-slate-700">
                {data?.topAccounts && data.topAccounts.length > 0 ? (
                  data.topAccounts.map((acc, idx) => (
                    <tr key={acc.accountNumber} className="hover:bg-slate-50/70">
                      <td className="py-3 px-4 font-bold text-slate-400">#{idx + 1}</td>
                      <td className="py-3 px-4 font-mono font-medium text-slate-900">{acc.accountNumber}</td>
                      <td className="py-3 px-4 font-semibold text-slate-800">{acc.accountHolderName}</td>
                      <td className="py-3 px-4">
                        <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-blue-50 text-blue-700">
                          {acc.accountType}
                        </span>
                      </td>
                      <td className="py-3 px-4 font-mono font-bold text-emerald-700">
                        {formatCurrency(acc.balance)}
                      </td>
                      <td className="py-3 px-4">
                        <Badge variant="success" size="sm">{acc.status}</Badge>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={6} className="py-6 text-center text-slate-400">No data available.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

