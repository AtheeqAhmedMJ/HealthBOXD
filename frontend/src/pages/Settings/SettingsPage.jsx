import React, { useState, useEffect, useContext } from 'react';
import { FiSettings, FiUser, FiMail, FiPhone, FiLock, FiLogOut, FiSave, FiX, FiEdit2 } from 'react-icons/fi';
import { useNavigate } from 'react-router-dom';
import { AppContext } from '../../context/AppContext';
import axios from 'axios';
import { authAPI, setAuthToken } from '../../services/api';
import ChargesPage from '../Charges/ChargesPage';
import PaymentsPage from '../Payments/PaymentsPage';
import SchedulingPage from '../Scheduling/SchedulingPage';
import MedicalRecordsPage from '../Patients/MedicalRecordsPage';

const SettingsPage = () => {
  const { user, setUser } = useContext(AppContext);
  const navigate = useNavigate();
  
  const [profileData, setProfileData] = useState({
    username: '',
    email: '',
    phno: '',
    hospitalName: '',
    hospitalCode: ''
  });
  
  const [passwordData, setPasswordData] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  });

  const [editMode, setEditMode] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [activeTab, setActiveTab] = useState('profile');
  const [clinics, setClinics] = useState([]);
  const [clinicMessage, setClinicMessage] = useState('');

  const API_URL = (import.meta.env.VITE_API_URL || 'http://localhost:8080/api').replace(/\/api$/, '');
  const token = localStorage.getItem('authToken');

  // Load user profile on mount
  useEffect(() => {
    fetchUserProfile();
    if (user?.role === 'ADMIN') authAPI.clinics().then(setClinics).catch(() => setClinics([]));
  }, []);

  const fetchUserProfile = async () => {
    try {
      setLoading(true);
      const response = await axios.get(`${API_URL}/auth/profile`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      
      const userData = response.data;
      setProfileData({
        username: userData.username || '',
        email: userData.email || '',
        phno: userData.phno || '',
        hospitalName: userData.hospitalName || '',
        hospitalCode: userData.hospitalCode || ''
      });
    } catch (err) {
      setError('Failed to load profile data');
      console.error('Error fetching profile:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleProfileChange = (field, value) => {
    setProfileData(prev => ({
      ...prev,
      [field]: value
    }));
    setError('');
  };

  const handlePasswordChange = (field, value) => {
    setPasswordData(prev => ({
      ...prev,
      [field]: value
    }));
    setError('');
  };

  const saveProfileChanges = async () => {
    setLoading(true);
    setError('');
    setSuccess('');

    try {
      const updateData = {
        username: profileData.username,
        email: profileData.email,
        phno: profileData.phno
      };

      const response = await axios.patch(`${API_URL}/auth/profile`, updateData, {
        headers: { Authorization: `Bearer ${token}` }
      });

      setSuccess('Profile updated successfully!');
      setEditMode(false);
      
      // Update context
      if (setUser) {
        setUser(prev => ({
          ...prev,
          ...updateData
        }));
      }

      setTimeout(() => setSuccess(''), 5000);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update profile');
      console.error('Error updating profile:', err);
    } finally {
      setLoading(false);
    }
  };

  const changePassword = async () => {
    setError('');
    setSuccess('');

    // Validation
    if (!passwordData.currentPassword || !passwordData.newPassword || !passwordData.confirmPassword) {
      setError('All password fields are required');
      return;
    }

    if (passwordData.newPassword !== passwordData.confirmPassword) {
      setError('New passwords do not match');
      return;
    }

    if (passwordData.newPassword.length < 8) {
      setError('Password must be at least 8 characters');
      return;
    }

    setLoading(true);

    try {
      await axios.post(
        `${API_URL}/auth/change-password`,
        {
          currentPassword: passwordData.currentPassword,
          newPassword: passwordData.newPassword
        },
        { headers: { Authorization: `Bearer ${token}` } }
      );

      setSuccess('Password changed successfully!');
      setPasswordData({
        currentPassword: '',
        newPassword: '',
        confirmPassword: ''
      });

      setTimeout(() => setSuccess(''), 5000);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to change password');
      console.error('Error changing password:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = async () => {
    try {
      await axios.post(
        `${API_URL}/auth/logout`,
        {},
        { headers: { Authorization: `Bearer ${token}` } }
      );
    } catch (err) {
      console.error('Logout error:', err);
    } finally {
      localStorage.removeItem('authToken');
      localStorage.removeItem('user');
      navigate('/');
    }
  };

  const switchClinic = async (hospitalId) => {
    try {
      const login = await authAPI.switchClinic(hospitalId);
      setAuthToken(login.token);
      const nextUser = { username: login.username, role: login.role, hospitalId: login.hospitalId, phno: login.phno };
      localStorage.setItem('user', JSON.stringify(nextUser));
      setClinicMessage('Clinic switched. Future active appointments in the previous clinic were paused.');
      window.location.reload();
    } catch (requestError) {
      setClinicMessage(requestError.response?.data?.message || 'Could not switch clinic.');
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-purple-50/50 via-pink-50/30 to-white p-4 md:p-8">
      <div className="max-w-4xl mx-auto">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-4xl font-bold text-gray-900 flex items-center gap-3">
            <FiSettings className="text-purple-600" size={36} />
            Settings
          </h1>
          <p className="text-gray-600 mt-2">Manage your account settings and preferences</p>
        </div>

        {error && (
          <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-6 text-red-800 flex justify-between items-center">
            <span>{error}</span>
            <button onClick={() => setError('')} className="text-red-600 hover:text-red-800">
              <FiX size={20} />
            </button>
          </div>
        )}

        {success && (
          <div className="bg-green-50 border border-green-200 rounded-lg p-4 mb-6 text-green-800 flex justify-between items-center">
            <span>{success}</span>
            <button onClick={() => setSuccess('')} className="text-green-600 hover:text-green-800">
              <FiX size={20} />
            </button>
          </div>
        )}

        {/* Tabs */}
        <div className="flex gap-2 mb-8 border-b border-white/20">
          <button
            onClick={() => setActiveTab('profile')}
            className={`px-6 py-3 font-medium border-b-2 transition-all ${
              activeTab === 'profile'
                ? 'border-purple-600 text-purple-600'
                : 'border-transparent text-gray-600 hover:text-gray-900'
            }`}
          >
            <FiUser className="inline mr-2" size={18} />
            Profile
          </button>
          <button
            onClick={() => setActiveTab('security')}
            className={`px-6 py-3 font-medium border-b-2 transition-all ${
              activeTab === 'security'
                ? 'border-purple-600 text-purple-600'
                : 'border-transparent text-gray-600 hover:text-gray-900'
            }`}
          >
            <FiLock className="inline mr-2" size={18} />
            Security
          </button>
          <button
            onClick={() => setActiveTab('charges')}
            className={`px-6 py-3 font-medium border-b-2 transition-all ${activeTab === 'charges' ? 'border-purple-600 text-purple-600' : 'border-transparent text-gray-600 hover:text-gray-900'}`}
          >
            Charges
          </button>
          <button
            onClick={() => setActiveTab('schedule')}
            className={`px-6 py-3 font-medium border-b-2 transition-all ${activeTab === 'schedule' ? 'border-purple-600 text-purple-600' : 'border-transparent text-gray-600 hover:text-gray-900'}`}
          >
            Schedule
          </button>
          <button
            onClick={() => setActiveTab('records')}
            className={`px-6 py-3 font-medium border-b-2 transition-all ${activeTab === 'records' ? 'border-purple-600 text-purple-600' : 'border-transparent text-gray-600 hover:text-gray-900'}`}
          >
            Patient Records
          </button>
          <button
            onClick={() => setActiveTab('payments')}
            className={`px-6 py-3 font-medium border-b-2 transition-all ${activeTab === 'payments' ? 'border-purple-600 text-purple-600' : 'border-transparent text-gray-600 hover:text-gray-900'}`}
          >
            Payment History
          </button>
        </div>

        {activeTab === 'charges' && <ChargesPage />}
        {activeTab === 'payments' && <PaymentsPage />}
        {activeTab === 'schedule' && <SchedulingPage />}
        {activeTab === 'records' && <MedicalRecordsPage />}

        {user?.role === 'ADMIN' && activeTab === 'profile' && clinics.length > 1 && <div className="mt-6 rounded-xl border border-white/20 bg-white/40 p-8 shadow-lg backdrop-blur-md"><h2 className="mb-2 text-2xl font-bold text-gray-900">Active clinic</h2><p className="mb-4 text-sm text-gray-600">Switching clinic pauses future active appointments in the clinic you leave.</p>{clinicMessage && <p className="mb-4 rounded bg-amber-50 p-3 text-amber-800">{clinicMessage}</p>}<div className="space-y-2">{clinics.map((clinic) => <button type="button" key={clinic.id} disabled={clinic.active || clinic.approvalStatus !== 'APPROVED'} onClick={() => switchClinic(clinic.id)} className={`flex w-full items-center justify-between rounded-lg border p-4 text-left ${clinic.active ? 'border-purple-300 bg-purple-50' : 'border-white/30 bg-white/70 hover:bg-white'}`}><span><strong className="block text-gray-900">{clinic.name}</strong><span className="text-sm text-gray-600">{clinic.location || clinic.code}</span></span><span className="text-xs font-semibold text-gray-500">{clinic.active ? 'ACTIVE' : clinic.approvalStatus}</span></button>)}</div></div>}

        {/* Profile Tab */}
        {activeTab === 'profile' && (
          <div className="space-y-6">
            {/* User Info Card */}
            <div className="bg-white/40 backdrop-blur-md rounded-xl shadow-lg border border-white/20 p-8">
              <div className="flex items-start justify-between mb-6">
                <div>
                  <h2 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
                    <div className="w-12 h-12 rounded-full bg-gradient-to-br from-purple-400 to-pink-400 flex items-center justify-center text-white text-lg font-bold">
                      {profileData.username?.charAt(0)?.toUpperCase()}
                    </div>
                    {profileData.username}
                  </h2>
                  <p className="text-gray-600 mt-2">Hospital: {profileData.hospitalName || 'N/A'}</p>
                </div>
                {!editMode && (
                  <button
                    onClick={() => setEditMode(true)}
                    className="flex items-center gap-2 bg-purple-600 hover:bg-purple-700 text-white px-4 py-2 rounded-lg transition-colors"
                  >
                    <FiEdit2 size={18} />
                    Edit Profile
                  </button>
                )}
              </div>

              {/* Profile Form */}
              <div className="space-y-6">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  {/* Username */}
                  <div>
                    <label className="block text-sm font-medium text-gray-900 mb-2">
                      <FiUser className="inline mr-2" size={16} />
                      Username
                    </label>
                    <input
                      type="text"
                      value={profileData.username}
                      onChange={(e) => handleProfileChange('username', e.target.value)}
                      disabled={!editMode}
                      className={`w-full px-4 py-2 rounded-lg border transition-all ${
                        editMode
                          ? 'border-purple-300 bg-white focus:outline-none focus:ring-2 focus:ring-purple-500'
                          : 'border-gray-300 bg-gray-50 text-gray-600'
                      }`}
                    />
                  </div>

                  {/* Phone */}
                  <div>
                    <label className="block text-sm font-medium text-gray-900 mb-2">
                      <FiPhone className="inline mr-2" size={16} />
                      Phone Number
                    </label>
                    <input
                      type="tel"
                      value={profileData.phno}
                      onChange={(e) => handleProfileChange('phno', e.target.value)}
                      disabled={!editMode}
                      className={`w-full px-4 py-2 rounded-lg border transition-all ${
                        editMode
                          ? 'border-purple-300 bg-white focus:outline-none focus:ring-2 focus:ring-purple-500'
                          : 'border-gray-300 bg-gray-50 text-gray-600'
                      }`}
                    />
                  </div>

                  {/* Email */}
                  <div className="md:col-span-2">
                    <label className="block text-sm font-medium text-gray-900 mb-2">
                      <FiMail className="inline mr-2" size={16} />
                      Email Address
                    </label>
                    <input
                      type="email"
                      value={profileData.email}
                      onChange={(e) => handleProfileChange('email', e.target.value)}
                      disabled={!editMode}
                      className={`w-full px-4 py-2 rounded-lg border transition-all ${
                        editMode
                          ? 'border-purple-300 bg-white focus:outline-none focus:ring-2 focus:ring-purple-500'
                          : 'border-gray-300 bg-gray-50 text-gray-600'
                      }`}
                    />
                  </div>

                  {/* Hospital Name */}
                  <div>
                    <label className="block text-sm font-medium text-gray-900 mb-2">Hospital Name</label>
                    <input
                      type="text"
                      value={profileData.hospitalName}
                      disabled
                      className="w-full px-4 py-2 rounded-lg border border-gray-300 bg-gray-50 text-gray-600"
                    />
                  </div>

                  {/* Hospital Code */}
                  <div>
                    <label className="block text-sm font-medium text-gray-900 mb-2">Hospital Code</label>
                    <input
                      type="text"
                      value={profileData.hospitalCode}
                      disabled
                      className="w-full px-4 py-2 rounded-lg border border-gray-300 bg-gray-50 text-gray-600"
                    />
                  </div>
                </div>

                {/* Action Buttons */}
                {editMode && (
                  <div className="flex gap-3 pt-6 border-t border-white/20">
                    <button
                      onClick={saveProfileChanges}
                      disabled={loading}
                      className="flex-1 bg-purple-600 hover:bg-purple-700 text-white py-2 rounded-lg font-medium flex items-center justify-center gap-2 transition-colors disabled:opacity-50"
                    >
                      <FiSave size={18} />
                      Save Changes
                    </button>
                    <button
                      onClick={() => {
                        setEditMode(false);
                        fetchUserProfile();
                      }}
                      className="flex-1 bg-gray-300 hover:bg-gray-400 text-gray-900 py-2 rounded-lg font-medium transition-colors"
                    >
                      Cancel
                    </button>
                  </div>
                )}
              </div>
            </div>
          </div>
        )}

        {/* Security Tab */}
        {activeTab === 'security' && (
          <div className="space-y-6">
            {/* Change Password Card */}
            <div className="bg-white/40 backdrop-blur-md rounded-xl shadow-lg border border-white/20 p-8">
              <h2 className="text-2xl font-bold text-gray-900 mb-6 flex items-center gap-2">
                <FiLock className="text-purple-600" size={24} />
                Change Password
              </h2>

              <div className="space-y-6">
                {/* Current Password */}
                <div>
                  <label className="block text-sm font-medium text-gray-900 mb-2">Current Password</label>
                  <input
                    type="password"
                    value={passwordData.currentPassword}
                    onChange={(e) => handlePasswordChange('currentPassword', e.target.value)}
                    className="w-full px-4 py-2 rounded-lg border border-purple-300 bg-white focus:outline-none focus:ring-2 focus:ring-purple-500"
                    placeholder="Enter your current password"
                  />
                </div>

                {/* New Password */}
                <div>
                  <label className="block text-sm font-medium text-gray-900 mb-2">New Password</label>
                  <input
                    type="password"
                    value={passwordData.newPassword}
                    onChange={(e) => handlePasswordChange('newPassword', e.target.value)}
                    className="w-full px-4 py-2 rounded-lg border border-purple-300 bg-white focus:outline-none focus:ring-2 focus:ring-purple-500"
                    placeholder="Enter your new password (min 8 characters)"
                  />
                  <p className="text-xs text-gray-600 mt-1">Must be at least 8 characters long</p>
                </div>

                {/* Confirm Password */}
                <div>
                  <label className="block text-sm font-medium text-gray-900 mb-2">Confirm New Password</label>
                  <input
                    type="password"
                    value={passwordData.confirmPassword}
                    onChange={(e) => handlePasswordChange('confirmPassword', e.target.value)}
                    className="w-full px-4 py-2 rounded-lg border border-purple-300 bg-white focus:outline-none focus:ring-2 focus:ring-purple-500"
                    placeholder="Confirm your new password"
                  />
                </div>

                {/* Submit Button */}
                <div className="pt-6 border-t border-white/20">
                  <button
                    onClick={changePassword}
                    disabled={loading}
                    className="w-full bg-purple-600 hover:bg-purple-700 text-white py-2 rounded-lg font-medium transition-colors disabled:opacity-50"
                  >
                    {loading ? 'Updating Password...' : 'Update Password'}
                  </button>
                </div>
              </div>
            </div>

            {/* Logout Card */}
            <div className="bg-white/40 backdrop-blur-md rounded-xl shadow-lg border border-white/20 p-8">
              <h2 className="text-2xl font-bold text-gray-900 mb-4">Sign Out</h2>
              <p className="text-gray-600 mb-6">
                You'll be logged out of your account and will need to sign in again to access HealthBoxD.
              </p>
              <button
                onClick={handleLogout}
                className="w-full bg-red-600 hover:bg-red-700 text-white py-2 rounded-lg font-medium flex items-center justify-center gap-2 transition-colors"
              >
                <FiLogOut size={18} />
                Sign Out
              </button>
            </div>

            {/* Account Info */}
            <div className="bg-white/40 backdrop-blur-md rounded-xl shadow-lg border border-white/20 p-8">
              <h2 className="text-2xl font-bold text-gray-900 mb-6">Account Information</h2>
              <div className="space-y-4">
                <div className="flex justify-between items-center py-3 border-b border-white/20">
                  <span className="text-gray-600 font-medium">Role</span>
                  <span className="text-gray-900 font-bold capitalize">{user?.role || 'User'}</span>
                </div>
                <div className="flex justify-between items-center py-3 border-b border-white/20">
                  <span className="text-gray-600 font-medium">Account Created</span>
                  <span className="text-gray-900 font-bold">
                    {new Date().toLocaleDateString('en-IN')}
                  </span>
                </div>
                <div className="flex justify-between items-center py-3">
                  <span className="text-gray-600 font-medium">Hospital ID</span>
                  <span className="text-gray-900 font-bold">{user?.hospitalId || 'N/A'}</span>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default SettingsPage;