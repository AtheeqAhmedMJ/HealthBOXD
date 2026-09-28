import React, { useEffect, useState } from 'react';
import { superadminAPI } from '../../services/api';

const SuperAdminPage = () => {
  const [stats, setStats] = useState(null);
  const [companies, setCompanies] = useState([]);
  const [approvals, setApprovals] = useState([]);
  const [status, setStatus] = useState('PENDING');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const [statsData, companiesData, approvalsData] = await Promise.all([
        superadminAPI.getStats(),
        superadminAPI.getCompanies(),
        superadminAPI.getApprovals(status),
      ]);
      setStats(statsData);
      setCompanies(companiesData || []);
      setApprovals(approvalsData || []);
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Could not load platform data.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [status]);

  const review = async (id, decision) => {
    try {
      await superadminAPI.reviewApproval(id, decision);
      await load();
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Could not update approval.');
    }
  };

  if (loading && !stats) return <main className="min-h-screen p-8 text-gray-700">Loading platform overview...</main>;

  return (
    <main className="min-h-screen p-4 md:p-8">
      <div className="mx-auto max-w-7xl">
        <header className="mb-8"><p className="text-sm font-semibold uppercase tracking-wide text-purple-700">HealthBoxD control plane</p><h1 className="mt-1 text-4xl font-bold text-gray-900">Platform Overview</h1><p className="mt-2 text-gray-600">Review companies, monitor platform health, and process operational approvals.</p></header>
        {error && <div className="mb-6 rounded-lg border border-red-200 bg-red-50 p-4 text-red-700">{error}</div>}
        <section className="mb-8 grid grid-cols-2 gap-4 lg:grid-cols-4">{[['Companies', stats?.totalHospitals], ['Doctors', stats?.totalDoctors], ['Patients', stats?.totalPatientProfiles], ['Gross volume', `₹${Number(stats?.totalGrossVolume || 0).toLocaleString('en-IN')}`]].map(([label, value]) => <article className="rounded-xl border border-white/30 bg-white/65 p-5 shadow-sm backdrop-blur-md" key={label}><p className="text-sm text-gray-600">{label}</p><p className="mt-2 text-3xl font-bold text-gray-900">{value || 0}</p></article>)}</section>
        <section className="mb-8 rounded-xl border border-white/30 bg-white/55 p-6 shadow-sm backdrop-blur-md"><div className="mb-5 flex flex-wrap items-center justify-between gap-3"><div><h2 className="text-2xl font-bold text-gray-900">Approval queue</h2><p className="text-sm text-gray-600">Every decision is recorded with a lifecycle status.</p></div><select value={status} onChange={(event) => setStatus(event.target.value)} className="rounded-lg border border-gray-200 bg-white px-3 py-2"><option>PENDING</option><option>APPROVED</option><option>REJECTED</option></select></div>{approvals.length === 0 ? <p className="rounded-lg bg-white/70 p-4 text-gray-600">No {status.toLowerCase()} approvals.</p> : <div className="space-y-3">{approvals.map((approval) => <div className="flex flex-wrap items-center justify-between gap-4 rounded-lg border border-white/30 bg-white/75 p-4" key={approval.id}><div><p className="font-semibold text-gray-900">{approval.requestType.replaceAll('_', ' ')}</p><p className="text-sm text-gray-600">{approval.details || 'No details provided'} · {new Date(approval.createdAt).toLocaleString()}</p></div><div className="flex gap-2">{approval.status === 'PENDING' && <><button type="button" onClick={() => review(approval.id, 'APPROVED')} className="rounded-lg bg-emerald-700 px-3 py-2 text-sm font-semibold text-white">Approve</button><button type="button" onClick={() => review(approval.id, 'REJECTED')} className="rounded-lg bg-red-700 px-3 py-2 text-sm font-semibold text-white">Reject</button></>}<span className="rounded-full bg-gray-100 px-3 py-2 text-xs font-semibold text-gray-700">{approval.status}</span></div></div>)}</div>}</section>
        <section className="rounded-xl border border-white/30 bg-white/55 p-6 shadow-sm backdrop-blur-md"><h2 className="mb-5 text-2xl font-bold text-gray-900">Companies</h2><div className="overflow-x-auto"><table className="w-full min-w-[700px] text-left text-sm"><thead className="border-b border-gray-200 text-gray-600"><tr><th className="p-3">Company</th><th className="p-3">Location</th><th className="p-3">Status</th><th className="p-3">Doctors</th><th className="p-3">Patients</th><th className="p-3">Transactions</th></tr></thead><tbody>{companies.map((company) => <tr className="border-b border-gray-100" key={company.id}><td className="p-3"><p className="font-semibold text-gray-900">{company.name}</p><p className="text-xs text-gray-500">{company.code}</p></td><td className="p-3 text-gray-700">{company.location || '-'}</td><td className="p-3"><span className="rounded-full bg-gray-100 px-3 py-1 text-xs font-semibold">{company.approvalStatus}</span></td><td className="p-3">{company.doctors}</td><td className="p-3">{company.patients}</td><td className="p-3">{company.transactions || 0}</td></tr>)}</tbody></table></div></section>
      </div>
    </main>
  );
};

export default SuperAdminPage;
