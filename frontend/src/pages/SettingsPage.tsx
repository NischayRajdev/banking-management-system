import React, { useEffect, useState } from 'react';
import {
  Settings,
  Building2,
  Cpu,
  Shield,
  Zap,
  CheckCircle2,
  AlertCircle,
  RefreshCw,
  Server,
  DollarSign,
} from 'lucide-react';
import { api } from '../services/api';
import { HealthInfo } from '../types/banking';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { useToast } from '../components/ui/Toast';

export const SettingsPage: React.FC = () => {
  const toast = useToast();
  const [health, setHealth] = useState<HealthInfo | null>(null);
  const [pingMs, setPingMs] = useState<number | null>(null);
  const [isPinging, setIsPinging] = useState(false);

  const testPing = async () => {
    setIsPinging(true);
    const start = performance.now();
    try {
      const data = await api.getHealth();
      const end = performance.now();
      setHealth(data);
      setPingMs(Math.round(end - start));
      toast.success('Core API Online', `Response received in ${Math.round(end - start)}ms`);
    } catch (err: any) {
      setPingMs(null);
      toast.error('Ping Failed', err.message);
    } finally {
      setIsPinging(false);
    }
  };

  useEffect(() => {
    testPing();
  }, []);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="pb-2 border-b border-slate-200/70">
        <h1 className="text-2xl font-bold tracking-tight text-slate-900">Branch & System Settings</h1>
        <p className="text-xs text-slate-500 mt-1">
          Branch operational parameters, currency localization, and backend core health diagnostics.
        </p>
      </div>

      {/* Branch Profile */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Building2 className="w-4 h-4 text-blue-600" />
            Branch Profile & Configuration
          </CardTitle>
          <CardDescription>Primary operating branch identity and currency denomination</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4 text-xs">
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 p-4 bg-slate-50 rounded-xl border border-slate-200/80">
            <div>
              <span className="text-slate-400 font-medium">Institution Name:</span>
              <p className="font-bold text-slate-900 mt-0.5">India Bank (INDB)</p>
            </div>
            <div>
              <span className="text-slate-400 font-medium">Assigned Branch Code:</span>
              <p className="font-mono font-bold text-blue-700 mt-0.5">INDB-001</p>
            </div>
            <div>
              <span className="text-slate-400 font-medium">Branch Location:</span>
              <p className="font-medium text-slate-800 mt-0.5">Nariman Point Financial Hub, Mumbai, MH</p>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="p-3 bg-white rounded-lg border border-slate-200">
              <span className="text-slate-400 font-medium block">Currency Denomination:</span>
              <span className="text-sm font-bold text-slate-900 mt-1 block">Indian Rupee (INR / ₹)</span>
              <span className="text-[11px] text-slate-500 mt-0.5 block">Standard Indian numbering system</span>
            </div>
            <div className="p-3 bg-white rounded-lg border border-slate-200">
              <span className="text-slate-400 font-medium block">Default Savings Rate:</span>
              <span className="text-sm font-bold text-blue-700 mt-1 block">4.50% APY</span>
              <span className="text-[11px] text-slate-500 mt-0.5 block">Compound annual accrual</span>
            </div>
            <div className="p-3 bg-white rounded-lg border border-slate-200">
              <span className="text-slate-400 font-medium block">Commercial Overdraft APR:</span>
              <span className="text-sm font-bold text-sky-700 mt-1 block">10.00% - 12.00% APR</span>
              <span className="text-[11px] text-slate-500 mt-0.5 block">Assessed on negative balances</span>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Backend Engine Diagnostics & Health */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <div>
            <CardTitle className="flex items-center gap-2">
              <Server className="w-4 h-4 text-emerald-600" />
              Core Engine Diagnostics
            </CardTitle>
            <CardDescription>Live heartbeat probe to Java REST API server</CardDescription>
          </div>
          <Button size="sm" variant="outline" leftIcon={<Zap className="w-3.5 h-3.5" />} onClick={testPing} isLoading={isPinging}>
            Ping Core API
          </Button>
        </CardHeader>
        <CardContent className="space-y-4 text-xs">
          <div className="grid grid-cols-1 sm:grid-cols-4 gap-4 p-4 bg-slate-50 rounded-xl border border-slate-200/80">
            <div>
              <span className="text-slate-400 font-medium">Core Status:</span>
              <div className="mt-1">
                <Badge variant={health?.status === 'UP' ? 'success' : 'danger'}>
                  {health?.status === 'UP' ? 'ONLINE (UP)' : 'DISCONNECTED'}
                </Badge>
              </div>
            </div>
            <div>
              <span className="text-slate-400 font-medium">Latency / RTT:</span>
              <p className="font-mono font-bold text-slate-900 mt-1">
                {pingMs !== null ? `${pingMs} ms` : '-'}
              </p>
            </div>
            <div>
              <span className="text-slate-400 font-medium">API Endpoint:</span>
              <p className="font-mono text-slate-800 mt-1">http://localhost:8080/api</p>
            </div>
            <div>
              <span className="text-slate-400 font-medium">Core Server Timestamp:</span>
              <p className="text-slate-800 mt-1 font-mono">{health?.serverTime || '-'}</p>
            </div>
          </div>

          <div className="p-4 bg-slate-50 rounded-xl border border-slate-200/80">
            <h4 className="font-bold text-slate-900 mb-2">Architectural Highlights</h4>
            <ul className="space-y-1.5 text-slate-600 list-disc list-inside text-xs">
              <li><strong>Zero-Bypass OOP Backend:</strong> All transactions and account rules run through compiled Java 21 classes (`SavingsAccount`, `CurrentAccount`, `Bank`).</li>
              <li><strong>ACID Transaction Locking:</strong> Inter-account fund transfers use programmatic SQLite JDBC rollback guards (`conn.setAutoCommit(false)`).</li>
              <li><strong>Defensive Encapsulation:</strong> Private balances and mutators prevent direct or unauthorized state tampering.</li>
              <li><strong>Zero External Runtime Dependencies:</strong> HTTP server runs directly on Java 21's native `com.sun.net.httpserver` with zero overhead.</li>
            </ul>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

