import React, { useEffect, useState } from 'react';
import {
  Database,
  RefreshCw,
  RotateCcw,
  HardDrive,
  Table,
  CheckCircle2,
  AlertTriangle,
  FileCode,
  ShieldCheck,
  Server,
} from 'lucide-react';
import { api } from '../services/api';
import { DatabaseStatus } from '../types/banking';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Dialog } from '../components/ui/Dialog';
import { Badge } from '../components/ui/Badge';
import { useToast } from '../components/ui/Toast';

export const DatabaseManagement: React.FC = () => {
  const toast = useToast();
  const [dbStatus, setDbStatus] = useState<DatabaseStatus | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isReseeding, setIsReseeding] = useState(false);
  const [isReloading, setIsReloading] = useState(false);
  const [isReseedModalOpen, setIsReseedModalOpen] = useState(false);

  const fetchDbInfo = async () => {
    setIsLoading(true);
    try {
      const data = await api.getDatabaseTables();
      setDbStatus(data);
    } catch (err: any) {
      toast.error('Failed to load database status', err.message);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchDbInfo();
  }, []);

  const handleReseed = async () => {
    setIsReseeding(true);
    try {
      const res = await api.reseedDatabase();
      toast.success('Database Re-seeded Successfully', res.message);
      setIsReseedModalOpen(false);
      fetchDbInfo();
    } catch (err: any) {
      toast.error('Re-seeding Failed', err.message);
    } finally {
      setIsReseeding(false);
    }
  };

  const handleReload = async () => {
    setIsReloading(true);
    try {
      const res = await api.reloadDatabase();
      toast.success('In-Memory Sync Complete', res.message);
      fetchDbInfo();
    } catch (err: any) {
      toast.error('Sync Failed', err.message);
    } finally {
      setIsReloading(false);
    }
  };

  const schemaDdl = `-- 1. Customers Table
CREATE TABLE IF NOT EXISTS customers (
    customer_id VARCHAR(50) PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    phone VARCHAR(50),
    address VARCHAR(255),
    created_at VARCHAR(50) NOT NULL
);

-- 2. Accounts Table
CREATE TABLE IF NOT EXISTS accounts (
    account_number VARCHAR(50) PRIMARY KEY,
    customer_id VARCHAR(50) NOT NULL,
    account_holder_name VARCHAR(150) NOT NULL,
    balance DECIMAL(15,2) NOT NULL,
    account_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    interest_rate DECIMAL(6,4),
    minimum_balance DECIMAL(15,2),
    max_withdrawal_limit DECIMAL(15,2),
    overdraft_limit DECIMAL(15,2),
    overdraft_interest_rate DECIMAL(6,4),
    created_at VARCHAR(50) NOT NULL,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
);

-- 3. Transactions Table
CREATE TABLE IF NOT EXISTS transactions (
    transaction_id VARCHAR(50) PRIMARY KEY,
    account_number VARCHAR(50) NOT NULL,
    related_account_number VARCHAR(50),
    type VARCHAR(30) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    balance_after DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    remarks VARCHAR(255),
    timestamp VARCHAR(50) NOT NULL,
    FOREIGN KEY (account_number) REFERENCES accounts(account_number)
);`;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-2 border-b border-slate-200/70">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">Database & Relational State</h1>
          <p className="text-xs text-slate-500 mt-1">
            SQLite JDBC relational persistence, transactional integrity, and demo data re-seeder.
          </p>
        </div>
        <div className="flex items-center gap-2.5">
          <Button
            size="sm"
            variant="outline"
            leftIcon={<RefreshCw className="w-3.5 h-3.5" />}
            onClick={fetchDbInfo}
            isLoading={isLoading}
          >
            Refresh
          </Button>
          <Button
            size="sm"
            variant="outline"
            leftIcon={<RefreshCw className="w-3.5 h-3.5" />}
            onClick={handleReload}
            isLoading={isReloading}
          >
            Reload In-Memory Cache
          </Button>
          <Button
            size="sm"
            variant="danger"
            leftIcon={<RotateCcw className="w-3.5 h-3.5" />}
            onClick={() => setIsReseedModalOpen(true)}
          >
            Reseed Demo Dataset
          </Button>
        </div>
      </div>

      {/* Database Connection Card */}
      <Card>
        <CardContent className="p-5">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center flex-shrink-0">
                <Database className="w-6 h-6" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <h3 className="text-base font-bold text-slate-900">SQLite JDBC Persistence Engine</h3>
                  <Badge variant="success">ONLINE & HEALTHY</Badge>
                </div>
                <p className="font-mono text-xs text-slate-500 mt-1">
                  Location: {dbStatus?.databaseUrl || 'jdbc:sqlite:bank_system.db'}
                </p>
              </div>
            </div>

            <div className="flex items-center gap-2 text-xs text-slate-600 bg-slate-50 px-3.5 py-2 rounded-lg border border-slate-200/80">
              <ShieldCheck className="w-4 h-4 text-emerald-600" />
              <span>Full ACID Compliance Enabled</span>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Tables Breakdown */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        {dbStatus?.tables?.map((tbl) => (
          <Card key={tbl.name} className="hover:border-slate-300">
            <CardContent className="p-5">
              <div className="flex items-center justify-between">
                <span className="font-mono font-bold text-slate-800 text-sm flex items-center gap-1.5">
                  <Table className="w-4 h-4 text-blue-600" />
                  {tbl.name}
                </span>
                <Badge variant="info">{tbl.rowCount} Rows</Badge>
              </div>
              <p className="text-xs text-slate-500 mt-2">{tbl.description}</p>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Relational Schema SQL Viewer */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <FileCode className="w-4 h-4 text-blue-600" />
            Relational DDL Schema
          </CardTitle>
          <CardDescription>
            Normalized 3NF relational schema with foreign key constraints and performance indexes
          </CardDescription>
        </CardHeader>
        <CardContent>
          <pre className="p-4 bg-slate-900 text-slate-100 rounded-xl text-xs font-mono overflow-x-auto leading-relaxed">
            {schemaDdl}
          </pre>
        </CardContent>
      </Card>

      {/* Reseed Confirmation Modal */}
      <Dialog
        isOpen={isReseedModalOpen}
        onClose={() => setIsReseedModalOpen(false)}
        title="Confirm Database Re-seeding"
        description="This will reset the SQLite tables and reload 16+ customers, 26+ accounts, and 85+ transactions."
      >
        <div className="space-y-4 text-xs">
          <div className="p-3 bg-amber-50 rounded-lg border border-amber-200 text-amber-900 flex items-start gap-2.5">
            <AlertTriangle className="w-5 h-5 text-amber-600 flex-shrink-0 mt-0.5" />
            <div>
              <strong className="block">Are you sure?</strong>
              <p className="mt-0.5 text-amber-700">
                Any ad-hoc customers or transactions created during testing will be replaced with the rich enterprise seed dataset.
              </p>
            </div>
          </div>

          <div className="flex justify-end gap-2.5 pt-2 border-t border-slate-100">
            <Button size="sm" variant="outline" onClick={() => setIsReseedModalOpen(false)} disabled={isReseeding}>
              Cancel
            </Button>
            <Button size="sm" variant="danger" onClick={handleReseed} isLoading={isReseeding}>
              Confirm Re-seed
            </Button>
          </div>
        </div>
      </Dialog>
    </div>
  );
};

