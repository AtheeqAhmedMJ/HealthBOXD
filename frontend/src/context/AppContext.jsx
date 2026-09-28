// src/context/AppContext.jsx
import React, { createContext, useContext, useCallback, useMemo, useState } from 'react';
import { NOTIFICATION_TYPES } from '../constants';

export const AppContext = createContext();

/**
 * App Context Provider with notifications and global state
 */
export const AppProvider = ({ children }) => {
  const [notifications, setNotifications] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [user, setUser] = useState(() => {
    try { return JSON.parse(localStorage.getItem('user') || 'null'); } catch { return null; }
  });
  const [selectedPatient, setSelectedPatientState] = useState(() => {
    try { return JSON.parse(localStorage.getItem('selectedPatient') || 'null'); } catch { return null; }
  });
  const [workflow, setWorkflow] = useState(() => {
    try { return JSON.parse(localStorage.getItem('patientWorkflow') || '{}'); } catch { return {}; }
  });

  const setSelectedPatient = useCallback((patient) => {
    setSelectedPatientState(patient);
    if (patient) localStorage.setItem('selectedPatient', JSON.stringify(patient));
    else localStorage.removeItem('selectedPatient');
  }, []);

  const updateWorkflow = useCallback((updates) => {
    setWorkflow((current) => {
      const next = { ...current, ...updates };
      localStorage.setItem('patientWorkflow', JSON.stringify(next));
      return next;
    });
  }, []);

  /**
   * Show notification
   */
  const showNotification = useCallback((message, type = NOTIFICATION_TYPES.INFO, duration = 3000) => {
    const id = Date.now();
    const notification = { id, message, type };

    setNotifications(prev => [...prev, notification]);

    if (duration > 0) {
      setTimeout(() => {
        removeNotification(id);
      }, duration);
    }

    return id;
  }, []);

  /**
   * Remove specific notification
   */
  const removeNotification = useCallback((id) => {
    setNotifications(prev => prev.filter(n => n.id !== id));
  }, []);

  /**
   * Clear all notifications
   */
  const clearNotifications = useCallback(() => {
    setNotifications([]);
  }, []);

  /**
   * Convenience methods
   */
  const notify = useMemo(() => ({
    success: (message, duration) => showNotification(message, NOTIFICATION_TYPES.SUCCESS, duration),
    error: (message, duration) => showNotification(message, NOTIFICATION_TYPES.ERROR, duration),
    warning: (message, duration) => showNotification(message, NOTIFICATION_TYPES.WARNING, duration),
    info: (message, duration) => showNotification(message, NOTIFICATION_TYPES.INFO, duration),
  }), [showNotification]);

  const value = useMemo(() => ({
    notifications,
    showNotification,
    removeNotification,
    clearNotifications,
    notify,
    isLoading,
    setIsLoading,
    sidebarOpen,
    setSidebarOpen,
    user,
    setUser,
    selectedPatient,
    setSelectedPatient,
    workflow,
    updateWorkflow,
  }), [notifications, showNotification, removeNotification, clearNotifications, notify, isLoading, sidebarOpen, user, selectedPatient, setSelectedPatient, workflow, updateWorkflow]);

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>;
};

/**
 * Hook to use App Context
 */
export const useApp = () => {
  const context = useContext(AppContext);
  if (!context) {
    throw new Error('useApp must be used within AppProvider');
  }
  return context;
};
