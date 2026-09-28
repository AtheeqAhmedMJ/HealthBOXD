// src/constants/index.js

/**
 * User Roles
 */
export const USER_ROLES = {
  ADMIN: 'ADMIN',
  PATIENT: 'PATIENT',
  DOCTOR: 'DOCTOR',
  SUPER_ADMIN: 'SUPER_ADMIN',
};

/**
 * Appointment Status
 */
export const APPOINTMENT_STATUS = {
  BOOKED: 'BOOKED',
  SCHEDULED: 'BOOKED',
  CHECKED_IN: 'CHECKED_IN',
  IN_CONSULTATION: 'IN_CONSULTATION',
  COMPLETED: 'COMPLETED',
  CANCELLED: 'CANCELLED',
  NO_SHOW: 'NO_SHOW',
};

/**
 * Payment Status
 */
export const PAYMENT_STATUS = {
  CREATED: 'CREATED',
  PENDING: 'PENDING',
  SUCCESS: 'SUCCESS',
  PAID: 'SUCCESS',
  PROCESSING: 'PENDING',
  COMPLETED: 'SUCCESS',
  FAILED: 'FAILED',
  REFUNDED: 'REFUNDED',
  CANCELLED: 'CANCELLED',
};

/**
 * Prescription Status
 */
export const PRESCRIPTION_STATUS = {
  ACTIVE: 'ACTIVE',
  INACTIVE: 'INACTIVE',
  EXPIRED: 'EXPIRED',
  DISPENSED: 'DISPENSED',
};

/**
 * Billing Status
 */
export const BILLING_STATUS = {
  UNPAID: 'UNPAID',
  PARTIALLY_PAID: 'PARTIALLY_PAID',
  PAID: 'PAID',
  OVERDUE: 'OVERDUE',
};

/**
 * HTTP Status Codes
 */
export const HTTP_STATUS = {
  OK: 200,
  CREATED: 201,
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  CONFLICT: 409,
  INTERNAL_ERROR: 500,
  SERVICE_UNAVAILABLE: 503,
};

/**
 * Error Messages
 */
export const ERROR_MESSAGES = {
  NETWORK_ERROR: 'Network error. Please check your connection.',
  SERVER_ERROR: 'Server error. Please try again later.',
  UNAUTHORIZED: 'You are not authorized to perform this action.',
  SESSION_EXPIRED: 'Your session has expired. Please login again.',
  INVALID_CREDENTIALS: 'Invalid email or password.',
  EMAIL_EXISTS: 'Email already registered.',
  PHONE_EXISTS: 'Phone number already registered.',
  INVALID_OTP: 'Invalid or expired OTP.',
  FORM_VALIDATION: 'Please fill all required fields correctly.',
};

/**
 * Success Messages
 */
export const SUCCESS_MESSAGES = {
  LOGIN_SUCCESS: 'Logged in successfully!',
  LOGOUT_SUCCESS: 'Logged out successfully!',
  REGISTRATION_SUCCESS: 'Registration successful! Redirecting...',
  APPOINTMENT_CREATED: 'Appointment scheduled successfully!',
  APPOINTMENT_CANCELLED: 'Appointment cancelled successfully!',
  PAYMENT_SUCCESSFUL: 'Payment processed successfully!',
  PROFILE_UPDATED: 'Profile updated successfully!',
  PRESCRIPTION_ADDED: 'Prescription added successfully!',
};

/**
 * API Endpoints Configuration
 */
export const API_ENDPOINTS = {
  AUTH: {
    LOGIN: '/auth/login',
    LOGOUT: '/auth/logout',
    REGISTER_HOSPITAL: '/auth/register-hospital',
    REGISTER_PATIENT: '/auth/register-patient',
    REQUEST_OTP: '/auth/otp/request',
  },
  APPOINTMENTS: {
    LIST: '/appointments',
    CREATE: '/appointments',
    GET_DETAILS: '/appointments/with-details',
    GET_BY_PATIENT: '/appointments/patient',
    DELETE: '/appointments',
  },
  PATIENTS: {
    LIST: '/patients',
    GET: '/patients',
  },
  PRESCRIPTIONS: {
    LIST: '/prescriptions',
    GET_BY_PATIENT: '/prescriptions/patient',
    GET_BY_APPOINTMENT: '/prescriptions/appointment',
  },
  BILLING: {
    LIST: '/billing',
    GET_BY_PATIENT: '/billing/patient',
  },
  PAYMENTS: {
    CHECKOUT: '/payments/checkout',
    VERIFY: '/payments/verify',
    LIST: '/payments',
    GET: '/payments',
  },
  SCHEDULING: {
    LIST: '/schedule',
    CREATE: '/schedule',
    GET_BY_DOCTOR: '/schedule/doctor',
    DELETE: '/schedule',
  },
  CHARGES: {
    LIST: '/charges',
    CREATE: '/charges',
    GET_ACTIVE: '/charges/active',
    DELETE: '/charges',
  },
  DASHBOARD: {
    ADMIN_SUMMARY: '/dashboard/summary',
    PATIENT_SUMMARY: '/dashboard/patient-summary',
  },
  SUPERADMIN: {
    STATS: '/superadmin/stats',
  },
};

/**
 * Regex Patterns
 */
export const REGEX_PATTERNS = {
  EMAIL: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
  PHONE: /^[6-9]\d{9}$/,
  PASSWORD: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/,
  OTP: /^\d{6}$/,
  URL: /^(https?:\/\/)?(www\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\.[a-zA-Z0-9()]{1,6}\b([-a-zA-Z0-9()@:%_+.~#?&//=]*)$/,
};

/**
 * Color Scheme
 */
export const COLORS = {
  PRIMARY: '#a887b2',
  SECONDARY: '#7a5af5',
  ACCENT: '#d794d0',
  LIGHT_PURPLE: '#dec9f9',
  LIGHT_PINK: '#e9dbf7',
  SUCCESS: '#10b981',
  WARNING: '#f59e0b',
  ERROR: '#ef4444',
  INFO: '#3b82f6',
  TEXT: '#1f1f2e',
  TEXT_LIGHT: '#6b7280',
  BORDER: '#e5e7eb',
};

/**
 * Chart Colors
 */
export const CHART_COLORS = [
  '#a887b2',
  '#7a5af5',
  '#d794d0',
  '#f97316',
  '#06b6d4',
  '#ec4899',
  '#14b8a6',
];

/**
 * Breakpoints for responsive design
 */
export const BREAKPOINTS = {
  SM: 640,
  MD: 768,
  LG: 1024,
  XL: 1280,
  XXL: 1536,
};

/**
 * Local Storage Keys
 */
export const LOCAL_STORAGE_KEYS = {
  AUTH_TOKEN: 'authToken',
  USER: 'user',
  THEME: 'theme',
  PREFERENCES: 'userPreferences',
  CACHE: 'appCache',
};

/**
 * Date/Time Formats
 */
export const DATE_FORMATS = {
  SHORT: 'DD/MM/YYYY',
  LONG: 'DD MMM YYYY',
  FULL: 'dddd, DD MMMM YYYY',
  ISO: 'YYYY-MM-DD',
  TIME: 'HH:mm',
  DATETIME: 'DD/MM/YYYY HH:mm',
};

/**
 * Pagination
 */
export const PAGINATION = {
  DEFAULT_PAGE_SIZE: 10,
  OPTIONS: [10, 20, 50, 100],
};

/**
 * Notification Types
 */
export const NOTIFICATION_TYPES = {
  SUCCESS: 'success',
  ERROR: 'error',
  WARNING: 'warning',
  INFO: 'info',
};

/**
 * Animation Durations (ms)
 */
export const ANIMATION = {
  FAST: 150,
  NORMAL: 300,
  SLOW: 500,
};

/**
 * API Timeout (ms)
 */
export const API_TIMEOUT = 30000;

/**
 * Retry Configuration
 */
export const RETRY_CONFIG = {
  MAX_RETRIES: 3,
  RETRY_DELAY: 1000,
  BACKOFF_MULTIPLIER: 2,
};

/**
 * Module Access Permissions
 */
export const MODULE_PERMISSIONS = {
  DASHBOARD: ['ADMIN', 'PATIENT', 'DOCTOR'],
  APPOINTMENTS: ['ADMIN', 'PATIENT', 'DOCTOR'],
  PATIENTS: ['ADMIN', 'DOCTOR'],
  PRESCRIPTIONS: ['ADMIN', 'PATIENT', 'DOCTOR'],
  BILLING: ['ADMIN', 'PATIENT'],
  PAYMENTS: ['ADMIN', 'PATIENT'],
  PHARMACY: ['ADMIN', 'PATIENT'],
  SCHEDULING: ['ADMIN', 'DOCTOR'],
  CHARGES: ['ADMIN', 'SUPER_ADMIN'],
};
