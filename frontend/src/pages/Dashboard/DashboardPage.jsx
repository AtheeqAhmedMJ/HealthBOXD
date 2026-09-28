import React, { useState, useEffect } from 'react';
import { dashboardAPI } from '../../services/api';
import StatCard from '../../components/StatCard/StatCard';
import ChartComponent from '../../components/Chart/ChartComponent';
import {
  FiCalendar,
  FiUsers,
  FiTrendingUp,
  FiCheckCircle,
  FiDollarSign,
  FiFileText,
  FiCreditCard,
} from 'react-icons/fi';
import { Link } from 'react-router-dom';

const DashboardPage = () => {
  const [analytics, setAnalytics] = useState({
    todayAppointments: 0,
    monthlyAppointments: 0,
    totalPatients: 0,
    recurringPatientPercentage: 0,
    graphData: [],
    paidRevenuePaise: 0,
    doctorEarnings: {},
  });

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [patientSummary, setPatientSummary] = useState(null);

  useEffect(() => {
    const fetchDashboardData = async () => {
      try {
        setLoading(true);

        const userRole = JSON.parse(
          localStorage.getItem('user')
        )?.role;

        if (userRole === 'ADMIN') {
          const data = await dashboardAPI.getAdminSummary();

          setAnalytics({
            todayAppointments: data.todaysAppointments || 0,
            monthlyAppointments: data.monthlyAppointments || 0,
            totalPatients: data.totalPatients || 0,
            recurringPatientPercentage:
              data.recurringPatientsPercentage || 0,
            graphData: Object.entries(data.graphData || {}).map(
              ([date, count]) => ({
                date,
                appointments: count,
              })
            ),
            paidRevenuePaise: data.paidRevenuePaise || 0,
            doctorEarnings: data.doctorEarnings || {},
          });
        } else {
          const data = await dashboardAPI.getPatientSummary();
          setPatientSummary(data);

          setAnalytics((prev) => ({
            ...prev,
            graphData: Object.entries(data.graphData || {}).map(
              ([date, count]) => ({
                date,
                value: count,
              })
            ),
          }));
        }
      } catch (err) {
        console.error('Failed to fetch dashboard data:', err);
        setError('Failed to load dashboard data');
      } finally {
        setLoading(false);
      }
    };

    fetchDashboardData();
  }, []);

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[calc(100vh-80px)]">
        <div className="text-center">
          <div className="animate-spin rounded-full h-16 w-16 border-2 border-white/20 border-t-[#1f1f2e] mx-auto mb-4" />

          <p className="text-gray-700 font-medium">
            Loading dashboard...
          </p>
        </div>
      </div>
    );
  }

  if (JSON.parse(localStorage.getItem('user') || '{}')?.role === 'PATIENT') {
    const next = patientSummary?.nextAppointment;
    return <div className="min-h-screen p-4 md:p-8"><div className="max-w-5xl mx-auto"><h1 className="text-4xl font-bold text-gray-900 mb-2">Patient Dashboard</h1><p className="text-gray-600 mb-8">Your appointments, payments, and finalized records.</p>{error && <div className="mb-6 p-4 bg-red-50 text-red-700 rounded-lg">{error}</div>}<div className="grid grid-cols-1 md:grid-cols-3 gap-5 mb-8"><StatCard title="Upcoming appointments" value={patientSummary?.upcomingAppointments || 0} icon={FiCalendar} color="bg-[rgba(31,31,46,0.52)]" /><StatCard title="Finalized prescriptions" value={patientSummary?.totalPrescriptions || 0} icon={FiFileText} color="bg-[rgba(31,31,46,0.52)]" /><StatCard title="Pending payments" value={patientSummary?.pendingPayments || 0} icon={FiCreditCard} color="bg-[rgba(31,31,46,0.52)]" /></div><div className="bg-white/70 rounded-2xl p-6 shadow mb-6"><h2 className="text-xl font-bold mb-3">Next appointment</h2>{next ? <p>{next.date} {next.time} · {next.status} · Doctor {next.doctorPhno || 'To be assigned'}</p> : <p className="text-gray-600">No upcoming appointments.</p>}</div><div className="flex gap-3"><Link className="px-4 py-3 rounded-lg bg-gray-900 text-white" to="/appointments">Book appointment</Link><Link className="px-4 py-3 rounded-lg border" to="/medical-records">Medical records</Link><Link className="px-4 py-3 rounded-lg border" to="/payments">Payments</Link></div></div></div>;
  }

  return (
    <div className="min-h-screen p-4 md:p-8">
      <div className="max-w-7xl mx-auto">

        <div className="mb-8">
          <h1 className="text-4xl font-bold text-gray-900 mb-2">
            Dashboard
          </h1>

          <p className="text-gray-600">
            Welcome back! Here's your healthcare overview
          </p>
        </div>

        {error && (
          <div className="mb-6 p-4 bg-red-500/10 backdrop-blur-md border border-red-400/30 rounded-xl text-red-700">
            {error}
          </div>
        )}

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
          <StatCard
            title="Today's Appointments"
            value={analytics.todayAppointments}
            icon={FiCalendar}
            color="bg-[rgba(31,31,46,0.52)]"
          />

          <StatCard
            title="Monthly Appointments"
            value={analytics.monthlyAppointments}
            icon={FiTrendingUp}
            color="bg-[rgba(31,31,46,0.52)]"
          />

          <StatCard
            title="Total Patients"
            value={analytics.totalPatients}
            icon={FiUsers}
            color="bg-[rgba(31,31,46,0.52)]"
          />

          <StatCard
            title="Recurring Patients"
            value={`${analytics.recurringPatientPercentage}%`}
            icon={FiCheckCircle}
            color="bg-[rgba(31,31,46,0.52)]"
          />
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2 bg-[rgba(225,210,232,0.72)] backdrop-blur-md rounded-xl p-6 shadow-lg border border-white/20">
            <h2 className="text-xl font-bold text-gray-900 mb-6">
              Appointment Trends
            </h2>

            <ChartComponent data={analytics.graphData} />
          </div>

          <div className="bg-purple-500/10 backdrop-blur-md rounded-xl p-6 shadow-lg border border-purple-300/30">
            <div className="flex items-center gap-3 mb-4">
              <div className="p-3 bg-purple-500/20 rounded-lg">
                <FiDollarSign
                  size={24}
                  className="text-purple-700"
                />
              </div>

              <div>
                <p className="text-sm text-gray-600">
                  Revenue
                </p>

                <h3 className="text-2xl font-bold text-gray-900">
                  ₹{(analytics.paidRevenuePaise / 100).toLocaleString('en-IN')}
                </h3>
              </div>
            </div>

            <p className="text-sm text-gray-600 mb-4">Verified successful payments</p>
          </div>
        </div>

        <section className="mt-6 rounded-xl border border-white/20 bg-white/40 p-6 shadow-lg backdrop-blur-md">
          <div className="mb-4 flex items-center justify-between"><div><h2 className="text-xl font-bold text-gray-900">Doctor earnings</h2><p className="text-sm text-gray-600">Verified payment totals for this clinic</p></div><span className="text-sm text-gray-600">{Object.keys(analytics.doctorEarnings).length} doctors</span></div>
          {Object.keys(analytics.doctorEarnings).length === 0 ? <p className="text-gray-600">No verified earnings yet.</p> : <div className="grid grid-cols-1 gap-3 md:grid-cols-2">{Object.entries(analytics.doctorEarnings).map(([doctor, amount]) => <div className="flex items-center justify-between rounded-lg bg-white/70 p-4" key={doctor}><span className="font-medium text-gray-800">Doctor {doctor}</span><span className="font-bold text-emerald-700">₹{(amount / 100).toLocaleString('en-IN')}</span></div>)}</div>}
        </section>

      </div>
    </div>
  );
};

export default DashboardPage;