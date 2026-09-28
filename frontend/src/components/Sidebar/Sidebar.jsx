import React, { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import './Sidebar.css';
import {
  FiHome,
  FiUsers,
  FiFileText,
  FiDollarSign,
  FiCreditCard,
  FiSettings,
  FiLogOut,
  FiCalendar,
} from 'react-icons/fi';

const Sidebar = ({ userRole }) => {
  const [isExpanded, setIsExpanded] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();

  const isActive = (path) => location.pathname.startsWith(path);

  const handleLogout = () => {
    localStorage.removeItem('authToken');
    localStorage.removeItem('user');
    navigate('/login');
  };

  const patientItems = [
    { icon: FiHome, label: 'Dashboard', path: '/dashboard' },
    { icon: FiCalendar, label: 'Appointments', path: '/appointments' },
    { icon: FiFileText, label: 'Prescription', path: '/prescriptions' },
    { icon: FiFileText, label: 'Patient Records', path: '/medical-records' },
    { icon: FiCreditCard, label: 'Payments', path: '/payments' },
    { icon: FiSettings, label: 'Settings', path: '/settings' },
  ];
  const clinicItems = [
    { icon: FiHome, label: 'Dashboard', path: '/dashboard' },
    { icon: FiCalendar, label: 'Appointments', path: '/appointments' },
    { icon: FiUsers, label: 'Patients', path: '/patients' },
    { icon: FiFileText, label: 'Prescription', path: '/prescriptions' },
    { icon: FiDollarSign, label: 'Billing', path: '/billing' },
    { icon: FiSettings, label: 'Settings', path: '/settings' },
  ];
  const superAdminItems = [
    { icon: FiHome, label: 'Platform Overview', path: '/superadmin' },
    { icon: FiSettings, label: 'Settings', path: '/settings' },
  ];
  const items = userRole === 'PATIENT' ? patientItems : userRole === 'SUPER_ADMIN' ? superAdminItems : clinicItems;

  return (
    <div
      className={`hbx-sidebar ${isExpanded ? 'expanded' : ''}`}
      onMouseEnter={() => setIsExpanded(true)}
      onMouseLeave={() => setIsExpanded(false)}
    >
      <ul>
        {items.map((item) => {
          const Icon = item.icon;
          return (
            <li
              key={item.path}
              className={isActive(item.path) ? 'active' : ''}
              onClick={() => navigate(item.path)}
            >
              <Icon className="hbx-icon" />
              <span className="hbx-label">{item.label}</span>
            </li>
          );
        })}
        <li onClick={handleLogout}>
          <FiLogOut className="hbx-icon" />
          <span className="hbx-label">Logout</span>
        </li>
      </ul>
    </div>
  );
};

export default Sidebar;
