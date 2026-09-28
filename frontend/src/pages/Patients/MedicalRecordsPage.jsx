import React, { useEffect, useState } from 'react';
import { consultationsAPI, prescriptionsAPI } from '../../services/api';
import { useApp } from '../../context/AppContext';

const MedicalRecordsPage = () => {
  const [records, setRecords] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const { selectedPatient, user } = useApp();

  useEffect(() => {
    const consultationRequest = user?.role === 'PATIENT'
      ? consultationsAPI.patientRecords()
      : selectedPatient?.phno
        ? consultationsAPI.recordsForPatient(selectedPatient.phno)
        : consultationsAPI.recordsForDoctor();
    Promise.all([consultationRequest, prescriptionsAPI.getAll()])
      .then(([consultations, prescriptions]) => {
        const byConsultation = new Map((prescriptions || []).map((prescription) => [prescription.consultationId, prescription]));
        setRecords((consultations || []).filter((consultation) => !selectedPatient?.phno || consultation.patientPhno === selectedPatient.phno).map((consultation) => ({ consultation, prescription: byConsultation.get(consultation.id) })));
      })
      .catch((requestError) => setError(requestError.response?.data?.message || 'Could not load medical records.'))
      .finally(() => setLoading(false));
  }, [selectedPatient?.phno, user?.role]);

  const downloadRecords = () => {
    const payload = records.map(({ consultation, prescription }) => ({ consultation, prescription }));
    const url = URL.createObjectURL(new Blob([JSON.stringify(payload, null, 2)], { type: 'application/json' }));
    const link = document.createElement('a');
    link.href = url;
    link.download = `healthboxd-patient-records-${selectedPatient?.phno || 'archive'}.json`;
    link.click();
    URL.revokeObjectURL(url);
  };

  return <main className="min-h-screen max-w-5xl mx-auto p-6 md:p-10"><div className="mb-6 flex flex-wrap items-center justify-between gap-3"><div><h1 className="text-3xl font-bold mb-2">Patient Records</h1><p className="text-gray-600">Finalized encounters saved after verified billing and payment.</p></div><div className="flex gap-2"><button type="button" onClick={downloadRecords} disabled={!records.length} className="rounded-lg border bg-white/70 px-4 py-2 text-sm font-semibold text-gray-800 disabled:opacity-50">Download data</button><button type="button" onClick={() => window.print()} className="rounded-lg border bg-white/70 px-4 py-2 text-sm font-semibold text-gray-800">Print records</button></div></div>{error && <div className="p-3 mb-5 bg-red-50 text-red-700 rounded">{error}</div>}{loading ? <div className="rounded-xl bg-white/70 p-8 text-gray-600">Loading patient records...</div> : records.length === 0 ? <div className="rounded-xl border border-white/30 bg-white/70 p-8 text-gray-600">No finalized patient records yet.</div> : <div className="space-y-5">{records.map(({ consultation, prescription }) => <article className="rounded-xl border border-white/30 bg-white/70 p-6 shadow" key={consultation.id}><div className="flex flex-wrap justify-between gap-4"><div><h2 className="text-xl font-bold">{consultation.diagnosis || 'Consultation'}</h2><p className="text-sm text-gray-500">Patient {consultation.patientPhno} · {consultation.patientType || 'OP'} · {new Date(consultation.finalizedAt || consultation.createdAt).toLocaleDateString()}</p></div><span className="text-sm font-semibold text-emerald-700">FINALIZED</span></div><div className="my-4 grid grid-cols-3 gap-3 text-sm"><span>BP: {consultation.bp || '-'}</span><span>GRBS: {consultation.grbs || '-'}</span><span>SPO2: {consultation.spo2 || '-'}</span></div><p className="text-gray-700">{consultation.clinicalNotes || consultation.generalNotes || 'No remarks recorded.'}</p>{prescription?.medicines?.length > 0 && <div className="mt-4"><h3 className="mb-2 font-semibold">Medications</h3><div className="space-y-1">{prescription.medicines.map((medicine, index) => <p className="rounded bg-purple-50/70 p-2 text-sm" key={index}>{medicine.type || medicine.medicationType} {medicine.name || medicine.medicationName} · {medicine.dosage} · {medicine.duration} · {medicine.foodInstruction || medicine.food || ''}</p>)}</div></div>}</article>)}</div>}</main>;
};

export default MedicalRecordsPage;
