import React from 'react';

const StatCard = ({ title, value, icon: Icon, color, trend }) => {
  return (
    <div className="glass-card motion-surface rounded-xl p-6">
      <div className="flex items-start justify-between mb-4">
        <div>
          <p className="text-sm font-medium text-gray-600 uppercase tracking-wide">
            {title}
          </p>
          <h3 className="text-3xl font-bold text-gray-900 mt-2 font-mono">
            {value}
          </h3>
        </div>
        <div className={`${color} p-4 rounded-lg shadow-lg`}>
          <Icon size={28} className="text-white" />
        </div>
      </div>
      {trend && (
        <div className="flex items-center gap-2">
          <div className={`text-xs font-semibold ${trend.includes('-') ? 'text-red-600' : 'text-green-600'}`}>
            {trend}
          </div>
        </div>
      )}
    </div>
  );
};

export default StatCard;
