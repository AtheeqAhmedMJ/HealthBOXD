import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { authAPI } from '../../services/api';
import {
FiUser,
FiMail,
FiLock,
FiPhone,
FiAlertCircle,
FiCheckCircle
} from 'react-icons/fi';
import './RegisterPage.css';

const RegisterPage = () => {
const navigate = useNavigate();

const [loading, setLoading] = useState(false);
const [error, setError] = useState('');
const [success, setSuccess] = useState('');
const [step, setStep] = useState(1);
const [userType, setUserType] = useState('');
const [otpSent, setOtpSent] = useState(false);

const [formData, setFormData] = useState({
hospitalName: '',
username: '',
hospitalCode: '',
location: '',
email: '',
phone: '',
password: '',
confirmPassword: '',
otp: '',
});

const handleChange = (e) => {
const { name, value } = e.target;


setFormData((prev) => ({
  ...prev,
  [name]: value,
}));


};

const handleRequestOTP = async () => {
setError('');
setLoading(true);


try {
  await authAPI.requestOTP(
    formData.email,
    userType === 'hospital' ? 'REGISTER_ADMIN' : 'REGISTER_PATIENT',
    formData.phone
  );

  setOtpSent(true);
  setSuccess('OTP sent to your phone number');

} catch (err) {
  setError(
    err.response?.data?.message ||
    'Failed to request OTP'
  );

} finally {
  setLoading(false);
}


};

const handleRegister = async (e) => {
e.preventDefault();


setError('');

if (formData.password !== formData.confirmPassword) {
  setError('Passwords do not match');
  return;
}

setLoading(true);

try {
  if (userType === 'hospital') {
    await authAPI.registerHospital({
      hospitalName: formData.hospitalName,
      hospitalCode: formData.hospitalCode,
      location: formData.location,
      username: formData.username,
      email: formData.email,
      adminPhno: formData.phone,
      password: formData.password,
      otp: formData.otp,
    });

  } else {
    await authAPI.registerPatient({
      phoneNumber: formData.phone,
      phno: formData.phone,
      username: formData.username || formData.phone,
      hospitalCode: formData.hospitalCode,
      email: formData.email,
      password: formData.password,
      otp: formData.otp,
    });
  }

  setSuccess(
    'Registration successful! Redirecting to login...'
  );

  setTimeout(() => {
    navigate('/login');
  }, 2000);

} catch (err) {
  setError(
    err.response?.data?.message ||
    'Registration failed'
  );

} finally {
  setLoading(false);
}


};

return (
<>

  <div className="register-page">
    <div className="register-container">

      <h1 className="register-title">
        HEALTH BOX
      </h1>

      <div className="register-form">

        {/* STEP 1 - SELECT USER TYPE */}
        {step === 1 && (
          <>
            <h2 className="register-heading">
              GET STARTED
            </h2>

            <p className="register-subtitle">
              What do you want to register as?
            </p>

            <div className="register-type-options">

              {/* HOSPITAL */}
              <button
                type="button"
                onClick={() => {
                  setUserType('hospital');
                  setStep(2);
                }}
                className="register-type-btn"
              >
                <FiUser size={28} />

                <div>
                  <h3>Hospital / Clinic</h3>

                  <p>
                    Register your healthcare facility
                  </p>
                </div>
              </button>


              {/* PATIENT */}
              <button
                type="button"
                onClick={() => {
                  setUserType('patient');
                  setStep(2);
                }}
                className="register-type-btn"
              >
                <FiUser size={28} />

                <div>
                  <h3>Patient</h3>

                  <p>
                    Create your patient account
                  </p>
                </div>
              </button>

            </div>

            <p className="login-link">
              Already have an account?{' '}

              <Link to="/login">
                SIGN IN
              </Link>
            </p>
          </>
        )}


        {/* STEP 2 - REGISTRATION DETAILS */}
        {step === 2 && (
          <>
            <h2 className="register-heading">
              {userType === 'hospital'
                ? 'HOSPITAL DETAILS'
                : 'PATIENT REGISTRATION'}
            </h2>


            {/* ERROR */}
            {error && (
              <div className="register-error">
                <FiAlertCircle size={20} />

                <span>{error}</span>
              </div>
            )}


            {/* SUCCESS */}
            {success && (
              <div className="register-success">
                <FiCheckCircle size={20} />

                <span>{success}</span>
              </div>
            )}


            <form onSubmit={handleRegister}>

              {/* HOSPITAL NAME */}
              {userType === 'hospital' && (
                <div className="register-input-group">

                  <label>
                    HOSPITAL NAME
                  </label>

                  <div className="register-input-wrapper">

                    <FiUser
                      className="register-icon"
                      size={20}
                    />

                    <input
                      type="text"
                      name="hospitalName"
                      value={formData.hospitalName}
                      onChange={handleChange}
                      required
                      placeholder="Your Hospital Name"
                    />

                  </div>

                </div>
              )}

              {userType === 'hospital' && (
                <div className="register-input-group">
                  <label>CLINIC CODE</label>
                  <div className="register-input-wrapper">
                    <FiUser className="register-icon" size={20} />
                    <input type="text" name="hospitalCode" value={formData.hospitalCode} onChange={handleChange} required placeholder="Unique clinic code" />
                  </div>
                </div>
              )}

              {userType === 'hospital' && (
                <div className="register-input-group">
                  <label>CLINIC LOCATION</label>
                  <div className="register-input-wrapper">
                    <FiUser className="register-icon" size={20} />
                    <input type="text" name="location" value={formData.location} onChange={handleChange} required placeholder="City or full clinic address" />
                  </div>
                </div>
              )}


              {/* EMAIL */}
              {userType === 'hospital' && (
                <div className="register-input-group">

                  <label>
                    EMAIL ADDRESS
                  </label>

                  <div className="register-input-wrapper">

                    <FiMail
                      className="register-icon"
                      size={20}
                    />

                    <input
                      type="email"
                      name="email"
                      value={formData.email}
                      onChange={handleChange}
                      required
                      placeholder="your@hospital.com"
                    />

                  </div>

                </div>
              )}

              <div className="register-input-group">
                <label>{userType === 'hospital' ? 'USERNAME' : 'CLINIC CODE'}</label>
                <div className="register-input-wrapper">
                  <FiUser className="register-icon" size={20} />
                  <input type="text" name={userType === 'hospital' ? 'username' : 'hospitalCode'} value={formData[userType === 'hospital' ? 'username' : 'hospitalCode']} onChange={handleChange} required placeholder={userType === 'hospital' ? 'Choose a username' : 'Clinic code'} />
                </div>
              </div>

              {userType === 'patient' && <div className="register-input-group"><label>EMAIL (OPTIONAL)</label><div className="register-input-wrapper"><FiMail className="register-icon" size={20} /><input type="email" name="email" value={formData.email} onChange={handleChange} placeholder="you@example.com" /></div></div>}


              {/* PHONE */}
              <div className="register-input-group">

                <label>
                  PHONE NUMBER
                </label>

                <div className="phone-row">

                  <div className="register-input-wrapper">

                    <FiPhone
                      className="register-icon"
                      size={20}
                    />

                    <input
                      type="tel"
                      name="phone"
                      value={formData.phone}
                      onChange={handleChange}
                      required
                      disabled={otpSent}
                      placeholder="9876543210"
                    />

                  </div>


                  <button
                    type="button"
                    onClick={handleRequestOTP}
                    disabled={loading || otpSent}
                    className="otp-button"
                  >
                    {otpSent
                      ? 'SENT'
                      : 'SEND OTP'}
                  </button>

                </div>

              </div>


              {/* OTP */}
              {otpSent && (
                <div className="register-input-group">

                  <label>
                    OTP VERIFICATION
                  </label>

                  <input
                    type="text"
                    name="otp"
                    value={formData.otp}
                    onChange={handleChange}
                    required
                    maxLength="6"
                    placeholder="000000"
                  />

                </div>
              )}


              {/* PASSWORD SECTION */}
              {otpSent && (
                <>
                  <div className="register-input-group">

                    <label>
                      PASSWORD
                    </label>

                    <div className="register-input-wrapper">

                      <FiLock
                        className="register-icon"
                        size={20}
                      />

                      <input
                        type="password"
                        name="password"
                        value={formData.password}
                        onChange={handleChange}
                        required
                        placeholder="••••••••"
                      />

                    </div>

                  </div>


                  {/* CONFIRM PASSWORD */}
                  <div className="register-input-group">

                    <label>
                      CONFIRM PASSWORD
                    </label>

                    <div className="register-input-wrapper">

                      <FiLock
                        className="register-icon"
                        size={20}
                      />

                      <input
                        type="password"
                        name="confirmPassword"
                        value={formData.confirmPassword}
                        onChange={handleChange}
                        required
                        placeholder="••••••••"
                      />

                    </div>

                  </div>


                  {/* CREATE ACCOUNT */}
                  <button
                    type="submit"
                    disabled={loading}
                    className="register-submit-button"
                  >
                    {loading
                      ? 'CREATING ACCOUNT...'
                      : 'CREATE ACCOUNT'}
                  </button>

                </>
              )}

            </form>


            {/* BACK BUTTON */}
            <button
              type="button"
              onClick={() => {
                setStep(1);
                setOtpSent(false);
                setError('');
                setSuccess('');
              }}
              className="back-button"
            >
              ← BACK TO SELECTION
            </button>

          </>
        )}

      </div>
    </div>
  </div>
</>

);
};

export default RegisterPage;
