import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { paymentsAPI } from '../../services/api';
import { useApp } from '../../context/AppContext';

const PaymentsPage = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const query = new URLSearchParams(location.search);
  const consultationId = query.get('consultationId');
  const patientPhno = query.get('patientPhno') || JSON.parse(localStorage.getItem('user') || '{}').phno;
  const medicines = location.state?.medicines || [];
  const patientName = location.state?.patientName || patientPhno;
  const [payments, setPayments] = useState([]);
  const [amount, setAmount] = useState('');
  const [loading, setLoading] = useState(true);
  const [processing, setProcessing] = useState(false);
  const [message, setMessage] = useState('');
  const { updateWorkflow } = useApp();

  const load = async () => {
    try {
      const result = await paymentsAPI.getAll();
      setPayments(Array.isArray(result) ? result : []);
    } catch (error) {
      setMessage(error.response?.data?.message || 'Could not load payment history.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const pay = async (event) => {
    event.preventDefault();
    const amountPaise = Math.round(Number(amount) * 100);
    if (!Number.isFinite(amountPaise) || amountPaise <= 75) {
      setMessage('Enter a valid amount above the platform fee.');
      return;
    }
    setProcessing(true);
    setMessage('');
    try {
      const order = await paymentsAPI.checkout({
        consultationId: Number(consultationId), patientPhno, patientName,
        customAmountPaise: amountPaise, medicines,
      });
      const openCheckout = () => {
        const razorpay = new window.Razorpay({
          key: order.keyId, amount: order.amountPaise, currency: order.currency,
          name: 'HealthBoxD', description: `Consultation ${consultationId}`,
          order_id: order.razorpayOrderId,
          handler: async (response) => {
            try {
              await paymentsAPI.verify({
                razorpayOrderId: order.razorpayOrderId,
                razorpayPaymentId: response.razorpay_payment_id,
                razorpaySignature: response.razorpay_signature,
              });
              updateWorkflow({ billing: true });
              setMessage('Payment verified. The medical record is now finalized.');
              await load();
              navigate('/medical-records');
            } catch (error) {
              setMessage(error.response?.data?.message || 'Payment verification failed.');
            } finally { setProcessing(false); }
          },
        });
        razorpay.open();
      };
      if (window.Razorpay) openCheckout();
      else {
        const script = document.createElement('script');
        script.src = 'https://checkout.razorpay.com/v1/checkout.js';
        script.onload = openCheckout;
        script.onerror = () => { setMessage('Payment provider could not be loaded.'); setProcessing(false); };
        document.head.appendChild(script);
      }
    } catch (error) {
      setMessage(error.response?.data?.message || 'Could not create payment.');
      setProcessing(false);
    }
  };

  return <main className="min-h-screen p-6 md:p-10 max-w-5xl mx-auto">
    <h1 className="text-3xl font-bold text-gray-900 mb-2">Payments</h1>
    <p className="text-gray-600 mb-6">Verified payment is required before a consultation becomes an official medical record.</p>
    {message && <div className="mb-5 p-3 rounded-lg bg-amber-50 text-amber-800">{message}</div>}
    {consultationId && <form onSubmit={pay} className="bg-white/70 rounded-2xl p-6 shadow mb-8 max-w-xl"><h2 className="font-bold text-xl mb-4">Consultation payment</h2><label className="block text-sm font-medium">Amount in INR<input className="mt-1 border rounded-lg p-3 w-full" value={amount} onChange={(event) => setAmount(event.target.value)} type="number" min="1" step="0.01" required /></label><button className="mt-5 px-4 py-3 rounded-lg bg-emerald-700 text-white" disabled={processing}>{processing ? 'PROCESSING...' : 'PAY AND FINALIZE'}</button></form>}
    <section className="bg-white/70 rounded-2xl p-6 shadow"><h2 className="font-bold text-xl mb-4">Payment history</h2>{loading ? <p>Loading...</p> : payments.length === 0 ? <p className="text-gray-600">No payments found.</p> : <div className="space-y-3">{payments.map((payment) => <div className="border rounded-lg p-4 flex justify-between" key={payment.id}><span>Order {payment.razorpayOrderId}</span><span className="font-semibold">{payment.status} · ₹{(payment.amountPaise / 100).toFixed(2)}</span></div>)}</div>}</section>
  </main>;
};

export default PaymentsPage;
