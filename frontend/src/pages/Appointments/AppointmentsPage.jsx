import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { appointmentsAPI } from '../../services/api';
import { useApp } from '../../context/AppContext';
import { FiPlus, FiSearch, FiCalendar, FiUser, FiPhone, FiClock, FiTrash2 } from 'react-icons/fi';

const AppointmentsPage = () => {
  const navigate = useNavigate();
  const { setSelectedPatient, updateWorkflow } = useApp();
  const [appointments, setAppointments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [formData, setFormData] = useState({
    patientPhno: '',
    doctorPhno: '',
    patientName: '',
    date: '',
    appointmentTime: '',
    reason: '',
    appointmentType: 'PRE_BOOKED',
  });

  useEffect(() => {
    fetchAppointments();
  }, []);

  const fetchAppointments = async () => {
    try {
      setLoading(true);
      const data = await appointmentsAPI.getAll();
      setAppointments(data);
    } catch (err) {
      setError('Failed to fetch appointments');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateAppointment = async (e) => {
    e.preventDefault();
    try {
      await appointmentsAPI.create(formData);
      setFormData({
        patientPhno: '',
        doctorPhno: '',
        patientName: '',
        date: '',
        appointmentTime: '',
        reason: '',
      });
      setShowForm(false);
      fetchAppointments();
    } catch (err) {
      setError('Failed to create appointment');
    }
  };

  const handleDeleteAppointment = async (id) => {
    if (window.confirm('Are you sure you want to delete this appointment?')) {
      try {
        await appointmentsAPI.delete(id);
        fetchAppointments();
      } catch (err) {
        setError('Failed to delete appointment');
      }
    }
  };

  const filteredAppointments = appointments.filter(app =>
    app.patientPhno?.includes(searchTerm) ||
    app.doctorPhno?.includes(searchTerm) ||
    app.reason?.toLowerCase().includes(searchTerm.toLowerCase())
  );

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[calc(100vh-80px)]">
        <div className="text-center">
          <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-purple-500 mx-auto mb-4"></div>
          <p className="text-gray-600">Loading appointments...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-purple-50/50 via-pink-50/30 to-white p-4 md:p-8">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-4xl font-bold text-gray-900 flex items-center gap-3">
              <FiCalendar className="text-purple-600" />
              Appointments
            </h1>
            <p className="text-gray-600 mt-1">Manage and schedule patient appointments</p>
          </div>
          <button
            onClick={() => setShowForm(!showForm)}
            className="flex items-center gap-2 px-6 py-3 bg-gradient-to-r from-purple-500 to-pink-500 text-white font-semibold rounded-lg hover:shadow-lg transition-all transform hover:scale-105"
          >
            <FiPlus size={20} />
            New Appointment
          </button>
        </div>

        {error && (
          <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg text-red-700">
            {error}
          </div>
        )}

        {/* Create Form */}
        {showForm && (
          <div className="mb-8 bg-white/40 backdrop-blur-md rounded-xl p-6 shadow-lg border border-white/20">
            <h2 className="text-2xl font-bold text-gray-900 mb-6">Schedule New Appointment</h2>
            <form onSubmit={handleCreateAppointment} className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-2">
                  Patient Phone
                </label>
                <input
                  type="tel"
                  required
                  value={formData.patientPhno}
                  onChange={(e) => setFormData({ ...formData, patientPhno: e.target.value })}
                  className="w-full px-4 py-3 bg-white/50 border border-white/30 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500"
                  placeholder="9876543210"
                />
              </div>

              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-2">
                  Doctor Phone
                </label>
                <input
                  type="tel"
                  required
                  value={formData.doctorPhno}
                  onChange={(e) => setFormData({ ...formData, doctorPhno: e.target.value })}
                  className="w-full px-4 py-3 bg-white/50 border border-white/30 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500"
                  placeholder="9876543210"
                />
              </div>

              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-2">
                  Date
                </label>
                <input
                  type="date"
                  required
                  value={formData.date}
                  onChange={(e) => setFormData({ ...formData, date: e.target.value })}
                  className="w-full px-4 py-3 bg-white/50 border border-white/30 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500"
                />
              </div>

              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-2">
                  Time
                </label>
                <input
                  type="time"
                  required
                  value={formData.appointmentTime}
                  onChange={(e) => setFormData({ ...formData, appointmentTime: e.target.value })}
                  className="w-full px-4 py-3 bg-white/50 border border-white/30 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500"
                />
              </div>

              <div className="md:col-span-2">
                <label className="block text-sm font-semibold text-gray-700 mb-2">
                  Reason for Visit
                </label>
                <textarea
                  value={formData.reason}
                  onChange={(e) => setFormData({ ...formData, reason: e.target.value })}
                  className="w-full px-4 py-3 bg-white/50 border border-white/30 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500"
                  placeholder="Describe the reason for appointment"
                  rows="3"
                />
              </div>

              <div className="md:col-span-2 flex gap-3">
                <button
                  type="submit"
                  className="flex-1 py-3 bg-gradient-to-r from-purple-500 to-pink-500 text-white font-semibold rounded-lg hover:shadow-lg transition-all"
                >
                  Schedule Appointment
                </button>
                <button
                  type="button"
                  onClick={() => setShowForm(false)}
                  className="flex-1 py-3 bg-gray-300/40 text-gray-700 font-semibold rounded-lg hover:bg-gray-300/60 transition-all"
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        )}

        {/* Search Bar */}
        <div className="mb-6 relative">
          <FiSearch className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={20} />
          <input
            type="text"
            placeholder="Search by patient/doctor phone or reason..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-12 pr-4 py-3 bg-white/40 border border-white/20 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500"
          />
        </div>

        {/* Appointments List */}
        <div className="space-y-4">
          {filteredAppointments.length === 0 ? (
            <div className="text-center py-12 bg-white/40 rounded-xl border border-white/20">
              <FiCalendar className="mx-auto mb-3 text-gray-400" size={32} />
              <p className="text-gray-600 font-medium">No appointments found</p>
            </div>
          ) : (
            filteredAppointments.map((appointment) => (
              <div
                key={appointment.id}
                onClick={() => {
                  setSelectedPatient({
                    id: appointment.patientId,
                    name: appointment.patientName,
                    phno: appointment.patientPhno,
                    appointmentId: appointment.id,
                  });
                  updateWorkflow({ appointment: true });
                  navigate('/patients');
                }}
                className="bg-white/40 backdrop-blur-md rounded-xl p-6 shadow-lg border border-white/20 hover:shadow-xl transition-shadow"
              >
                <div className="grid grid-cols-1 md:grid-cols-5 gap-4 items-center">
                  <div>
                    <p className="text-xs text-gray-600 uppercase">Patient</p>
                    <p className="flex items-center gap-2 text-gray-900 font-semibold mt-1">
                      <FiPhone size={16} className="text-purple-600" />
                      {appointment.patientPhno}
                    </p>
                  </div>

                  <div>
                    <p className="text-xs text-gray-600 uppercase">Doctor</p>
                    <p className="flex items-center gap-2 text-gray-900 font-semibold mt-1">
                      <FiUser size={16} className="text-purple-600" />
                      {appointment.doctorPhno || 'Unassigned'}
                    </p>
                  </div>

                  <div>
                    <p className="text-xs text-gray-600 uppercase">Date & Time</p>
                    <p className="flex items-center gap-2 text-gray-900 font-semibold mt-1">
                      <FiClock size={16} className="text-purple-600" />
                      {new Date(appointment.date).toLocaleDateString()} {appointment.appointmentTime}
                    </p>
                  </div>

                  <div>
                    <p className="text-xs text-gray-600 uppercase">Reason</p>
                    <p className="text-gray-700 mt-1 line-clamp-2">{appointment.reason || 'General consultation'}</p>
                  </div>

                  <div className="flex justify-end">
                    <button
                      onClick={(event) => {
                        event.stopPropagation();
                        setSelectedPatient({ name: appointment.patientName, phno: appointment.patientPhno, appointmentId: appointment.id });
                        updateWorkflow({ appointment: true });
                        navigate(`/consultation/editor?phno=${encodeURIComponent(appointment.patientPhno)}&appointmentId=${appointment.id}`);
                      }}
                      className="p-3 bg-purple-500/20 hover:bg-purple-500/30 text-purple-700 rounded-lg transition-all"
                      aria-label="Open consultation"
                    >
                      <FiUser size={20} />
                    </button>
                    <button
                      onClick={() => handleDeleteAppointment(appointment.id)}
                      className="p-3 bg-red-500/20 hover:bg-red-500/30 text-red-600 rounded-lg transition-all"
                    >
                      <FiTrash2 size={20} />
                    </button>
                  </div>
                </div>
              </div>
            ))
          )}
        </div>

        {/* Stats */}
        <div className="mt-8 grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="bg-white/40 backdrop-blur-md rounded-xl p-6 border border-white/20 text-center">
            <p className="text-sm text-gray-600 mb-2">Total Appointments</p>
            <p className="text-3xl font-bold text-gray-900">{appointments.length}</p>
          </div>
          <div className="bg-white/40 backdrop-blur-md rounded-xl p-6 border border-white/20 text-center">
            <p className="text-sm text-gray-600 mb-2">Upcoming</p>
            <p className="text-3xl font-bold text-purple-600">
              {appointments.filter(a => new Date(a.appointmentDate) > new Date()).length}
            </p>
          </div>
          <div className="bg-white/40 backdrop-blur-md rounded-xl p-6 border border-white/20 text-center">
            <p className="text-sm text-gray-600 mb-2">Completed</p>
            <p className="text-3xl font-bold text-green-600">
              {appointments.filter(a => new Date(a.appointmentDate) < new Date()).length}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AppointmentsPage;
