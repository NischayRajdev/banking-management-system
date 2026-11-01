import React, { useState, useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import {
  Search,
  Bell,
  Calendar,
  User,
  CheckCircle2,
  AlertTriangle,
  ChevronRight,
  LogOut,
  Shield,
  HelpCircle,
} from 'lucide-react';
import { api } from '../../services/api';

interface TopNavProps {
  collapsed: boolean;
}

export const TopNav: React.FC<TopNavProps> = ({ collapsed }) => {
  const location = useLocation();
  const navigate = useNavigate();
  const [searchQuery, setSearchQuery] = useState('');
  const [isNotificationsOpen, setIsNotificationsOpen] = useState(false);
  const [isProfileOpen, setIsProfileOpen] = useState(false);
  const [isOnline, setIsOnline] = useState<boolean | null>(null);

  // Determine current page title & breadcrumb
  const path = location.pathname;
  let pageTitle = 'Dashboard Overview';
  let section = 'Core';

  if (path.startsWith('/customers')) {
    pageTitle = 'Customer Directory';
    section = 'Customers';
  } else if (path.startsWith('/accounts')) {
    pageTitle = 'Account Management';
    section = 'Accounts';
  } else if (path.startsWith('/transactions')) {
    pageTitle = 'Financial Transactions';
    section = 'Ledger';
  } else if (path.startsWith('/analytics')) {
    pageTitle = 'Financial Analytics & Risk';
    section = 'Reports';
  } else if (path.startsWith('/database')) {
    pageTitle = 'Database & Relational State';
    section = 'System';
  } else if (path.startsWith('/settings')) {
    pageTitle = 'Branch & System Settings';
    section = 'Settings';
  }

  // Health check polling
  useEffect(() => {
    let isMounted = true;
    const checkHealth = async () => {
      try {
        await api.getHealth();
        if (isMounted) setIsOnline(true);
      } catch {
        if (isMounted) setIsOnline(false);
      }
    };
    checkHealth();
    const interval = setInterval(checkHealth, 30000);
    return () => {
      isMounted = false;
      clearInterval(interval);
    };
  }, []);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!searchQuery.trim()) return;
    const q = searchQuery.trim();
    // Quick heuristic: if starts with SAV- or CUR-, jump to accounts; if CUST-, jump to customers
    if (q.toUpperCase().startsWith('SAV-') || q.toUpperCase().startsWith('CUR-')) {
      navigate(`/accounts?q=${encodeURIComponent(q)}`);
    } else {
      navigate(`/customers?q=${encodeURIComponent(q)}`);
    }
  };

  const todayFormatted = new Intl.DateTimeFormat('en-IN', {
    weekday: 'short',
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  }).format(new Date());

  return (
    <header
      className={`fixed top-0 right-0 z-20 h-16 bg-white/90 backdrop-blur-md border-b border-slate-200/90 transition-all duration-300 px-6 flex items-center justify-between ${
        collapsed ? 'left-20' : 'left-64'
      }`}
    >
      {/* Page Title & Breadcrumb */}
      <div>
        <div className="flex items-center gap-1.5 text-xs text-slate-500 font-medium">
          <span>India Bank</span>
          <ChevronRight className="w-3.5 h-3.5 text-slate-400" />
          <span>{section}</span>
          <ChevronRight className="w-3.5 h-3.5 text-slate-400" />
          <span className="text-slate-800 font-semibold">{pageTitle}</span>
        </div>
        <h2 className="text-base font-bold text-slate-900 tracking-tight">{pageTitle}</h2>
      </div>

      {/* Center Global Search */}
      <div className="hidden md:block w-72 lg:w-96">
        <form onSubmit={handleSearchSubmit}>
          <div className="relative">
            <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search customer, account or ID..."
              className="w-full bg-slate-50 hover:bg-slate-100/80 focus:bg-white text-xs text-slate-900 pl-9 pr-4 py-2 rounded-lg border border-slate-200 focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 transition-all placeholder:text-slate-400"
            />
          </div>
        </form>
      </div>

      {/* Right Controls */}
      <div className="flex items-center gap-3">
        {/* Backend Online Status Pill */}
        <div
          className={`hidden sm:flex items-center gap-1.5 text-xs px-2.5 py-1 rounded-full border ${
            isOnline
              ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
              : isOnline === false
              ? 'bg-rose-50 text-rose-700 border-rose-200'
              : 'bg-slate-50 text-slate-600 border-slate-200'
          }`}
          title={isOnline ? 'REST API Connected' : 'Cannot reach backend'}
        >
          <span
            className={`w-2 h-2 rounded-full ${
              isOnline ? 'bg-emerald-500 animate-pulse' : 'bg-rose-500'
            }`}
          />
          <span className="font-medium text-[11px]">
            {isOnline ? 'Core API Online' : isOnline === false ? 'Core Offline' : 'Connecting...'}
          </span>
        </div>

        {/* Current Date */}
        <div className="hidden lg:flex items-center gap-1.5 text-xs text-slate-600 bg-slate-50 px-3 py-1.5 rounded-lg border border-slate-200/60 font-medium">
          <Calendar className="w-3.5 h-3.5 text-slate-400" />
          <span>{todayFormatted}</span>
        </div>

        {/* Notifications Icon & Popover */}
        <div className="relative">
          <button
            onClick={() => setIsNotificationsOpen(!isNotificationsOpen)}
            className="p-2 rounded-lg text-slate-500 hover:text-slate-800 hover:bg-slate-100 relative transition-colors"
            title="Notifications"
          >
            <Bell className="w-4 h-4" />
            <span className="absolute top-1.5 right-1.5 w-2 h-2 rounded-full bg-blue-600" />
          </button>

          {isNotificationsOpen && (
            <div className="absolute right-0 mt-2 w-80 bg-white rounded-xl border border-slate-200 shadow-xl p-4 z-40 animate-fade-in">
              <div className="flex items-center justify-between pb-2 border-b border-slate-100">
                <h4 className="text-xs font-bold uppercase tracking-wider text-slate-600">
                  System Alerts
                </h4>
                <span className="text-[10px] bg-blue-50 text-blue-700 font-semibold px-2 py-0.5 rounded-full">
                  2 New
                </span>
              </div>
              <div className="mt-2 space-y-2 text-xs">
                <div className="p-2.5 rounded-lg bg-emerald-50/70 border border-emerald-100 flex gap-2">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600 flex-shrink-0 mt-0.5" />
                  <div>
                    <p className="font-semibold text-slate-800">Database Seed Synchronized</p>
                    <p className="text-[11px] text-slate-500 mt-0.5">
                      30+ accounts & 85+ ledger items initialized.
                    </p>
                  </div>
                </div>
                <div className="p-2.5 rounded-lg bg-amber-50/70 border border-amber-100 flex gap-2">
                  <AlertTriangle className="w-4 h-4 text-amber-600 flex-shrink-0 mt-0.5" />
                  <div>
                    <p className="font-semibold text-slate-800">Overdraft Risk Monitored</p>
                    <p className="text-[11px] text-slate-500 mt-0.5">
                      Commercial accounts in active credit facility.
                    </p>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Profile Dropdown */}
        <div className="relative">
          <button
            onClick={() => setIsProfileOpen(!isProfileOpen)}
            className="flex items-center gap-2 p-1.5 rounded-lg hover:bg-slate-100 transition-colors"
          >
            <div className="w-8 h-8 rounded-full bg-slate-900 text-white flex items-center justify-center text-xs font-semibold shadow-xs">
              BM
            </div>
            <div className="hidden sm:block text-left">
              <p className="text-xs font-semibold text-slate-800 leading-tight">Branch Manager</p>
              <p className="text-[10px] text-slate-500">INDB-001</p>
            </div>
          </button>

          {isProfileOpen && (
            <div className="absolute right-0 mt-2 w-56 bg-white rounded-xl border border-slate-200 shadow-xl p-2 z-40 animate-fade-in text-xs">
              <div className="px-3 py-2 border-b border-slate-100">
                <p className="font-semibold text-slate-900">Branch Operations Officer</p>
                <p className="text-[11px] text-slate-500 truncate">manager@indiabank.co.in</p>
              </div>
              <div className="py-1">
                <button
                  onClick={() => {
                    navigate('/settings');
                    setIsProfileOpen(false);
                  }}
                  className="w-full flex items-center gap-2 px-3 py-2 rounded-md hover:bg-slate-50 text-slate-700"
                >
                  <Shield className="w-3.5 h-3.5 text-slate-400" />
                  Branch Security & Roles
                </button>
                <button
                  onClick={() => {
                    navigate('/database');
                    setIsProfileOpen(false);
                  }}
                  className="w-full flex items-center gap-2 px-3 py-2 rounded-md hover:bg-slate-50 text-slate-700"
                >
                  <HelpCircle className="w-3.5 h-3.5 text-slate-400" />
                  Relational Diagnostics
                </button>
              </div>
              <div className="pt-1 border-t border-slate-100">
                <button
                  onClick={() => setIsProfileOpen(false)}
                  className="w-full flex items-center gap-2 px-3 py-2 rounded-md hover:bg-rose-50 text-rose-600 font-medium"
                >
                  <LogOut className="w-3.5 h-3.5 text-rose-500" />
                  Terminal Console Mode
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </header>
  );
};

