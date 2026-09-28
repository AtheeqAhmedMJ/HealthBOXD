import React, { useState } from 'react';
import './Logo.css';

const Logo = ({
  onComplete,
  interactive = true,
  onHoverChange,
}) => {
  const [isFalling, setIsFalling] = useState(false);

  const handleClick = () => {
    if (!interactive || isFalling) return;

    setIsFalling(true);

    if (onHoverChange) {
      onHoverChange(false);
    }

    setTimeout(() => {
      onComplete?.();
    }, 1000);
  };

  const handleMouseEnter = () => {
    if (!interactive || isFalling) return;

    onHoverChange?.(true);
  };

  const handleMouseLeave = () => {
    if (!interactive || isFalling) return;

    onHoverChange?.(false);
  };

  const handleKeyDown = (event) => {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      handleClick();
    }
  };

  return (
    <div
      className={`logo-wrapper ${
        interactive ? 'logo-interactive' : ''
      } ${
        isFalling ? 'logo-falling' : ''
      }`}
      onClick={handleClick}
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
      onKeyDown={handleKeyDown}
      role={interactive ? 'button' : undefined}
      tabIndex={interactive ? 0 : undefined}
      aria-label={interactive ? 'Enter HealthBoxD' : undefined}
    >
      <div className="logo-halves">
        <img
          src="/icons/iconLeft.png"
          alt=""
          className="health-logo health-logo-left"
          draggable="false"
        />

        <img
          src="/icons/iconRight.png"
          alt=""
          className="health-logo health-logo-right"
          draggable="false"
        />
      </div>
    </div>
  );
};

export default Logo;