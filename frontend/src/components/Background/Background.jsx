import React from 'react';

const Background = () => {
  return (
    <div className="hbx-background">
      <div className="hbx-dune" />
      <style>{`
        .hbx-background {
          position: fixed;
          inset: 0;
          z-index: -10;
          overflow: hidden;
          background: linear-gradient(135deg, #d794d0, #d9b3e2);
          contain: strict;
        }

        .hbx-dune {
          position: absolute;
          top: 0;
          right: 0;
          width: 60%;
          height: 100%;
          background: linear-gradient(to left,
            rgba(255, 255, 255, 0.8),
            rgba(255, 255, 255, 0.1) 60%,
            transparent
          );
          clip-path: ellipse(80% 100% at 100% 50%);
          animation: hbxWave 8s ease-in-out infinite alternate, hbxWaveBefore 12s ease-in-out infinite alternate-reverse 0s, hbxWaveAfter 10s ease-in-out infinite alternate -2s;
          will-change: clip-path;
          backface-visibility: hidden;
          filter: blur(2px);
        }

        @keyframes hbxWave {
          0% { clip-path: ellipse(80% 100% at 100% 50%); }
          50% { clip-path: ellipse(82% 101% at 100% 50%); }
          100% { clip-path: ellipse(80% 100% at 100% 50%); }
        }

        @keyframes hbxWaveBefore {
          0% { clip-path: ellipse(85% 100% at 100% 50%); }
          50% { clip-path: ellipse(87% 102% at 100% 48%); }
          100% { clip-path: ellipse(85% 100% at 100% 50%); }
        }

        @keyframes hbxWaveAfter {
          0% { clip-path: ellipse(75% 100% at 100% 50%); }
          50% { clip-path: ellipse(77% 103% at 100% 47%); }
          100% { clip-path: ellipse(75% 100% at 100% 50%); }
        }

        .hbx-dune::before {
          content: '';
          position: absolute;
          top: 0;
          right: 0;
          width: 100%;
          height: 100%;
          background: linear-gradient(to left,
            rgba(255, 255, 255, 0.4),
            transparent 70%
          );
          animation: hbxWaveBefore 12s ease-in-out infinite alternate-reverse;
          filter: blur(2px);
          will-change: clip-path;
          backface-visibility: hidden;
        }

        .hbx-dune::after {
          content: '';
          position: absolute;
          top: 0;
          right: 0;
          width: 100%;
          height: 100%;
          background: linear-gradient(to left,
            rgba(255, 255, 255, 0.6),
            rgba(255, 255, 255, 0.3) 50%,
            transparent 80%
          );
          animation: hbxWaveAfter 10s ease-in-out infinite alternate -2s;
          filter: blur(1px);
          will-change: clip-path;
          backface-visibility: hidden;
        }

        @media (max-width: 991px) {
          .hbx-dune { width: 70%; }
        }
        @media (max-width: 767px) {
          .hbx-dune { width: 80%; filter: blur(1.5px); }
        }
        @media (max-width: 479px) {
          .hbx-dune { width: 90%; }
        }
        @media (prefers-reduced-motion: reduce) {
          .hbx-dune {
            animation: none;
            clip-path: ellipse(80% 100% at 100% 50%);
          }
        }
      `}</style>
    </div>
  );
};

export default Background;