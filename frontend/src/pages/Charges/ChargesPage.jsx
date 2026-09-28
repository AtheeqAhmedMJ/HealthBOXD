import React, { useEffect, useState } from 'react';
import { chargesAPI } from '../../services/api';
import { useApp } from '../../context/AppContext';

const ChargesPage = () => {
  const [charges, setCharges] = useState([]);
  const [form, setForm] = useState({ name: '', amountPaise: '' });
  const [message, setMessage] = useState('');
  const { user } = useApp();
  const [loading, setLoading] = useState(true);
  const load = async () => { setLoading(true); try { const data = await chargesAPI.getAll(); setCharges(Array.isArray(data) ? data : []); } catch (error) { setMessage(error.response?.status === 403 ? 'Charges can only be managed by an administrator.' : error.response?.data?.message || 'Could not load charges.'); } finally { setLoading(false); } };
  useEffect(() => { load(); }, []);
  const create = async (event) => { event.preventDefault(); try { await chargesAPI.create({ name: form.name.trim(), amountPaise: Number(form.amountPaise) }); setForm({ name: '', amountPaise: '' }); await load(); } catch (error) { setMessage(error.response?.data?.message || 'Could not create charge.'); } };
  const deactivate = async (id) => { try { await chargesAPI.delete(id); await load(); } catch (error) { setMessage(error.response?.data?.message || 'Could not deactivate charge.'); } };
  return <section className="rounded-xl border border-white/20 bg-white/40 p-6 shadow-lg backdrop-blur-md"><h2 className="mb-2 text-2xl font-bold text-gray-900">Charges</h2><p className="mb-6 text-sm text-gray-600">Configure billable services used during checkout.</p>{user?.role !== 'ADMIN' ? <p className="rounded-lg bg-amber-50 p-4 text-amber-800">Charges can only be managed by an administrator.</p> : <>{message && <p className="mb-4 rounded bg-red-50 p-3 text-red-700">{message}</p>}<form onSubmit={create} className="mb-8 grid grid-cols-1 gap-3 md:grid-cols-3"><input className="rounded-lg border border-gray-200 bg-white p-3" placeholder="Charge name" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} required /><input className="rounded-lg border border-gray-200 bg-white p-3" type="number" min="1" placeholder="Amount in paise" value={form.amountPaise} onChange={(event) => setForm({ ...form, amountPaise: event.target.value })} required /><button className="rounded-lg bg-gray-900 p-3 font-semibold text-white">Add charge</button></form><div className="space-y-3">{loading ? <p className="rounded-lg bg-white/60 p-4 text-gray-600">Loading charges...</p> : charges.length === 0 ? <p className="rounded-lg bg-white/60 p-4 text-gray-600">No charges configured.</p> : charges.map((charge) => <div className="flex items-center justify-between rounded-lg border border-white/30 bg-white/70 p-4" key={charge.id}><span>{charge.name} · ₹{(charge.amountPaise / 100).toFixed(2)}</span>{charge.active && <button className="text-red-700" onClick={() => deactivate(charge.id)}>Deactivate</button>}</div>)}</div></>}</section>;
};
export default ChargesPage;
