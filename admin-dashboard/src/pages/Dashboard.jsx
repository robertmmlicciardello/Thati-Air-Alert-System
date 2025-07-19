import React, { useState, useEffect } from 'react';
import {
  Grid,
  Card,
  CardContent,
  Typography,
  Box,
  Paper,
  IconButton,
  Chip,
  LinearProgress,
  Alert,
  Divider,
} from '@mui/material';
import {
  Refresh as RefreshIcon,
  Warning as WarningIcon,
  CheckCircle as CheckCircleIcon,
  Error as ErrorIcon,
  People as PeopleIcon,
  Devices as DevicesIcon,
  Notifications as NotificationsIcon,
  TrendingUp as TrendingUpIcon,
} from '@mui/icons-material';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, BarChart, Bar, PieChart, Pie, Cell } from 'recharts';
import { useQuery } from 'react-query';
import { format } from 'date-fns';
import toast from 'react-hot-toast';

import { dashboardApi } from '../services/api';
import { useWebSocket } from '../hooks/useWebSocket';
import StatCard from '../components/dashboard/StatCard';
import AlertsTable from '../components/dashboard/AlertsTable';
import NetworkStatus from '../components/dashboard/NetworkStatus';
import LoadingSpinner from '../components/common/LoadingSpinner';

const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#8884D8'];

function Dashboard() {
  const [refreshing, setRefreshing] = useState(false);
  const [realTimeData, setRealTimeData] = useState({});

  // Fetch dashboard data
  const { data: dashboardData, isLoading, error, refetch } = useQuery(
    'dashboard',
    dashboardApi.getDashboardStats,
    {
      refetchInterval: 30000, // Refresh every 30 seconds
    }
  );

  // WebSocket for real-time updates
  const { socket, connected } = useWebSocket();

  useEffect(() => {
    if (socket) {
      socket.on('dashboard_update', (data) => {
        setRealTimeData(data);
      });

      socket.on('new_alert', (alert) => {
        toast.success(`New ${alert.type} alert received from ${alert.region || 'Unknown region'}`);
      });

      socket.on('system_status', (status) => {
        if (status.level === 'error') {
          toast.error(status.message);
        } else if (status.level === 'warning') {
          toast.warning(status.message);
        }
      });

      return () => {
        socket.off('dashboard_update');
        socket.off('new_alert');
        socket.off('system_status');
      };
    }
  }, [socket]);

  const handleRefresh = async () => {
    setRefreshing(true);
    try {
      await refetch();
      toast.success('Dashboard refreshed successfully');
    } catch (err) {
      toast.error('Failed to refresh dashboard');
    } finally {
      setRefreshing(false);
    }
  };

  if (isLoading) return <LoadingSpinner />;
  if (error) return <Alert severity="error">Failed to load dashboard data</Alert>;

  const stats = { ...dashboardData, ...realTimeData };

  return (
    <Box>
      {/* Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4" component="h1" fontWeight="bold">
          Dashboard
        </Typography>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Chip
            icon={connected ? <CheckCircleIcon /> : <ErrorIcon />}
            label={connected ? 'Connected' : 'Disconnected'}
            color={connected ? 'success' : 'error'}
            variant="outlined"
          />
          <IconButton onClick={handleRefresh} disabled={refreshing}>
            <RefreshIcon sx={{ animation: refreshing ? 'spin 1s linear infinite' : 'none' }} />
          </IconButton>
        </Box>
      </Box>

      {/* Stats Cards */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Total Alerts"
            value={stats.totalAlerts || 0}
            change={stats.alertsChange || 0}
            icon={<NotificationsIcon />}
            color="primary"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Active Users"
            value={stats.activeUsers || 0}
            change={stats.usersChange || 0}
            icon={<PeopleIcon />}
            color="success"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Connected Devices"
            value={stats.connectedDevices || 0}
            change={stats.devicesChange || 0}
            icon={<DevicesIcon />}
            color="info"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="System Health"
            value={`${stats.systemHealth || 100}%`}
            change={stats.healthChange || 0}
            icon={<TrendingUpIcon />}
            color="warning"
          />
        </Grid>
      </Grid>

      {/* Charts Row */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        {/* Alert Trends */}
        <Grid item xs={12} md={8}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Alert Trends (Last 7 Days)
              </Typography>
              <Box sx={{ height: 300 }}>
                <ResponsiveContainer width="100%" height="100%">
                  <LineChart data={stats.alertTrends || []}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis 
                      dataKey="date" 
                      tickFormatter={(value) => format(new Date(value), 'MMM dd')}
                    />
                    <YAxis />
                    <Tooltip 
                      labelFormatter={(value) => format(new Date(value), 'MMM dd, yyyy')}
                    />
                    <Line 
                      type="monotone" 
                      dataKey="alerts" 
                      stroke="#1976d2" 
                      strokeWidth={2}
                      dot={{ fill: '#1976d2' }}
                    />
                  </LineChart>
                </ResponsiveContainer>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Alert Types Distribution */}
        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Alert Types
              </Typography>
              <Box sx={{ height: 300 }}>
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={stats.alertTypes || []}
                      cx="50%"
                      cy="50%"
                      labelLine={false}
                      label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                      outerRadius={80}
                      fill="#8884d8"
                      dataKey="value"
                    >
                      {(stats.alertTypes || []).map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                      ))}
                    </Pie>
                    <Tooltip />
                  </PieChart>
                </ResponsiveContainer>
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Network Status and Recent Alerts */}
      <Grid container spacing={3}>
        {/* Network Status */}
        <Grid item xs={12} md={4}>
          <NetworkStatus data={stats.networkStatus} />
        </Grid>

        {/* Recent Alerts */}
        <Grid item xs={12} md={8}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Recent Alerts
              </Typography>
              <AlertsTable alerts={stats.recentAlerts || []} compact />
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* System Status */}
      {stats.systemAlerts && stats.systemAlerts.length > 0 && (
        <Box sx={{ mt: 3 }}>
          <Typography variant="h6" gutterBottom>
            System Alerts
          </Typography>
          {stats.systemAlerts.map((alert, index) => (
            <Alert 
              key={index} 
              severity={alert.severity} 
              sx={{ mb: 1 }}
              action={
                alert.action && (
                  <IconButton size="small" onClick={alert.action}>
                    <RefreshIcon />
                  </IconButton>
                )
              }
            >
              {alert.message}
            </Alert>
          ))}
        </Box>
      )}
    </Box>
  );
}

export default Dashboard;