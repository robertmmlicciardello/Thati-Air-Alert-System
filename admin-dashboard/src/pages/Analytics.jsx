import React, { useState } from 'react';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Grid,
  Alert,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip
} from '@mui/material';
import {
  TrendingUp as TrendingUpIcon,
  Warning as WarningIcon,
  People as PeopleIcon,
  Devices as DevicesIcon
} from '@mui/icons-material';

// Mock analytics data
const mockAnalytics = {
  alertsSent: 45,
  devicesReached: 156,
  responseRate: 87,
  networkUptime: 99.2,
  recentAlerts: [
    { date: '2024-01-20', count: 8, type: 'emergency' },
    { date: '2024-01-19', count: 12, type: 'warning' },
    { date: '2024-01-18', count: 6, type: 'info' },
    { date: '2024-01-17', count: 15, type: 'warning' },
    { date: '2024-01-16', count: 4, type: 'emergency' }
  ],
  deviceStats: [
    { location: 'Yangon', devices: 45, online: 42, alerts: 23 },
    { location: 'Mandalay', devices: 32, online: 30, alerts: 18 },
    { location: 'Naypyidaw', devices: 28, online: 26, alerts: 12 },
    { location: 'Bagan', devices: 15, online: 12, alerts: 8 },
    { location: 'Taunggyi', devices: 22, online: 20, alerts: 14 }
  ]
};

export default function Analytics() {
  const [analytics] = useState(mockAnalytics);

  const getTypeColor = (type) => {
    switch (type) {
      case 'emergency': return 'error';
      case 'warning': return 'warning';
      case 'info': return 'info';
      default: return 'default';
    }
  };

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Analytics Dashboard
      </Typography>

      <Alert severity="info" sx={{ mb: 3 }}>
        Analytics data is cached locally in offline mode. Real-time sync when connected.
      </Alert>

      {/* Key Metrics */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Box display="flex" alignItems="center">
                <WarningIcon color="warning" sx={{ mr: 2 }} />
                <Box>
                  <Typography color="textSecondary" gutterBottom>
                    Alerts Sent (30d)
                  </Typography>
                  <Typography variant="h4">
                    {analytics.alertsSent}
                  </Typography>
                  <Typography variant="body2" color="success.main">
                    +12% from last month
                  </Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Box display="flex" alignItems="center">
                <DevicesIcon color="primary" sx={{ mr: 2 }} />
                <Box>
                  <Typography color="textSecondary" gutterBottom>
                    Devices Reached
                  </Typography>
                  <Typography variant="h4">
                    {analytics.devicesReached}
                  </Typography>
                  <Typography variant="body2" color="success.main">
                    98.7% coverage
                  </Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Box display="flex" alignItems="center">
                <TrendingUpIcon color="success" sx={{ mr: 2 }} />
                <Box>
                  <Typography color="textSecondary" gutterBottom>
                    Response Rate
                  </Typography>
                  <Typography variant="h4">
                    {analytics.responseRate}%
                  </Typography>
                  <Typography variant="body2" color="success.main">
                    Above target (85%)
                  </Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Box display="flex" alignItems="center">
                <PeopleIcon color="info" sx={{ mr: 2 }} />
                <Box>
                  <Typography color="textSecondary" gutterBottom>
                    Network Uptime
                  </Typography>
                  <Typography variant="h4">
                    {analytics.networkUptime}%
                  </Typography>
                  <Typography variant="body2" color="success.main">
                    Excellent
                  </Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Grid container spacing={3}>
        {/* Recent Alert Activity */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Recent Alert Activity
              </Typography>
              <TableContainer>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Date</TableCell>
                      <TableCell>Count</TableCell>
                      <TableCell>Type</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {analytics.recentAlerts.map((alert, index) => (
                      <TableRow key={index}>
                        <TableCell>{alert.date}</TableCell>
                        <TableCell>{alert.count}</TableCell>
                        <TableCell>
                          <Chip
                            label={alert.type}
                            color={getTypeColor(alert.type)}
                            size="small"
                          />
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </CardContent>
          </Card>
        </Grid>

        {/* Device Statistics by Location */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Device Statistics by Location
              </Typography>
              <TableContainer>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Location</TableCell>
                      <TableCell>Total</TableCell>
                      <TableCell>Online</TableCell>
                      <TableCell>Alerts</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {analytics.deviceStats.map((stat, index) => (
                      <TableRow key={index}>
                        <TableCell>{stat.location}</TableCell>
                        <TableCell>{stat.devices}</TableCell>
                        <TableCell>
                          <Chip
                            label={`${stat.online}/${stat.devices}`}
                            color={stat.online === stat.devices ? 'success' : 'warning'}
                            size="small"
                          />
                        </TableCell>
                        <TableCell>{stat.alerts}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </CardContent>
          </Card>
        </Grid>

        {/* Performance Summary */}
        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                System Performance Summary
              </Typography>
              <Grid container spacing={2}>
                <Grid item xs={12} sm={4}>
                  <Box textAlign="center" p={2}>
                    <Typography variant="h3" color="success.main">
                      99.2%
                    </Typography>
                    <Typography variant="body2" color="textSecondary">
                      Network Uptime
                    </Typography>
                  </Box>
                </Grid>
                <Grid item xs={12} sm={4}>
                  <Box textAlign="center" p={2}>
                    <Typography variant="h3" color="primary.main">
                      2.3s
                    </Typography>
                    <Typography variant="body2" color="textSecondary">
                      Avg Alert Delivery
                    </Typography>
                  </Box>
                </Grid>
                <Grid item xs={12} sm={4}>
                  <Box textAlign="center" p={2}>
                    <Typography variant="h3" color="warning.main">
                      142
                    </Typography>
                    <Typography variant="body2" color="textSecondary">
                      Active Devices
                    </Typography>
                  </Box>
                </Grid>
              </Grid>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
}