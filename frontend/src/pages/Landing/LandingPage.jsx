import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';

import Logo from '../../components/Logo/Logo';

import './LandingPage.css';

const LandingPage = () => {
  const navigate = useNavigate();

  const [isHovered, setIsHovered] = useState(false);

  const handleLogoComplete = () => {
    navigate('/login');
  };

  return (
    <main className="landing-page">
      <section className="landing-content">

        <Logo
          onComplete={handleLogoComplete}
          interactive
          onHoverChange={setIsHovered}
        />

        <div
          className={`
            healthboxd-text-container
            ${isHovered ? 'healthboxd-text-visible' : ''}
          `}
        >
          <h1 className="landing-title">
            HEALTHBOXD
          </h1>

          <p className="landing-subtitle">
            Click to enter
          </p>
        </div>

      </section>
    </main>
  );
};

export default LandingPage;