import React, { useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { consultationsAPI, medicationsAPI } from '../../services/api';
import { useApp } from '../../context/AppContext';
import './ConsultationEditor.css';

const emptyMedication = { type: 'TAB', name: '', dosage: '', quantity: '', duration: '', foodInstruction: '' };
const emptyInjection = { type: 'INJECTION', itemName: '', strength: '', volume: '', route: 'IV', frequency: '', startTime: '', intervalMinutes: '', notes: '' };

const ConsultationEditor = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const query = new URLSearchParams(location.search);
  const phone = query.get('phno') || '';
  const appointmentId = query.get('appointmentId') ? Number(query.get('appointmentId')) : null;
  const [consultation, setConsultation] = useState(null);
  const [form, setForm] = useState({ patientType: 'OP', symptoms: '', diagnosis: '', clinicalNotes: '', generalNotes: '', bp: '', grbs: '', spo2: '', temperature: '', nextVisitDate: '' });
  const [medicines, setMedicines] = useState([{ ...emptyMedication }]);
  const [inpatientDetails, setInpatientDetails] = useState({ bedNumber: '', wardType: 'General', admissionDate: '', dischargeDate: '' });
  const [injections, setInjections] = useState([]);
  const [suggestions, setSuggestions] = useState([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [activeTab, setActiveTab] = useState('patient');
  const initialized = useRef(false);
  const { selectedPatient, setSelectedPatient, updateWorkflow } = useApp();

  useEffect(() => {
    const load = async () => {
      if (initialized.current) return;
      initialized.current = true;
      try {
        const created = query.get('consultationId')
          ? await consultationsAPI.get(Number(query.get('consultationId')))
          : await consultationsAPI.create({ patientPhno: phone, appointmentId, patientType: form.patientType, inpatientDetails, injections });
        setConsultation(created);
        setSelectedPatient({ ...selectedPatient, name: selectedPatient?.name || phone, phno: phone, appointmentId, consultationId: created.id });
        updateWorkflow({ consultation: true });
        setForm((current) => ({ ...current, ...created }));
        if (created.inpatientDetails) setInpatientDetails((current) => ({ ...current, ...created.inpatientDetails }));
        if (created.injections) setInjections(created.injections);
        if (created.medicines) setMedicines(created.medicines);
        if (created.status === 'CREATED') await consultationsAPI.start(created.id);
        if (!query.get('consultationId')) navigate(`/consultation/editor?phno=${encodeURIComponent(phone)}&appointmentId=${appointmentId || ''}&consultationId=${created.id}`, { replace: true });
      } catch (requestError) {
        setError(requestError.response?.data?.message || 'Could not open consultation.');
      }
    };
    if (phone) load();
  }, [phone, appointmentId]);

  const update = (event) => setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  const updateMedicine = (index, field, value) => setMedicines((current) => current.map((item, itemIndex) => itemIndex === index ? { ...item, [field]: value } : item));

  const searchMedication = async (index, value) => {
    updateMedicine(index, 'name', value);
    if (value.trim().length < 2) return setSuggestions([]);
    try {
      const result = await medicationsAPI.search(value);
      setSuggestions(result.content || []);
    } catch {
      setSuggestions([]);
    }
  };

  const save = async () => {
    if (!consultation) return;
    setBusy(true);
    setError('');
    try {
      const saved = await consultationsAPI.update(consultation.id, { ...form, inpatientDetails, injections, medicines });
      setConsultation(saved);
      setForm((current) => ({ ...current, ...saved }));
      updateWorkflow({ remarks: true });
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Could not save consultation.');
    } finally {
      setBusy(false);
    }
  };

  const requestPayment = async () => {
    if (!consultation) return;
    setBusy(true);
    setError('');
    try {
      await consultationsAPI.update(consultation.id, { ...form, inpatientDetails, injections, medicines });
      await consultationsAPI.awaitPayment(consultation.id);
      updateWorkflow({ prescription: true, medication: medicines.length > 0 });
      navigate(`/payments?consultationId=${consultation.id}&patientPhno=${encodeURIComponent(phone)}`, { state: { medicines, patientName: phone } });
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Could not prepare payment.');
      setBusy(false);
    }
  };

  const printPrescription = () => {
    window.print();
    updateWorkflow({ prescription: true });
  };

  const tabs = [['patient', 'Patient & Remarks'], ['medications', 'Medications'], ['summary', 'Summary']];
  const updateInjection = (index, field, value) => setInjections((current) => current.map((item, itemIndex) => itemIndex === index ? { ...item, [field]: value } : item));

  return (
    <main className="min-h-screen p-6 md:p-10">
      <div className="prescription-editor">
        <section className="prescription-paper">
          <header className="prescription-header">
            <div className="prescription-header-grid">
              <div><h1>Prescription</h1><p>HealthBoxD clinical encounter</p></div>
              <div className="prescription-meta"><strong>{selectedPatient?.name || 'Patient'}</strong><br />{phone}<br />Status: {consultation?.status || 'Loading'}</div>
            </div>
          </header>
          <nav className="prescription-tabs no-print" aria-label="Prescription sections">
            {tabs.map(([key, label]) => <button type="button" key={key} className={`prescription-tab ${activeTab === key ? 'active' : ''}`} onClick={() => setActiveTab(key)}>{label}</button>)}
          </nav>
          {error && <div className="m-5 rounded-lg bg-red-50 p-3 text-red-700">{error}</div>}
          <section className="prescription-panel" hidden={activeTab !== 'patient'}>
            <div className="prescription-section"><h2>Patient Information</h2><div className="prescription-grid"><label className="prescription-field">Patient name<input value={selectedPatient?.name || ''} readOnly /></label><label className="prescription-field">Patient phone<input value={phone} readOnly /></label><label className="prescription-field">Patient type<select name="patientType" value={form.patientType || 'OP'} onChange={update}><option value="OP">Outpatient (OP)</option><option value="IP">Inpatient (IP)</option></select></label><label className="prescription-field">Appointment<input value={appointmentId || 'Walk-in'} readOnly /></label><label className="prescription-field">Next visit<input type="date" name="nextVisitDate" value={form.nextVisitDate || ''} onChange={update} /></label></div></div>
            <div className="prescription-section"><h2>Vital Signs</h2><div className="prescription-grid vitals">{['bp', 'grbs', 'spo2', 'temperature'].map((field) => <label className="prescription-field" key={field}>{field.toUpperCase()}<input name={field} value={form[field] || ''} onChange={update} /></label>)}</div></div>
            <div className="prescription-section"><h2>Clinical Remarks</h2><div className="prescription-grid">{['symptoms', 'diagnosis', 'clinicalNotes', 'generalNotes'].map((field) => <label className="prescription-field full" key={field}>{field.replace(/([A-Z])/g, ' $1')}<textarea name={field} value={form[field] || ''} onChange={update} /></label>)}</div></div>
            {form.patientType === 'IP' && <div className="prescription-section"><h2>Inpatient Care</h2><div className="prescription-grid"><label className="prescription-field">Bed number<input value={inpatientDetails.bedNumber} onChange={(event) => setInpatientDetails({ ...inpatientDetails, bedNumber: event.target.value })} /></label><label className="prescription-field">Ward type<select value={inpatientDetails.wardType} onChange={(event) => setInpatientDetails({ ...inpatientDetails, wardType: event.target.value })}><option>General</option><option>Semi-Private</option><option>Private</option></select></label><label className="prescription-field">Admission date<input type="datetime-local" value={inpatientDetails.admissionDate} onChange={(event) => setInpatientDetails({ ...inpatientDetails, admissionDate: event.target.value })} /></label><label className="prescription-field">Expected discharge<input type="date" value={inpatientDetails.dischargeDate} onChange={(event) => setInpatientDetails({ ...inpatientDetails, dischargeDate: event.target.value })} /></label></div><div className="inpatient-rows">{injections.map((injection, index) => <div className="prescription-grid inpatient-row" key={index}><label className="prescription-field">Type<select value={injection.type} onChange={(event) => updateInjection(index, 'type', event.target.value)}><option>INJECTION</option><option>FLUID</option><option>IV_LINE</option></select></label><label className="prescription-field">Item<input value={injection.itemName} onChange={(event) => updateInjection(index, 'itemName', event.target.value)} /></label><label className="prescription-field">Strength<input value={injection.strength} onChange={(event) => updateInjection(index, 'strength', event.target.value)} /></label><label className="prescription-field">Volume<input value={injection.volume} onChange={(event) => updateInjection(index, 'volume', event.target.value)} /></label><label className="prescription-field">Route<input value={injection.route} onChange={(event) => updateInjection(index, 'route', event.target.value)} /></label><label className="prescription-field">Frequency<input value={injection.frequency} onChange={(event) => updateInjection(index, 'frequency', event.target.value)} /></label><label className="prescription-field">Start time<input type="time" value={injection.startTime} onChange={(event) => updateInjection(index, 'startTime', event.target.value)} /></label><label className="prescription-field">Interval minutes<input type="number" value={injection.intervalMinutes} onChange={(event) => updateInjection(index, 'intervalMinutes', event.target.value)} /></label><label className="prescription-field full">Notes<input value={injection.notes} onChange={(event) => updateInjection(index, 'notes', event.target.value)} /></label></div>)}</div><button type="button" className="rounded-lg bg-gray-900 px-4 py-2 text-sm font-semibold text-white" onClick={() => setInjections((current) => [...current, { ...emptyInjection }])}>Add injection or fluid</button></div>}
          </section>
          <section className="prescription-panel" hidden={activeTab !== 'medications'}>
            <div className="prescription-section"><h2>Medications Prescribed</h2><div className="medication-list">{medicines.map((medicine, index) => <div className="medication-row" key={index}><select value={medicine.type} onChange={(event) => updateMedicine(index, 'type', event.target.value)}>{['TAB', 'CAP', 'SYRUP', 'OINT', 'POWDER', 'INJC', 'OTHERS'].map((type) => <option key={type}>{type}</option>)}</select><div className="suggestions"><input placeholder="Medication name" value={medicine.name} onChange={(event) => searchMedication(index, event.target.value)} />{index === 0 && suggestions.length > 0 && <div className="medication-suggestions">{suggestions.map((item) => <button type="button" key={item.id} onClick={() => { updateMedicine(index, 'name', item.name); setSuggestions([]); }}>{item.name} ({item.category})</button>)}</div>}</div><input placeholder="Dosage" value={medicine.dosage} onChange={(event) => updateMedicine(index, 'dosage', event.target.value)} /><input placeholder="Quantity" value={medicine.quantity} onChange={(event) => updateMedicine(index, 'quantity', event.target.value)} /><input placeholder="Duration" value={medicine.duration} onChange={(event) => updateMedicine(index, 'duration', event.target.value)} /><input placeholder="Food" value={medicine.foodInstruction} onChange={(event) => updateMedicine(index, 'foodInstruction', event.target.value)} />{medicines.length > 1 && <button className="medication-remove" type="button" onClick={() => setMedicines((current) => current.filter((_, itemIndex) => itemIndex !== index))} aria-label="Remove medication">Remove</button>}</div>)}</div><button type="button" className="rounded-lg bg-gray-900 px-4 py-2 text-sm font-semibold text-white" onClick={() => setMedicines((current) => [...current, { ...emptyMedication }])}>Add medication</button></div>
          </section>
          <section className="prescription-panel" hidden={activeTab !== 'summary'}>
            <div className="prescription-section"><h2>Prescription Summary</h2><div className="prescription-summary"><article><strong>Patient</strong>{selectedPatient?.name || phone}</article><article><strong>Remarks</strong>{form.diagnosis || 'Not entered'}</article><article><strong>Medication count</strong>{medicines.filter((medicine) => medicine.name).length}</article><article><strong>Payment</strong>Pending until billing is completed</article></div></div>
          </section>
          <div className="prescription-actions no-print"><button type="button" onClick={save} disabled={busy}>Save remarks</button><button type="button" onClick={printPrescription} disabled={busy || !consultation}>Print prescription</button><button type="button" className="success" onClick={requestPayment} disabled={busy || !consultation}>Continue to billing</button></div>
        </section>
      </div>
    </main>
  );
};

export default ConsultationEditor;
