// src/components/ErrorBoundary/ErrorBoundary.jsx
import React from 'react';
import { FiAlertTriangle, FiRotateCcw } from 'react-icons/fi';

/**
 * Error Boundary Component
 * Catches React component errors and displays fallback UI
 */
export class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      hasError: false,
      error: null,
      errorInfo: null,
      errorCount: 0,
    };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true };
  }

  componentDidCatch(error, errorInfo) {
    // Log error details
    console.error('Error Boundary caught an error:', error);
    console.error('Error Info:', errorInfo);

    this.setState(prevState => ({
      error,
      errorInfo,
      errorCount: prevState.errorCount + 1,
    }));

    // Report to error tracking service (e.g., Sentry)
    this.reportError(error, errorInfo);
  }

  reportError = (error, errorInfo) => {
    // TODO: Implement error tracking service integration
    // Example: Sentry.captureException(error);
  };

  handleReset = () => {
    this.setState({
      hasError: false,
      error: null,
      errorInfo: null,
    });
  };

  render() {
    const { hasError, error, errorCount } = this.state;

    if (hasError) {
      return (
        <div className="min-h-screen bg-gradient-to-br from-purple-50 via-pink-50 to-white flex items-center justify-center p-4">
          <div className="max-w-md w-full bg-white/40 backdrop-blur-md rounded-xl p-8 shadow-lg border border-white/20">
            {/* Error Icon */}
            <div className="flex justify-center mb-6">
              <div className="w-16 h-16 bg-red-100 rounded-full flex items-center justify-center">
                <FiAlertTriangle className="text-red-600" size={32} />
              </div>
            </div>

            {/* Error Message */}
            <h1 className="text-2xl font-bold text-gray-900 text-center mb-2">
              Oops! Something went wrong
            </h1>
            <p className="text-gray-600 text-center mb-6">
              An unexpected error has occurred. Don't worry, our team has been notified.
            </p>

            {/* Error Details (Development only) */}
            {process.env.NODE_ENV === 'development' && error && (
              <div className="mb-6 p-4 bg-red-50 rounded-lg border border-red-200">
                <p className="text-xs text-red-700 font-mono mb-2">
                  <strong>Error:</strong> {error.toString()}
                </p>
                {this.state.errorInfo && (
                  <details className="text-xs text-red-600">
                    <summary className="cursor-pointer font-semibold mb-2">
                      Stack Trace
                    </summary>
                    <pre className="bg-white/50 p-2 rounded overflow-auto text-xs">
                      {this.state.errorInfo.componentStack}
                    </pre>
                  </details>
                )}
              </div>
            )}

            {/* Error Count Warning */}
            {errorCount > 2 && (
              <div className="mb-6 p-4 bg-yellow-50 rounded-lg border border-yellow-200">
                <p className="text-sm text-yellow-700">
                  Multiple errors detected. Please refresh the page if the issue persists.
                </p>
              </div>
            )}

            {/* Action Buttons */}
            <div className="space-y-3">
              <button
                onClick={this.handleReset}
                className="w-full flex items-center justify-center gap-2 px-4 py-3 bg-gradient-to-r from-purple-500 to-pink-500 text-white font-semibold rounded-lg hover:shadow-lg transition-all"
              >
                <FiRotateCcw size={18} />
                Try Again
              </button>
              <button
                onClick={() => window.location.href = '/'}
                className="w-full px-4 py-3 bg-gray-300/40 text-gray-700 font-semibold rounded-lg hover:bg-gray-300/60 transition-all"
              >
                Go to Dashboard
              </button>
            </div>

            {/* Contact Support */}
            <p className="text-xs text-gray-500 text-center mt-6">
              If the problem persists,{' '}
              <a href="mailto:support@healthboxd.com" className="text-purple-600 hover:underline">
                contact support
              </a>
            </p>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
