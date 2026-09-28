import React, { lazy, Suspense, useEffect, useState } from 'react';
import {
  Routes,
  Route,
  useLocation,
  Navigate,
} from 'react-router-dom';

import Background from './components/Background/Background';
import Sidebar from './components/Sidebar/Sidebar';
import NotificationContainer from './components/Notification/NotificationContainer';
import ErrorBoundary from './components/ErrorBoundary/ErrorBoundary';
import PatientJourney from './components/PatientJourney/PatientJourney';

import { AppProvider, useApp } from './context/AppContext';

import {
  setupApiInterceptors,
  setupCustomHeaders,
} from './middleware/apiInterceptors';

// Pages
const LandingPage = lazy(() => import('./pages/Landing/LandingPage'));
const LoginPage = lazy(() => import('./pages/Auth/LoginPage'));
const RegisterPage = lazy(() => import('./pages/Auth/RegisterPage'));
const PatientAccessPage = lazy(() => import('./pages/Auth/PatientAccessPage'));
const DashboardPage = lazy(() => import('./pages/Dashboard/DashboardPage'));
const AppointmentsPage = lazy(() => import('./pages/Appointments/AppointmentsPage'));
const ConsultationEditor = lazy(() => import('./pages/Consultation/ConsultationEditor'));
const PatientsPage = lazy(() => import('./pages/Patients/PatientsPage'));
const MedicalRecordsPage = lazy(() => import('./pages/Patients/MedicalRecordsPage'));
const PrescriptionsPage = lazy(() => import('./pages/Prescriptions/PrescriptionsPage'));
const BillingPage = lazy(() => import('./pages/Billing/BillingPage'));
const PaymentsPage = lazy(() => import('./pages/Payments/PaymentsPage'));
const SettingsPage = lazy(() => import('./pages/Settings/SettingsPage'));
const SuperAdminPage = lazy(() => import('./pages/SuperAdmin/SuperAdminPage'));
const NotFoundPage = lazy(() => import('./pages/NotFound/NotFoundPage'));

/**
 * AppContent Component
 * Handles all routing, authentication, and layout logic
 */
const AppContent = () => {
  const location = useLocation();

  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [userRole, setUserRole] = useState(null);
  const [loading, setLoading] = useState(true);

  const { notify } = useApp();

  /**
   * Setup API interceptors and custom headers
   */
  useEffect(() => {
    if (typeof setupApiInterceptors === 'function') {
      setupApiInterceptors((message, type) => {
        if (notify && typeof notify[type] === 'function') {
          notify[type](message);
        }
      });
    }

    if (typeof setupCustomHeaders === 'function') {
      setupCustomHeaders();
    }
  }, [notify]);

  /**
   * Check authentication on mount
   */
  useEffect(() => {
    const token = localStorage.getItem('authToken');
    const user = localStorage.getItem('user');

    if (token && user) {
      try {
        const parsedUser = JSON.parse(user);
        setIsAuthenticated(true);
        setUserRole(parsedUser.role);
      } catch {
        // Clear invalid data
        localStorage.removeItem('user');
        localStorage.removeItem('authToken');
        setIsAuthenticated(false);
        setUserRole(null);
      }
    }

    setLoading(false);
  }, []);

  // Pages that don't show sidebar
  const authPages = ['/', '/login', '/register', '/patient-access'];

  // Show sidebar only when authenticated and not on auth pages
  const showSidebar =
    isAuthenticated &&
    !authPages.includes(location.pathname);

  // Loading state
  if (loading) {
    return (
      <div className="relative min-h-screen">
        <div className="relative z-10 flex min-h-screen items-center justify-center">
          <div className="text-center">
            <div className="mx-auto mb-4 h-12 w-12 animate-spin rounded-full border-4 border-white/40 border-t-purple-600" />
            <p className="font-medium text-gray-700">
              Loading HealthBoxD...
            </p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="relative min-h-screen">
      {showSidebar && (
        <Sidebar userRole={userRole} />
      )}

      <ErrorBoundary>
        <Suspense fallback={<div className="flex min-h-screen items-center justify-center">Loading...</div>}>
          <Routes>
          {/* Public Routes */}
          <Route
            path="/"
            element={<LandingPage />}
          />

          <Route
            path="/login"
            element={<LoginPage />}
          />

          <Route
            path="/register"
            element={<RegisterPage />}
          />

          <Route
            path="/patient-access"
            element={<PatientAccessPage />}
          />

          {/* Protected Routes */}
          {isAuthenticated ? (
            <>
              <Route
                path="/dashboard"
                element={<DashboardPage />}
              />

              <Route
                path="/appointments"
                element={<AppointmentsPage />}
              />

              <Route path="/consultation" element={<Navigate to="/appointments" replace />} />
              <Route path="/consultation/with" element={<Navigate to="/appointments" replace />} />
              <Route path="/consultation/without" element={<Navigate to="/appointments" replace />} />

              <Route
                path="/consultation/editor"
                element={<ConsultationEditor />}
              />

              <Route
                path="/patients"
                element={<PatientsPage />}
              />

              <Route
                path="/medical-records"
                element={<MedicalRecordsPage />}
              />

              <Route
                path="/prescriptions"
                element={<PrescriptionsPage />}
              />

              <Route
                path="/billing"
                element={<BillingPage />}
              />

              <Route
                path="/payments"
                element={<PaymentsPage />}
              />

              <Route path="/pharmacy" element={<Navigate to="/prescriptions" replace />} />
              <Route path="/scheduling" element={<Navigate to="/settings" replace />} />

              {/* Admin only routes */}
              <Route path="/charges" element={<Navigate to="/settings" replace />} />

              <Route
                path="/settings"
                element={<SettingsPage />}
              />
              <Route path="/superadmin" element={userRole === 'SUPER_ADMIN' ? <SuperAdminPage /> : <Navigate to="/dashboard" replace />} />
            </>
          ) : (
            <>
              {/* Redirect protected routes to login when not authenticated */}
              <Route path="/dashboard" element={<Navigate to="/login" replace />} />
              <Route path="/appointments" element={<Navigate to="/login" replace />} />
              <Route path="/consultation" element={<Navigate to="/login" replace />} />
              <Route path="/consultation/with" element={<Navigate to="/login" replace />} />
              <Route path="/consultation/without" element={<Navigate to="/login" replace />} />
              <Route path="/consultation/editor" element={<Navigate to="/login" replace />} />
              <Route path="/patients" element={<Navigate to="/login" replace />} />
              <Route path="/medical-records" element={<Navigate to="/login" replace />} />
              <Route path="/prescriptions" element={<Navigate to="/login" replace />} />
              <Route path="/billing" element={<Navigate to="/login" replace />} />
              <Route path="/payments" element={<Navigate to="/login" replace />} />
              <Route path="/pharmacy" element={<Navigate to="/login" replace />} />
              <Route path="/scheduling" element={<Navigate to="/login" replace />} />
              <Route path="/charges" element={<Navigate to="/login" replace />} />
              <Route path="/settings" element={<Navigate to="/login" replace />} />
              <Route path="/superadmin" element={<Navigate to="/login" replace />} />
            </>
          )}

          {/* 404 Catch-all */}
          <Route
            path="*"
            element={<NotFoundPage />}
          />
          </Routes>
        </Suspense>
      </ErrorBoundary>

      <NotificationContainer />
      {isAuthenticated && <PatientJourney />}
    </div>
  );
};

/**
 * Main App Component
 * Provides global context and renders AppContent
 * Background is placed at top level for global coverage
 */
const App = () => {
  return (
    <>
      {/* Global Background - covers entire app, z-index 0 */}
      <Background />

      {/* App content with relative z-positioning */}
      <AppProvider>
        <AppContent />
      </AppProvider>
    </>
  );
};

export default App;