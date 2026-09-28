import React, { useState, useEffect } from 'react';
import { prescriptionsAPI } from '../../services/api';
import { useApp } from '../../context/AppContext';
import { FiFileText, FiSearch } from 'react-icons/fi';
import { useNavigate } from 'react-router-dom';

const PrescriptionsPage = () => {
  const [prescriptions, setPrescriptions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const { selectedPatient } = useApp();
  const navigate = useNavigate();

  useEffect(() => {
    fetchPrescriptions();
  }, []);

  const fetchPrescriptions = async () => {
    try {
      const data = await prescriptionsAPI.getAll();
      setPrescriptions(data);
    } catch (err) {
      console.error('Failed to fetch prescriptions:', err);
    } finally {
      setLoading(false);
    }
  };

  const visiblePrescriptions = prescriptions.filter((rx) => !selectedPatient?.phno || rx.patientPhno === selectedPatient.phno);

  return (
    <div className="min-h-screen bg-gradient-to-br from-purple-50/50 via-pink-50/30 to-white p-4 md:p-8">
      <div className="max-w-7xl mx-auto">
        <div className="mb-8">
          <h1 className="flex items-center gap-3 text-4xl font-bold text-gray-900">
            <FiFileText className="text-purple-600" />
            Prescription & Medication
          </h1>

          <p className="mt-1 text-gray-600">
            {selectedPatient ? `Prescription for ${selectedPatient.name || selectedPatient.phno}` : 'View and manage patient prescriptions'}
          </p>
        </div>

        <div className="mb-6 flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-purple-200 bg-white/60 p-5 shadow-sm backdrop-blur-md">
          <div>
            <p className="text-xs font-semibold uppercase tracking-wide text-purple-600">Current patient</p>
            <p className="mt-1 text-xl font-bold text-gray-900">{selectedPatient?.name || 'No patient selected'}</p>
            <p className="text-sm text-gray-600">{selectedPatient?.phno || 'Select an appointment or patient to continue.'}</p>
          </div>
          {selectedPatient && <button type="button" onClick={() => navigate(`/consultation/editor?phno=${encodeURIComponent(selectedPatient.phno)}&appointmentId=${selectedPatient.appointmentId || ''}`)} className="rounded-lg bg-purple-600 px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-purple-700">Open prescription editor</button>}
        </div>

        <div className="relative mb-6">
          <FiSearch
            className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400"
            size={20}
          />

          <input
            type="text"
            placeholder="Search prescriptions..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full rounded-lg border border-white/20 bg-white/40 py-3 pl-12 pr-4 focus:outline-none focus:ring-2 focus:ring-purple-500"
          />
        </div>

        {loading ? (
          <div className="py-12 text-center">
            <div className="mx-auto mb-3 h-12 w-12 animate-spin rounded-full border-b-2 border-purple-500" />
          </div>
        ) : (
          <div className="space-y-4">
            {visiblePrescriptions.length === 0 ? (
              <div className="rounded-xl border border-white/20 bg-white/40 py-12 text-center">
                <p className="text-gray-600">
                  No prescriptions found
                </p>
              </div>
            ) : (
              visiblePrescriptions.filter((rx) => JSON.stringify(rx).toLowerCase().includes(searchTerm.toLowerCase())).map((rx) => (
                <div
                  key={rx.id}
                  className="rounded-xl border border-white/20 bg-white/40 p-6 shadow-lg backdrop-blur-md transition-shadow hover:shadow-xl"
                >
                  <div className="flex items-start justify-between">
                    <div>
                      <h3 className="text-lg font-bold text-gray-900">
                        {rx.diagnosis || rx.symptoms || 'Finalized prescription'}
                      </h3>

                      <p className="mt-1 text-sm text-gray-600">
                        {rx.patientPhno} · {rx.patientType || 'OP'} · {rx.date}
                      </p>
                    </div>

                    <span className="rounded-full bg-green-100 px-3 py-1 text-sm font-semibold text-green-800">
                      Active
                    </span>
                  </div>
                  <div className="mt-4 rounded-lg bg-purple-50/70 p-4"><p className="mb-2 text-xs font-semibold uppercase tracking-wide text-purple-700">Medication</p>{(rx.medicines || []).length === 0 ? <p className="text-sm text-gray-600">No medication added.</p> : <div className="space-y-2">{rx.medicines.map((medicine, index) => <div key={index} className="flex flex-wrap justify-between gap-2 rounded-md bg-white/70 px-3 py-2 text-sm text-gray-700"><span className="font-semibold">{medicine.type || medicine.medicationType} {medicine.name || medicine.medicationName}</span><span>{medicine.dosage} · {medicine.quantity} · {medicine.duration} · {medicine.foodInstruction || medicine.food || ''}</span></div>)}</div>}</div>
                  {rx.patientType === 'IP' && <div className="mt-3 rounded-lg bg-amber-50/80 p-4 text-sm text-gray-700"><p className="mb-2 font-semibold text-amber-800">Inpatient care</p><p>Bed: {rx.inpatientDetails?.bedNumber || '-'} · Ward: {rx.inpatientDetails?.wardType || '-'}</p>{(rx.injections || []).map((injection, index) => <p key={index} className="mt-1">{injection.type} {injection.itemName} · {injection.route} · {injection.frequency}</p>)}</div>}
                </div>
              ))
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default PrescriptionsPage;