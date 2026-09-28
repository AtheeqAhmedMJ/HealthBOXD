import React, { useState, useEffect } from 'react';
import { patientsAPI } from '../../services/api';
import { useApp } from '../../context/AppContext';
import { FiUsers, FiSearch, FiPhone, FiCalendar } from 'react-icons/fi';
import { useNavigate } from 'react-router-dom';

const PatientsPage = () => {
  const [patients, setPatients] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const { selectedPatient, setSelectedPatient } = useApp();
  const navigate = useNavigate();

  useEffect(() => {
    fetchPatients();
  }, []);

  const fetchPatients = async () => {
    try {
      const data = await patientsAPI.getAll();
      setPatients(data);
    } catch (err) {
      console.error('Failed to fetch patients:', err);
    } finally {
      setLoading(false);
    }
  };

  const filteredPatients = patients.filter(patient =>
    patient.phno?.includes(searchTerm) ||
    patient.name?.toLowerCase().includes(searchTerm.toLowerCase())
  );

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[calc(100vh-80px)]">
        <div className="text-center">
          <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-purple-500 mx-auto mb-4"></div>
          <p className="text-gray-600">Loading patients...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-purple-50/50 via-pink-50/30 to-white p-4 md:p-8">
      <div className="max-w-7xl mx-auto">
        {selectedPatient && (
          <div className="mb-6 rounded-xl border border-purple-200 bg-purple-50/70 p-4">
            <p className="text-xs font-semibold uppercase text-purple-600">Current patient</p>
            <p className="text-xl font-bold text-gray-900">{selectedPatient.name || 'Patient'} <span className="text-sm font-normal text-gray-600">{selectedPatient.phno}</span></p>
            <p className="mt-1 text-sm text-gray-600">Continue the appointment workflow from the patient journey panel.</p>
            <button type="button" onClick={() => navigate('/medical-records')} className="mt-3 rounded-lg bg-gray-900 px-3 py-2 text-xs font-semibold text-white">Open completed records</button>
          </div>
        )}
        <div className="mb-8">
          <h1 className="text-4xl font-bold text-gray-900 flex items-center gap-3">
            <FiUsers className="text-purple-600" />
            Patients
          </h1>
          <p className="text-gray-600 mt-1">Manage patient records and information</p>
        </div>

        <div className="mb-6 relative">
          <FiSearch className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={20} />
          <input
            type="text"
            placeholder="Search patients by name or phone..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-12 pr-4 py-3 bg-white/40 border border-white/20 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500"
          />
        </div>

        <div className="space-y-4">
          {filteredPatients.length === 0 ? (
            <div className="text-center py-12 bg-white/40 rounded-xl border border-white/20">
              <p className="text-gray-600">No patients found</p>
            </div>
          ) : (
            filteredPatients.map((patient) => (
              <button type="button" key={patient.id} onClick={() => setSelectedPatient({ id: patient.id, name: patient.name, phno: patient.phno })} className="block w-full text-left bg-white/40 backdrop-blur-md rounded-xl p-6 shadow-lg border border-white/20 hover:shadow-xl transition-shadow">
                <div className="grid grid-cols-1 md:grid-cols-4 gap-4 items-center">
                  <div>
                    <p className="text-xs text-gray-600 uppercase">Name</p>
                    <p className="text-gray-900 font-semibold mt-1">{patient.name || 'N/A'}</p>
                  </div>
                  <div>
                    <p className="text-xs text-gray-600 uppercase">Phone</p>
                    <p className="flex items-center gap-2 text-gray-900 font-semibold mt-1">
                      <FiPhone size={16} className="text-purple-600" />
                      {patient.phno}
                    </p>
                  </div>
                  <div>
                    <p className="text-xs text-gray-600 uppercase">Age</p>
                    <p className="text-gray-900 font-semibold mt-1">{patient.age || 'N/A'}</p>
                  </div>
                  <div>
                    <p className="text-xs text-gray-600 uppercase">Member Since</p>
                    <p className="flex items-center gap-2 text-gray-900 font-semibold mt-1">
                      <FiCalendar size={16} className="text-purple-600" />
                      {new Date(patient.createdAt).toLocaleDateString()}
                    </p>
                  </div>
                </div>
              </button>
            ))
          )}
        </div>

        <div className="mt-8 text-center">
          <p className="text-gray-600 font-medium">Total Patients: <span className="text-2xl text-purple-600 font-bold">{patients.length}</span></p>
        </div>
      </div>
    </div>
  );
};

export default PatientsPage;
