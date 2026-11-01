import React, { useEffect, useState } from 'react';
import {
  Users,
  UserPlus,
  Search,
  RefreshCw,
  Wallet,
  Phone,
  Mail,
  MapPin,
  Calendar,
  Edit2,
  ExternalLink,
  PlusCircle,
  AlertCircle,
  ArrowUpDown,
} from 'lucide-react';
import { api } from '../services/api';
import { Customer, Account } from '../types/banking';
import { formatCurrency, formatDateTime } from '../utils/formatters';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Dialog } from '../components/ui/Dialog';
import { Badge } from '../components/ui/Badge';
import { useToast } from '../components/ui/Toast';

export const CustomerManagement: React.FC = () => {
  const toast = useToast();
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [sortField, setSortField] = useState<'customerId' | 'fullName' | 'totalPortfolioBalance'>('customerId');
  const [sortAsc, setSortAsc] = useState(true);
  const [page, setPage] = useState(1);
  const pageSize = 10;

  // Modals state
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [selectedCustomer, setSelectedCustomer] = useState<Customer | null>(null);
  const [customerAccounts, setCustomerAccounts] = useState<Account[]>([]);
  const [isAccountsLoading, setIsAccountsLoading] = useState(false);

  // Form states
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    address: '',
  });
  const [formErrors, setFormErrors] = useState<Record<string, string>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  const fetchCustomers = async (query = '') => {
    setIsLoading(true);
    try {
      const data = await api.getCustomers(query);
      setCustomers(data);
    } catch (err: any) {
      toast.error('Error fetching customers', err.message);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchCustomers(searchQuery);
  }, []);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(1);
    fetchCustomers(searchQuery);
  };

  const handleOpenDetails = async (cust: Customer) => {
    setSelectedCustomer(cust);
    setIsAccountsLoading(true);
    try {
      const accounts = await api.getCustomerAccounts(cust.customerId);
      setCustomerAccounts(accounts);
    } catch {
      setCustomerAccounts([]);
    } finally {
      setIsAccountsLoading(false);
    }
  };

  const handleOpenEdit = (cust: Customer) => {
    setSelectedCustomer(cust);
    setFormData({
      firstName: cust.firstName,
      lastName: cust.lastName,
      email: cust.email,
      phone: cust.phoneNumber,
      address: cust.address,
    });
    setFormErrors({});
    setIsEditModalOpen(true);
  };

  const validateForm = () => {
    const errors: Record<string, string> = {};
    if (!formData.firstName.trim()) errors.firstName = 'First name is mandatory.';
    if (!formData.lastName.trim()) errors.lastName = 'Last name is mandatory.';
    if (!formData.email.trim()) {
      errors.email = 'Email address is mandatory.';
    } else if (!formData.email.includes('@')) {
      errors.email = 'Must be a valid email format containing @.';
    }
    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleCreateCustomer = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validateForm()) return;

    setIsSubmitting(true);
    try {
      const newCust = await api.createCustomer(formData);
      toast.success('Customer Registered Successfully', `${newCust.fullName} assigned ID: ${newCust.customerId}`);
      setIsAddModalOpen(false);
      setFormData({ firstName: '', lastName: '', email: '', phone: '', address: '' });
      fetchCustomers();
    } catch (err: any) {
      toast.error('Registration Failed', err.message);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleUpdateCustomer = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedCustomer) return;

    setIsSubmitting(true);
    try {
      const updated = await api.updateCustomer(selectedCustomer.customerId, {
        firstName: formData.firstName,
        lastName: formData.lastName,
        phone: formData.phone,
        address: formData.address,
      });
      toast.success('Customer Profile Updated', `Changes saved for ${updated.fullName}`);
      setIsEditModalOpen(false);
      fetchCustomers();
      if (selectedCustomer && selectedCustomer.customerId === updated.customerId) {
        setSelectedCustomer({ ...selectedCustomer, ...updated });
      }
    } catch (err: any) {
      toast.error('Update Failed', err.message);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Sorting & Pagination
  const sortedCustomers = [...customers].sort((a, b) => {
    let comp = 0;
    if (sortField === 'customerId') comp = a.customerId.localeCompare(b.customerId);
    else if (sortField === 'fullName') comp = a.fullName.localeCompare(b.fullName);
    else if (sortField === 'totalPortfolioBalance') comp = a.totalPortfolioBalance - b.totalPortfolioBalance;
    return sortAsc ? comp : -comp;
  });

  const totalPages = Math.max(1, Math.ceil(sortedCustomers.length / pageSize));
  const paginatedCustomers = sortedCustomers.slice((page - 1) * pageSize, page * pageSize);

  const totalPortfolioWealth = customers.reduce((sum, c) => sum + (c.totalPortfolioBalance || 0), 0);

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-2 border-b border-slate-200/70">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">Customer Management</h1>
          <p className="text-xs text-slate-500 mt-1">
            Registered customer portfolio aggregates, KYC profiles, and associated banking accounts.
          </p>
        </div>
        <div className="flex items-center gap-2.5">
          <Button
            size="sm"
            variant="outline"
            leftIcon={<RefreshCw className="w-3.5 h-3.5" />}
            onClick={() => fetchCustomers(searchQuery)}
            isLoading={isLoading}
          >
            Refresh
          </Button>
          <Button
            size="sm"
            variant="primary"
            leftIcon={<UserPlus className="w-4 h-4" />}
            onClick={() => {
              setFormData({ firstName: '', lastName: '', email: '', phone: '', address: '' });
              setFormErrors({});
              setIsAddModalOpen(true);
            }}
          >
            Register New Customer
          </Button>
        </div>
      </div>

      {/* Summary KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-5">
        <Card>
          <CardContent className="p-4 flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Total Onboarded</p>
              <p className="text-xl font-bold text-slate-900 mt-1">{customers.length} Customers</p>
            </div>
            <div className="w-10 h-10 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center">
              <Users className="w-5 h-5" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4 flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Combined Net Worth</p>
              <p className="text-xl font-bold text-emerald-700 mt-1">{formatCurrency(totalPortfolioWealth)}</p>
            </div>
            <div className="w-10 h-10 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center">
              <Wallet className="w-5 h-5" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4 flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Average Portfolio</p>
              <p className="text-xl font-bold text-slate-900 mt-1">
                {formatCurrency(customers.length ? totalPortfolioWealth / customers.length : 0)}
              </p>
            </div>
            <div className="w-10 h-10 rounded-lg bg-indigo-50 text-indigo-600 flex items-center justify-center">
              <Users className="w-5 h-5" />
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Customers Table Card */}
      <Card>
        <CardHeader className="p-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <form onSubmit={handleSearch} className="flex-1 max-w-md">
              <div className="relative">
                <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="Filter by name, ID, email or phone..."
                  className="w-full bg-slate-50 focus:bg-white text-xs text-slate-900 pl-9 pr-4 py-2 rounded-lg border border-slate-200 focus:outline-none focus:border-blue-500 transition-colors"
                />
              </div>
            </form>

            <div className="text-xs text-slate-500 font-medium">
              Showing {paginatedCustomers.length} of {customers.length} profiles
            </div>
          </div>
        </CardHeader>

        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 text-slate-600 uppercase tracking-wider font-semibold border-y border-slate-100">
                <tr>
                  <th
                    className="py-3 px-4 cursor-pointer hover:text-blue-600"
                    onClick={() => {
                      if (sortField === 'customerId') setSortAsc(!sortAsc);
                      else {
                        setSortField('customerId');
                        setSortAsc(true);
                      }
                    }}
                  >
                    <div className="flex items-center gap-1">
                      <span>Customer ID</span>
                      <ArrowUpDown className="w-3 h-3 text-slate-400" />
                    </div>
                  </th>
                  <th
                    className="py-3 px-4 cursor-pointer hover:text-blue-600"
                    onClick={() => {
                      if (sortField === 'fullName') setSortAsc(!sortAsc);
                      else {
                        setSortField('fullName');
                        setSortAsc(true);
                      }
                    }}
                  >
                    <div className="flex items-center gap-1">
                      <span>Full Name</span>
                      <ArrowUpDown className="w-3 h-3 text-slate-400" />
                    </div>
                  </th>
                  <th className="py-3 px-4">Contact Info</th>
                  <th className="py-3 px-4">Address</th>
                  <th className="py-3 px-4">Accounts</th>
                  <th
                    className="py-3 px-4 cursor-pointer hover:text-blue-600"
                    onClick={() => {
                      if (sortField === 'totalPortfolioBalance') setSortAsc(!sortAsc);
                      else {
                        setSortField('totalPortfolioBalance');
                        setSortAsc(false);
                      }
                    }}
                  >
                    <div className="flex items-center gap-1">
                      <span>Portfolio Balance (₹)</span>
                      <ArrowUpDown className="w-3 h-3 text-slate-400" />
                    </div>
                  </th>
                  <th className="py-3 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-slate-700">
                {paginatedCustomers.length > 0 ? (
                  paginatedCustomers.map((cust) => (
                    <tr key={cust.customerId} className="hover:bg-slate-50/70 transition-colors">
                      <td className="py-3 px-4 font-mono font-medium text-slate-900">
                        {cust.customerId}
                      </td>
                      <td className="py-3 px-4 font-semibold text-slate-900">
                        <button
                          onClick={() => handleOpenDetails(cust)}
                          className="hover:text-blue-600 text-left hover:underline"
                        >
                          {cust.fullName}
                        </button>
                      </td>
                      <td className="py-3 px-4 text-slate-600">
                        <div className="truncate max-w-[180px]">{cust.email}</div>
                        <div className="text-[11px] text-slate-400">{cust.phoneNumber || '-'}</div>
                      </td>
                      <td className="py-3 px-4 text-slate-600 truncate max-w-[180px]">
                        {cust.address || '-'}
                      </td>
                      <td className="py-3 px-4">
                        <Badge variant={cust.accountCount > 0 ? 'info' : 'neutral'} size="sm">
                          {cust.accountCount} {cust.accountCount === 1 ? 'acc' : 'accs'}
                        </Badge>
                      </td>
                      <td className="py-3 px-4 font-mono font-semibold text-slate-900">
                        {formatCurrency(cust.totalPortfolioBalance)}
                      </td>
                      <td className="py-3 px-4 text-right space-x-2">
                        <button
                          onClick={() => handleOpenDetails(cust)}
                          className="text-blue-600 hover:text-blue-800 font-semibold hover:underline"
                        >
                          View
                        </button>
                        <button
                          onClick={() => handleOpenEdit(cust)}
                          className="text-slate-600 hover:text-slate-900 font-semibold hover:underline"
                        >
                          Edit
                        </button>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={7} className="py-8 text-center text-slate-400">
                      No matching customers found.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          {/* Pagination Controls */}
          {totalPages > 1 && (
            <div className="p-4 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
              <span>
                Page {page} of {totalPages}
              </span>
              <div className="flex gap-1.5">
                <Button
                  size="sm"
                  variant="outline"
                  disabled={page === 1}
                  onClick={() => setPage((p) => Math.max(1, p - 1))}
                >
                  Previous
                </Button>
                <Button
                  size="sm"
                  variant="outline"
                  disabled={page === totalPages}
                  onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
                >
                  Next
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Customer Details Modal */}
      <Dialog
        isOpen={!!selectedCustomer && !isEditModalOpen}
        onClose={() => setSelectedCustomer(null)}
        title="Customer Profile & Accounts Aggregate"
        description="Comprehensive customer profile and active portfolio balances."
        maxWidth="lg"
      >
        {selectedCustomer && (
          <div className="space-y-5 text-xs">
            {/* Demographics Banner */}
            <div className="p-4 bg-slate-50 rounded-xl border border-slate-200/80 space-y-3">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div>
                  <h4 className="text-base font-bold text-slate-900">{selectedCustomer.fullName}</h4>
                  <p className="font-mono text-slate-500 mt-0.5">ID: {selectedCustomer.customerId}</p>
                </div>
                <div className="text-right">
                  <span className="text-slate-500 font-medium">Total Portfolio Balance:</span>
                  <p className="text-lg font-bold text-emerald-700">
                    {formatCurrency(selectedCustomer.totalPortfolioBalance)}
                  </p>
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5 pt-2 border-t border-slate-200/60">
                <div className="flex items-center gap-2 text-slate-700">
                  <Mail className="w-3.5 h-3.5 text-slate-400" />
                  <span className="truncate">{selectedCustomer.email}</span>
                </div>
                <div className="flex items-center gap-2 text-slate-700">
                  <Phone className="w-3.5 h-3.5 text-slate-400" />
                  <span>{selectedCustomer.phoneNumber || 'No phone'}</span>
                </div>
                <div className="flex items-center gap-2 text-slate-700">
                  <MapPin className="w-3.5 h-3.5 text-slate-400" />
                  <span className="truncate">{selectedCustomer.address || 'No address'}</span>
                </div>
              </div>
            </div>

            {/* Associated Accounts Section */}
            <div>
              <div className="flex items-center justify-between mb-2">
                <h5 className="font-bold text-slate-900 uppercase tracking-wider text-[11px]">
                  Associated Accounts ({customerAccounts.length})
                </h5>
              </div>

              {isAccountsLoading ? (
                <div className="p-6 text-center text-slate-400">Loading accounts...</div>
              ) : customerAccounts.length > 0 ? (
                <div className="space-y-2.5">
                  {customerAccounts.map((acc) => (
                    <div
                      key={acc.accountNumber}
                      className="p-3 bg-white rounded-lg border border-slate-200 shadow-xs flex items-center justify-between"
                    >
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="font-mono font-semibold text-slate-900">
                            {acc.accountNumber}
                          </span>
                          <span
                            className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                              acc.accountType === 'SAVINGS'
                                ? 'bg-blue-50 text-blue-700'
                                : 'bg-sky-50 text-sky-700'
                            }`}
                          >
                            {acc.accountType}
                          </span>
                          <span
                            className={`px-1.5 py-0.5 rounded text-[10px] font-medium ${
                              acc.status === 'ACTIVE'
                                ? 'bg-emerald-50 text-emerald-700'
                                : 'bg-amber-50 text-amber-700'
                            }`}
                          >
                            {acc.status}
                          </span>
                        </div>
                        <p className="text-[11px] text-slate-500 mt-1">
                          {acc.accountType === 'SAVINGS'
                            ? `Interest Rate: ${(acc.interestRate! * 100).toFixed(2)}% • Min Bal: ${formatCurrency(acc.minimumBalance)}`
                            : `Overdraft Limit: ${formatCurrency(acc.overdraftLimit)} • Available: ${formatCurrency(acc.availableOverdraft)}`}
                        </p>
                      </div>

                      <div className="text-right">
                        <span className="text-[10px] text-slate-400 block">Balance</span>
                        <span className="font-mono font-bold text-sm text-slate-900">
                          {formatCurrency(acc.balance)}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="p-4 bg-slate-50 border border-dashed border-slate-200 rounded-lg text-center text-slate-500">
                  No bank accounts opened for this customer yet.
                </div>
              )}
            </div>

            <div className="flex justify-between pt-3 border-t border-slate-100">
              <Button
                size="sm"
                variant="outline"
                leftIcon={<Edit2 className="w-3.5 h-3.5" />}
                onClick={() => handleOpenEdit(selectedCustomer)}
              >
                Edit Profile
              </Button>
              <Button size="sm" variant="primary" onClick={() => setSelectedCustomer(null)}>
                Done
              </Button>
            </div>
          </div>
        )}
      </Dialog>

      {/* Register Customer Modal */}
      <Dialog
        isOpen={isAddModalOpen}
        onClose={() => setIsAddModalOpen(false)}
        title="Register New Customer"
        description="Onboard an individual customer into India Bank relational core."
      >
        <form onSubmit={handleCreateCustomer} className="space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <Input
              label="First Name"
              required
              value={formData.firstName}
              onChange={(e) => setFormData({ ...formData, firstName: e.target.value })}
              error={formErrors.firstName}
              placeholder="e.g. Aarav"
            />
            <Input
              label="Last Name"
              required
              value={formData.lastName}
              onChange={(e) => setFormData({ ...formData, lastName: e.target.value })}
              error={formErrors.lastName}
              placeholder="e.g. Sharma"
            />
          </div>

          <Input
            label="Email Address (Mandatory Unique Identifier)"
            type="email"
            required
            value={formData.email}
            onChange={(e) => setFormData({ ...formData, email: e.target.value })}
            error={formErrors.email}
            placeholder="e.g. aarav.sharma@example.com"
          />

          <Input
            label="Phone Number"
            value={formData.phone}
            onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
            placeholder="e.g. +91-98201-10001"
          />

          <Input
            label="Residential / Business Address"
            value={formData.address}
            onChange={(e) => setFormData({ ...formData, address: e.target.value })}
            placeholder="e.g. 123 Marine Drive, Mumbai, MH"
          />

          <div className="flex items-center justify-end gap-2.5 pt-3 border-t border-slate-100">
            <Button size="sm" variant="outline" type="button" onClick={() => setIsAddModalOpen(false)}>
              Cancel
            </Button>
            <Button size="sm" variant="primary" type="submit" isLoading={isSubmitting}>
              Register Customer
            </Button>
          </div>
        </form>
      </Dialog>

      {/* Edit Customer Modal */}
      <Dialog
        isOpen={isEditModalOpen}
        onClose={() => setIsEditModalOpen(false)}
        title="Edit Customer Profile"
        description={`Update contact and demographic details for ${selectedCustomer?.customerId}.`}
      >
        <form onSubmit={handleUpdateCustomer} className="space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <Input
              label="First Name"
              required
              value={formData.firstName}
              onChange={(e) => setFormData({ ...formData, firstName: e.target.value })}
              error={formErrors.firstName}
            />
            <Input
              label="Last Name"
              required
              value={formData.lastName}
              onChange={(e) => setFormData({ ...formData, lastName: e.target.value })}
              error={formErrors.lastName}
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1.5">Email (Immutable Primary Key)</label>
            <input
              disabled
              value={formData.email}
              className="block w-full rounded-lg border border-slate-200 bg-slate-100 text-slate-500 text-xs px-3.5 py-2.5 cursor-not-allowed"
            />
          </div>

          <Input
            label="Phone Number"
            value={formData.phone}
            onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
          />

          <Input
            label="Address"
            value={formData.address}
            onChange={(e) => setFormData({ ...formData, address: e.target.value })}
          />

          <div className="flex items-center justify-end gap-2.5 pt-3 border-t border-slate-100">
            <Button size="sm" variant="outline" type="button" onClick={() => setIsEditModalOpen(false)}>
              Cancel
            </Button>
            <Button size="sm" variant="primary" type="submit" isLoading={isSubmitting}>
              Save Profile Changes
            </Button>
          </div>
        </form>
      </Dialog>
    </div>
  );
};

