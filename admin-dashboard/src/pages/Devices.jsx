import React, { useState } from 'react';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  IconButton,
  Button,
  Alert,
  Paper,
  LinearProgress,
  Grid
} from '@mui/material';
import {
  Refresh as RefreshIcon,
  Settings as SettingsIcon,
  Delete as DeleteIcon,
  NetworkWifi as NetworkIcon,
  Battery3Bar as BatteryIcon,
  SignalWifi4Bar as SignalIcon
} from '@mui/icons-material';

// Mock devices data with mesh network info
const mockDevices = [
  {
    id: 1,
    name: 'Device-YGN-001',
    location: 'Yangon Central District',
    status: 'online',
    batteryLevel: 85,
    signalStrength: 90,
    meshConnections: 4,
    lastSeen: new Date(),
    firmware: '2.1.0',
    type: 'primary'
  },
  {
    id: 2,
    name: 'Device-MDY-002',
    location: 'Mandalay North',
    status: 'online',
    batteryLevel: 72,
    signalStrength: 75,
    meshConnections: 3,
    lastSeen: new Date(Date.now() - 120000),
    firmware: '2.1.0',
    type: 'relay'
  },
  {
    id: 3,
    name: 'Device-NPT-003',
    location: 'Naypyidaw Central',
    status: 'online',
    batteryLevel: 95,
    signalStrength: 85,
    meshConnections: 2,
    lastSeen: new Date(Date.now() - 60000),
    firmware: '2.0.8',
    type: 'endpoint'
  },
  {
    id: 4,
    name: 'Device-BGN-004',
    location: 'Bagan Archaeological Zone',
    status: 'offline',
    batteryLevel: 15,
    signalStrength: 0,
    meshConnections: 0,
    lastSeen: new Date(Date.now() - 1800000),
    firmware: '2.0.5',
    type: 'endpoint'
  },
  {
    id: 5,
    name: 'Device-TNG-005',
    location: 'Taunggyi Market',
    status: 'online',
    batteryLevel: 68,
    signalStrength: 60,
    meshConnections: 1,
    lastSeen: new Date(Date.now() - 300000),
    firmware: '2.1.0',
    type: 'relay'
  }
];

export default function Devices() {
  const [devices, setDevices] = useState(mockDevices);

  const getStatusColor = (status) => {
    return status === 'online' ? 'success' : 'error';
  };

  const getTypeColor = (type) => {
    switch (type) {
      case 'primary': return 'error';
      case 'relay': return 'warning';
      case 'endpoint': return 'info';
      default: return 'default';
    }
  };

  const getBatteryColor = (level) => {
    if (level > 50) return 'success';
    if (level > 20) return 'warning';
    return 'error';
  };

  const getSignalColor = (strength) => {
    if (strength > 70) return 'success';
    if (strength > 40) return 'warning';
    return 'error';
  };

  const onlineDevices = devices.filter(d => d.status === 'online').length;
  const totalMeshConnections = devices.reduce((sum, d) => sum + d.meshConnections, 0);

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">
          Device Management
        </Typography>
        <Button
          variant="outlined"
          startIcon={<RefreshIcon />}
        >
          Refresh Network
        </Button>
      </Box>

      <Alert severity="success" sx={{ mb: 3 }}>
        Mesh Network Active - {onlineDevices} devices online with {totalMeshConnections} total connections
      </Alert>

      {/* Stats Cards */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Box display="flex" alignItems="center">
                <NetworkIcon color="primary" sx={{ mr: 2 }} />
                <Box>
                  <Typography color="textSecondary" gutterBottom>
                    Online Devices
                  </Typography>
                  <Typography variant="h4">
                    {onlineDevices}/{devices.length}
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
                <SignalIcon color="success" sx={{ mr: 2 }} />
                <Box>
                  <Typography color="textSecondary" gutterBottom>
                    Mesh Connections
                  </Typography>
                  <Typography variant="h4">
                    {totalMeshConnections}
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
                <BatteryIcon color="warning" sx={{ mr: 2 }} />
                <Box>
                  <Typography color="textSecondary" gutterBottom>
                    Avg Battery
                  </Typography>
                  <Typography variant="h4">
                    {Math.round(devices.reduce((sum, d) => sum + d.batteryLevel, 0) / devices.length)}%
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
                <SettingsIcon color="info" sx={{ mr: 2 }} />
                <Box>
                  <Typography color="textSecondary" gutterBottom>
                    Primary Nodes
                  </Typography>
                  <Typography variant="h4">
                    {devices.filter(d => d.type === 'primary').length}
                  </Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Device Network Status
          </Typography>
          <TableContainer component={Paper}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Device</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Type</TableCell>
                  <TableCell>Battery</TableCell>
                  <TableCell>Signal</TableCell>
                  <TableCell>Mesh Links</TableCell>
                  <TableCell>Last Seen</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {devices.map((device) => (
                  <TableRow key={device.id}>
                    <TableCell>
                      <Box>
                        <Typography variant="body1" fontWeight="medium">
                          {device.name}
                        </Typography>
                        <Typography variant="body2" color="textSecondary">
                          {device.location}
                        </Typography>
                        <Typography variant="caption" color="textSecondary">
                          Firmware: {device.firmware}
                        </Typography>
                      </Box>
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={device.status}
                        color={getStatusColor(device.status)}
                        size="small"
                      />
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={device.type}
                        color={getTypeColor(device.type)}
                        size="small"
                      />
                    </TableCell>
                    <TableCell>
                      <Box sx={{ minWidth: 100 }}>
                        <Box display="flex" alignItems="center">
                          <Typography variant="body2" sx={{ mr: 1 }}>
                            {device.batteryLevel}%
                          </Typography>
                        </Box>
                        <LinearProgress
                          variant="determinate"
                          value={device.batteryLevel}
                          color={getBatteryColor(device.batteryLevel)}
                          sx={{ height: 6, borderRadius: 3 }}
                        />
                      </Box>
                    </TableCell>
                    <TableCell>
                      <Box sx={{ minWidth: 100 }}>
                        <Box display="flex" alignItems="center">
                          <Typography variant="body2" sx={{ mr: 1 }}>
                            {device.signalStrength}%
                          </Typography>
                        </Box>
                        <LinearProgress
                          variant="determinate"
                          value={device.signalStrength}
                          color={getSignalColor(device.signalStrength)}
                          sx={{ height: 6, borderRadius: 3 }}
                        />
                      </Box>
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={device.meshConnections}
                        variant="outlined"
                        size="small"
                        color={device.meshConnections > 0 ? 'success' : 'default'}
                      />
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2">
                        {device.lastSeen.toLocaleTimeString()}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <IconButton size="small" color="primary">
                        <SettingsIcon />
                      </IconButton>
                      <IconButton size="small" color="error">
                        <DeleteIcon />
                      </IconButton>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>
    </Box>
  );
}