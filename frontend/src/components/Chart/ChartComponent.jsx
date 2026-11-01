import React from 'react';
import {
  LineChart,
  Line,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';

const ChartComponent = ({ data = [], type = 'line' }) => {
  if (!data || data.length === 0) {
    return (
      <div className="flex items-center justify-center h-80 text-gray-500">
        <p>No data available</p>
      </div>
    );
  }

  const chartConfig = {
    margin: { top: 5, right: 30, left: 0, bottom: 5 },
  };

  if (type === 'bar') {
    return (
      <ResponsiveContainer width="100%" height={300}>
        <BarChart data={data} {...chartConfig}>
          <CartesianGrid strokeDasharray="3 3" stroke="rgba(200, 200, 200, 0.2)" />
          <XAxis
            dataKey="date"
            stroke="#999"
            style={{ fontSize: '12px' }}
          />
          <YAxis stroke="#999" style={{ fontSize: '12px' }} />
          <Tooltip
            contentStyle={{
              backgroundColor: 'rgba(255, 255, 255, 0.95)',
              border: '1px solid rgba(200, 200, 200, 0.3)',
              borderRadius: '8px',
            }}
          />
          <Legend />
          <Bar
            dataKey="appointments"
            fill="#4a2150"
            radius={[8, 8, 0, 0]}
            name="Appointments"
          />
        </BarChart>
      </ResponsiveContainer>
    );
  }

  return (
    <ResponsiveContainer width="100%" height={300}>
      <LineChart data={data} {...chartConfig}>
        <CartesianGrid strokeDasharray="3 3" stroke="rgba(200, 200, 200, 0.2)" />
        <XAxis
          dataKey="date"
          stroke="#999"
          style={{ fontSize: '12px' }}
        />
        <YAxis stroke="#999" style={{ fontSize: '12px' }} />
        <Tooltip
          contentStyle={{
            backgroundColor: 'rgba(255, 255, 255, 0.95)',
            border: '1px solid rgba(200, 200, 200, 0.3)',
            borderRadius: '8px',
          }}
          cursor={{ stroke: 'rgba(74, 33, 80, 0.2)' }}
        />
        <Legend />
        <Line
          type="monotone"
          dataKey="appointments"
          stroke="#4a2150"
          strokeWidth={3}
          dot={{ fill: '#4a2150', r: 4 }}
          activeDot={{ r: 6 }}
          name="Appointments"
          isAnimationActive
        />
      </LineChart>
    </ResponsiveContainer>
  );
};

export default ChartComponent;
