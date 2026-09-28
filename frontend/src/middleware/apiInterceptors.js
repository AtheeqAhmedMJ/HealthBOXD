// src/middleware/apiInterceptors.js
import apiClient from '../services/api';
import { setAuthToken } from '../services/api';

let notificationHandler;
let interceptorsConfigured = false;
let customHeadersConfigured = false;

/**
 * Setup API interceptors for request/response handling
 */
export const setupApiInterceptors = (showNotification) => {
  notificationHandler = showNotification;
  if (interceptorsConfigured) return;
  interceptorsConfigured = true;

  // Request Interceptor
  apiClient.interceptors.request.use(
    (config) => {
      // Add request timestamp for performance tracking
      config.metadata = { startTime: new Date() };

      // Log requests in development
      if (process.env.NODE_ENV === 'development') {
        console.log('API Request:', {
          method: config.method.toUpperCase(),
          url: config.url,
          data: config.data,
        });
      }

      return config;
    },
    (error) => {
      console.error('Request error:', error);
      return Promise.reject(error);
    }
  );

  // Response Interceptor
  apiClient.interceptors.response.use(
    (response) => {
      // Calculate response time
      const duration = new Date() - response.config.metadata.startTime;

      // Log responses in development
      if (process.env.NODE_ENV === 'development') {
        console.log('API Response:', {
          status: response.status,
          url: response.config.url,
          duration: `${duration}ms`,
          data: response.data,
        });
      }

      return response.data;
    },
    (error) => {
      const status = error.response?.status;
      const message = error.response?.data?.message || error.message;

      // Handle specific status codes
      switch (status) {
        case 401:
          // Unauthorized - clear token and redirect to login
          setAuthToken(null);
          localStorage.removeItem('user');
          window.location.href = '/login';
          notificationHandler?.('Your session has expired. Please login again.', 'error');
          break;

        case 403:
          // Forbidden
          notificationHandler?.('You do not have permission to perform this action.', 'error');
          break;

        case 404:
          // Not found
          notificationHandler?.('The requested resource was not found.', 'error');
          break;

        case 409:
          // Conflict
          notificationHandler?.(message || 'A conflict occurred. Please try again.', 'warning');
          break;

        case 429:
          // Too many requests
          notificationHandler?.('Too many requests. Please wait a moment and try again.', 'warning');
          break;

        case 500:
        case 502:
        case 503:
        case 504:
          // Server errors
          notificationHandler?.('Server error. Please try again later.', 'error');
          break;

        default:
          if (!error.response) {
            // Network error
            notificationHandler?.('Network error. Please check your connection.', 'error');
          } else {
            notificationHandler?.(message || 'An error occurred. Please try again.', 'error');
          }
      }

      // Log errors in development
      if (process.env.NODE_ENV === 'development') {
        console.error('API Error:', {
          status,
          message,
          url: error.config?.url,
          data: error.response?.data,
        });
      }

      return Promise.reject(error);
    }
  );
};

/**
 * Add custom headers to API requests
 */
export const setupCustomHeaders = () => {
  if (customHeadersConfigured) return;
  customHeadersConfigured = true;

  apiClient.interceptors.request.use((config) => {
    // Add custom headers
    config.headers['X-App-Version'] = '2.0.0';
    config.headers['X-Request-ID'] = generateRequestId();

    // Add timezone info
    config.headers['X-Timezone'] = Intl.DateTimeFormat().resolvedOptions().timeZone;

    return config;
  });
};

/**
 * Generate unique request ID
 */
const generateRequestId = () => {
  return `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
};

/**
 * Retry failed requests
 */
export const setupRetryInterceptor = (maxRetries = 3) => {
  apiClient.interceptors.response.use(
    (response) => response,
    async (error) => {
      const config = error.config;

      if (!config) return Promise.reject(error);

      // Don't retry if max retries reached
      config.retryCount = config.retryCount || 0;
      if (config.retryCount >= maxRetries) {
        return Promise.reject(error);
      }

      // Only retry on specific status codes
      const retryableStatuses = [408, 429, 500, 502, 503, 504];
      if (!retryableStatuses.includes(error.response?.status)) {
        return Promise.reject(error);
      }

      // Don't retry non-GET requests (unless they're idempotent)
      if (config.method !== 'get' && !config.idempotent) {
        return Promise.reject(error);
      }

      config.retryCount += 1;

      // Exponential backoff
      const delay = Math.pow(2, config.retryCount - 1) * 1000;
      await new Promise(resolve => setTimeout(resolve, delay));

      return apiClient(config);
    }
  );
};

/**
 * Cache GET requests
 */
export const setupCacheInterceptor = () => {
  const cache = new Map();

  apiClient.interceptors.request.use((config) => {
    if (config.method === 'get' && !config.cache) {
      config.cache = true;
    }
    return config;
  });

  apiClient.interceptors.response.use(
    (response) => {
      if (response.config.cache && response.config.method === 'get') {
        cache.set(response.config.url, {
          data: response.data,
          timestamp: Date.now(),
        });
      }
      return response;
    },
    (error) => Promise.reject(error)
  );
};

/**
 * Request timeout handler
 */
export const setupTimeoutHandler = (timeout = 30000) => {
  apiClient.defaults.timeout = timeout;

  apiClient.interceptors.response.use(
    (response) => response,
    (error) => {
      if (error.code === 'ECONNABORTED') {
        return Promise.reject({
          ...error,
          message: 'Request timeout. Please try again.',
        });
      }
      return Promise.reject(error);
    }
  );
};
