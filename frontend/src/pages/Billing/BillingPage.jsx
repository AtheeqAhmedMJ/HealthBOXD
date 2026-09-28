import React, { useState, useEffect, useContext } from 'react';
import { FiDollarSign, FiRefreshCw, FiDownload, FiFilter, FiSearch } from 'react-icons/fi';
import { AppContext } from '../../context/AppContext';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';

const BillingPage = () => {
  const { user } = useContext(AppContext);
  const { selectedPatient } = useContext(AppContext);
  const navigate = useNavigate();
  const [billings, setBillings] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [filterStatus, setFilterStatus] = useState('all');
  const [selectedBilling, setSelectedBilling] = useState(null);
  const [showDetails, setShowDetails] = useState(false);

  const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';
  const token = localStorage.getItem('authToken');

  // Fetch billings
  useEffect(() => {
    fetchBillings();
  }, [filterStatus]);

  const fetchBillings = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await axios.get(`${API_URL}/billing`, {
        headers: { Authorization: `Bearer ${token}` },
        params: { status: filterStatus !== 'all' ? filterStatus : undefined }
      });
      setBillings(Array.isArray(response.data) ? response.data : []);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load billings');
      console.error('Error fetching billings:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleRefresh = () => {
    fetchBillings();
  };

  const handleDownload = async (billingId) => {
    setSelectedBilling(billings.find((billing) => billing.id === billingId) || selectedBilling);
    window.print();
  };

  const filteredBillings = billings.filter(billing =>
    (!selectedPatient?.phno || billing.patientPhno === selectedPatient.phno) &&
    billing.id.toString().includes(searchTerm) ||
    billing.consultationId?.toString().includes(searchTerm) ||
    billing.patientName?.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const stats = {
    total: billings.length,
    pending: billings.filter(b => b.status === 'PENDING').length,
    paid: billings.filter(b => b.status === 'PAID').length,
    totalAmount: billings.reduce((sum, b) => sum + (b.totalAmount || 0), 0)
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'PAID': return 'bg-green-100 text-green-800';
      case 'PENDING': return 'bg-yellow-100 text-yellow-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  const formatCurrency = (amount) => {
    return `₹${Number(amount || 0).toFixed(2)}`;
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('en-IN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-purple-50/50 via-pink-50/30 to-white p-4 md:p-8">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="flex items-center justify-between mb-8">
          <h1 className="text-4xl font-bold text-gray-900 flex items-center gap-3">
            <FiDollarSign className="text-purple-600" size={36} />
            Billing
          </h1>
          <button
            onClick={handleRefresh}
            disabled={loading}
            className="flex items-center gap-2 bg-purple-600 hover:bg-purple-700 text-white px-4 py-2 rounded-lg transition-colors disabled:opacity-50"
          >
            <FiRefreshCw className={loading ? 'animate-spin' : ''} size={18} />
            Refresh
          </button>
        </div>

        {selectedPatient && (
          <div className="mb-6 flex flex-wrap items-center justify-between gap-3 rounded-xl border border-purple-200 bg-purple-50/70 p-4">
            <div><p className="text-xs font-semibold uppercase text-purple-600">Current patient</p><p className="font-bold text-gray-900">{selectedPatient.name || selectedPatient.phno}</p></div>
            {selectedPatient.consultationId && <button type="button" onClick={() => navigate(`/payments?consultationId=${selectedPatient.consultationId}&patientPhno=${encodeURIComponent(selectedPatient.phno)}`, { state: { patientName: selectedPatient.name } })} className="rounded-lg bg-emerald-700 px-4 py-2 text-sm font-semibold text-white">Open QR payment</button>}
          </div>
        )}

        {error && (
          <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-6 text-red-800">
            {error}
          </div>
        )}

        {/* Stats */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
          <div className="bg-white/40 backdrop-blur-md rounded-xl p-6 shadow-lg border border-white/20">
            <p className="text-gray-600 text-sm font-medium mb-2">Total Billings</p>
            <p className="text-3xl font-bold text-gray-900">{stats.total}</p>
          </div>
          <div className="bg-white/40 backdrop-blur-md rounded-xl p-6 shadow-lg border border-white/20">
            <p className="text-gray-600 text-sm font-medium mb-2">Pending</p>
            <p className="text-3xl font-bold text-yellow-600">{stats.pending}</p>
          </div>
          <div className="bg-white/40 backdrop-blur-md rounded-xl p-6 shadow-lg border border-white/20">
            <p className="text-gray-600 text-sm font-medium mb-2">Paid</p>
            <p className="text-3xl font-bold text-green-600">{stats.paid}</p>
          </div>
          <div className="bg-white/40 backdrop-blur-md rounded-xl p-6 shadow-lg border border-white/20">
            <p className="text-gray-600 text-sm font-medium mb-2">Total Amount</p>
            <p className="text-2xl font-bold text-purple-600">{formatCurrency(stats.totalAmount)}</p>
          </div>
        </div>

        {/* Search & Filter */}
        <div className="flex flex-col md:flex-row gap-4 mb-6">
          <div className="flex-1 relative">
            <FiSearch className="absolute left-4 top-3 text-gray-400" size={20} />
            <input
              type="text"
              placeholder="Search by ID, consultation, or patient..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-12 pr-4 py-2 bg-white/40 backdrop-blur-md border border-white/20 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500"
            />
          </div>
          <div className="flex items-center gap-2">
            <FiFilter className="text-gray-600" size={20} />
            <select
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value)}
              className="px-4 py-2 bg-white/40 backdrop-blur-md border border-white/20 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500"
            >
              <option value="all">All Status</option>
              <option value="PENDING">Pending</option>
              <option value="PAID">Paid</option>
            </select>
          </div>
        </div>

        {/* Billings Table */}
        <div className="bg-white/40 backdrop-blur-md rounded-xl shadow-lg border border-white/20 overflow-hidden">
          {loading ? (
            <div className="p-8 text-center">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-purple-600 mx-auto"></div>
              <p className="text-gray-600 mt-4">Loading billings...</p>
            </div>
          ) : filteredBillings.length === 0 ? (
            <div className="p-8 text-center text-gray-600">
              No billings found
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="border-b border-white/20 bg-white/20">
                  <tr>
                    <th className="px-6 py-4 text-left text-sm font-semibold text-gray-900">ID</th>
                    <th className="px-6 py-4 text-left text-sm font-semibold text-gray-900">Consultation</th>
                    <th className="px-6 py-4 text-left text-sm font-semibold text-gray-900">Amount</th>
                    <th className="px-6 py-4 text-left text-sm font-semibold text-gray-900">Status</th>
                    <th className="px-6 py-4 text-left text-sm font-semibold text-gray-900">Date</th>
                    <th className="px-6 py-4 text-left text-sm font-semibold text-gray-900">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/20">
                  {filteredBillings.map((billing) => (
                    <tr key={billing.id} className="hover:bg-white/30 transition-colors">
                      <td className="px-6 py-4 text-sm text-gray-900 font-medium">#{billing.id}</td>
                      <td className="px-6 py-4 text-sm text-gray-700">{billing.consultationId}</td>
                      <td className="px-6 py-4 text-sm font-semibold text-gray-900">
                        {formatCurrency(billing.totalAmount)}
                      </td>
                      <td className="px-6 py-4 text-sm">
                        <span className={`px-3 py-1 rounded-full text-xs font-medium ${getStatusColor(billing.status)}`}>
                          {billing.status}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-sm text-gray-600">
                        {formatDate(billing.createdAt)}
                      </td>
                      <td className="px-6 py-4 text-sm">
                        <div className="flex gap-2">
                          <button
                            onClick={() => {
                              setSelectedBilling(billing);
                              setShowDetails(true);
                            }}
                            className="text-purple-600 hover:text-purple-800 font-medium text-xs"
                          >
                            View
                          </button>
                          <button
                            onClick={() => handleDownload(billing.id)}
                            className="text-blue-600 hover:text-blue-800 font-medium text-xs flex items-center gap-1"
                          >
                            <FiDownload size={14} />
                            PDF
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Details Modal */}
        {showDetails && selectedBilling && (
          <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
            <div className="bg-white rounded-xl max-w-2xl w-full max-h-screen overflow-y-auto p-6">
              <div className="flex justify-between items-center mb-6">
                <h2 className="text-2xl font-bold text-gray-900">Billing Details</h2>
                <button
                  onClick={() => setShowDetails(false)}
                  className="text-gray-500 hover:text-gray-700 text-2xl"
                >
                  ×
                </button>
              </div>

              <div className="space-y-6">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <p className="text-sm text-gray-600">Billing ID</p>
                    <p className="font-bold text-lg">#{selectedBilling.id}</p>
                  </div>
                  <div>
                    <p className="text-sm text-gray-600">Status</p>
                    <p className={`font-bold px-3 py-1 rounded-full text-xs w-fit ${getStatusColor(selectedBilling.status)}`}>
                      {selectedBilling.status}
                    </p>
                  </div>
                  <div>
                    <p className="text-sm text-gray-600">Consultation ID</p>
                    <p className="font-bold text-lg">#{selectedBilling.consultationId}</p>
                  </div>
                  <div>
                    <p className="text-sm text-gray-600">Date</p>
                    <p className="font-bold text-lg">{formatDate(selectedBilling.createdAt)}</p>
                  </div>
                </div>

                <div className="border-t pt-6">
                  <h3 className="font-bold text-lg mb-4">Line Items</h3>
                  <div className="space-y-3">
                    {selectedBilling.lineItems?.map((item, idx) => (
                      <div key={idx} className="flex justify-between items-center p-3 bg-gray-50 rounded-lg">
                        <div>
                          <p className="font-medium text-gray-900">{item.description}</p>
                          <p className="text-sm text-gray-600">Qty: {item.qty}</p>
                        </div>
                        <p className="font-bold text-gray-900">{formatCurrency(item.finalPrice)}</p>
                      </div>
                    ))}
                  </div>
                </div>

                <div className="border-t pt-6 flex justify-between items-center">
                  <p className="text-xl font-bold text-gray-900">Total Amount:</p>
                  <p className="text-3xl font-bold text-purple-600">{formatCurrency(selectedBilling.totalAmount)}</p>
                </div>

                <div className="flex gap-3 pt-6">
                  <button
                    onClick={() => handleDownload(selectedBilling.id)}
                    className="flex-1 bg-blue-600 hover:bg-blue-700 text-white py-2 rounded-lg font-medium flex items-center justify-center gap-2"
                  >
                    <FiDownload size={18} />
                    Download PDF
                  </button>
                  <button
                    onClick={() => setShowDetails(false)}
                    className="flex-1 bg-gray-300 hover:bg-gray-400 text-gray-900 py-2 rounded-lg font-medium"
                  >
                    Close
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default BillingPage;