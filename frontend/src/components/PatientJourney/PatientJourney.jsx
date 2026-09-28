import React from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useApp } from '../../context/AppContext';

const stages = [
  ['appointment', 'Appointment', '/appointments'],
  ['consultation', 'Consultation', '/consultation/editor'],
  ['remarks', 'Remarks', '/consultation/editor'],
  ['prescription', 'Prescription', '/prescriptions'],
  ['medication', 'Medication', '/prescriptions'],
  ['billing', 'Billing', '/billing'],
];

const PatientJourney = () => {
  const { selectedPatient, workflow, setSelectedPatient } = useApp();
  const navigate = useNavigate();
  const location = useLocation();
  if (!selectedPatient) return null;

  const completed = stages.filter(([key]) => workflow[key]).length;
  const next = stages.find(([key]) => !workflow[key]);

  const go = (path) => {
    navigate(path);
  };

  return (
    <aside className="fixed bottom-4 left-4 right-4 z-40 rounded-xl border border-white/30 bg-white/80 p-3 shadow-lg backdrop-blur-md md:left-auto md:right-6 md:w-[min(30rem,calc(100vw-2rem))]">
      <div className="flex items-center justify-between gap-3">
        <button type="button" className="min-w-0 text-left" onClick={() => go('/patients')}>
          <p className="truncate text-sm font-bold text-gray-900">{selectedPatient.name || 'Selected patient'}</p>
          <p className="text-xs text-gray-500">{selectedPatient.phno}</p>
        </button>
        <button type="button" onClick={() => setSelectedPatient(null)} className="text-xs text-gray-500 hover:text-gray-900">Clear</button>
      </div>
      <div className="mt-3 grid grid-cols-3 gap-1.5 sm:grid-cols-6">
        {stages.map(([key, label, path]) => (
          <button
            type="button"
            key={key}
            onClick={() => go(path)}
            className={`rounded-md px-1 py-1.5 text-[10px] font-semibold transition-colors ${workflow[key] ? 'bg-emerald-100 text-emerald-800' : location.pathname === path ? 'bg-purple-100 text-purple-800' : 'bg-gray-100 text-gray-500'}`}
            title={label}
          >
            {label} {workflow[key] ? '✓' : '·'}
          </button>
        ))}
      </div>
      <button type="button" onClick={() => next ? go(next[2]) : go('/medical-records')} className="mt-2 w-full rounded-md bg-gray-900 px-3 py-2 text-xs font-semibold text-white disabled:bg-emerald-700">
        {next ? `Next: ${next[1]}` : 'Save to patient records'}
      </button>
      <p className="mt-1 text-center text-[11px] text-gray-500">{completed} / {stages.length} stages completed</p>
    </aside>
  );
};

export default PatientJourney;
