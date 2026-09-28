import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { authAPI, setAuthToken } from '../../services/api';

const PatientAccessPage = () => {
  const navigate = useNavigate();
  const [form, setForm] = useState({ phoneNumber: '', hospitalCode: '', name: '', otp: '' });
  const [sent, setSent] = useState(false);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');

  const update = (event) => setForm((current) => ({ ...current, [event.target.name]: event.target.value }));

  const requestOtp = async (event) => {
    event.preventDefault();
    setBusy(true);
    setMessage('');
    try {
      await authAPI.requestOTP(null, 'PATIENT_LOGIN', form.phoneNumber);
      setSent(true);
      setMessage('Verification code requested.');
    } catch (error) {
      setMessage(error.response?.data?.message || 'Could not request a verification code.');
    } finally {
      setBusy(false);
    }
  };

  const verify = async (event) => {
    event.preventDefault();
    setBusy(true);
    setMessage('');
    try {
      const response = await authAPI.verifyPatientOTP(form);
      const { token, username, role, hospitalId, phno } = response.data;
      setAuthToken(token);
      localStorage.setItem('user', JSON.stringify({ username, role, hospitalId, phno }));
      navigate('/dashboard');
    } catch (error) {
      setMessage(error.response?.data?.message || 'Verification failed.');
    } finally {
      setBusy(false);
    }
  };

  return (
    <main className="login-page">
      <section className="login-container">
        <h1 className="login-title">PATIENT ACCESS</h1>
        <p className="text-gray-600 mb-6">Use your verified mobile number to access appointments and finalized records.</p>
        {message && <div className="login-error mb-4">{message}</div>}
        <form onSubmit={sent ? verify : requestOtp} className="login-form">
          <label className="login-label" htmlFor="phoneNumber">PHONE NUMBER</label>
          <input className="login-input" id="phoneNumber" name="phoneNumber" value={form.phoneNumber} onChange={update} required disabled={sent} />
          <label className="login-label" htmlFor="hospitalCode">CLINIC CODE</label>
          <input className="login-input" id="hospitalCode" name="hospitalCode" value={form.hospitalCode} onChange={update} required disabled={sent} />
          {!sent && <><label className="login-label" htmlFor="name">NAME</label><input className="login-input" id="name" name="name" value={form.name} onChange={update} /></>}
          {sent && <><label className="login-label" htmlFor="otp">OTP</label><input className="login-input" id="otp" name="otp" value={form.otp} onChange={update} inputMode="numeric" pattern="[0-9]{6}" required /></>}
          <button className="login-button" type="submit" disabled={busy}>{busy ? 'PLEASE WAIT...' : sent ? 'VERIFY OTP' : 'REQUEST OTP'}</button>
        </form>
        <Link className="login-link" to="/login">Staff login</Link>
      </section>
    </main>
  );
};

export default PatientAccessPage;
