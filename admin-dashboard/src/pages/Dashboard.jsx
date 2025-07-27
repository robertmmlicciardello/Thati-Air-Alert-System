import React, { useState, useEffect } from 'react';
import {
  Box,
  Grid,
  Card,
  CardContent,
  Typography,
  Button,
  TextField,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Chip,
  List,
  ListItem,
  ListItemText,
  ListItemSecondaryAction,
  IconButton,
  Alert,
  Paper,
  Divider
} from '@mui/material';
import {
  Warning as WarningIcon,
  Send as SendIcon,
  People as PeopleIcon,
  Devices as DevicesIcon,
  NetworkWifi as NetworkIcon,
  Delete as DeleteIcon,
  CheckCircle as CheckIcon
} from '@mui/icons-material';

// Mock data for offline mode
const mockDevices = [
  { id: 1, name: 'Device-001', status: 'online', location: 'Yangon Central', lastSeen: new Date() },
  { id: 2, name: 'Device-002', status: 'online', location: 'Mandalay North', lastSeen: new Date() },
  { id: 3, name: 'Device-003', status: 'offline', location: 'Naypyidaw', lastSeen: new Date(Date.now() - 300000) }
];

const mockUsers = [
  { id: 1, name: 'Admin User', role: 'admin', status: 'active' },
  { id: 2, name: 'Regional Admin', role: 'regional', status: 'active' },
  { id: 3, name: 'Local Operator', role: 'operator', status: 'active' }
];

export default function Dashboard() {
  const [devices, setDevices] = useState(mockDevices);
  const [users, setUsers] = useState(mockUsers);
  const [alerts, setAlerts] = useState([]);
  const [openAlertDialog, setOpenAlertDialog] = useState(false);
  const [alertMessage, setAlertMessage] = useState('');
  const [alertType, setAlertType] = useState('warning');
  const [sentAlerts, setSentAlerts] = useState([]);

  // Stats calculation
  const onlineDevices = devices.filter(d => d.status === 'online').length;
  const totalDevices = devices.length;
  const activeUsers = users.filter(u => u.status === 'active').length;

  const handleSendAlert = () => {
    if (!alertMessage.trim()) return;

    const newAlert = {
      id: Date.now(),
      message: alertMessage,
      type: alertType,
      timestamp: new Date(),
      status: 'sent',
      recipients: onlineDevices
    };

    setSentAlerts(prev => [newAlert, ...prev]);
    setAlerts(prev => [newAlert, ...prev]);
    setAlertMessage('');
    setOpenAlertDialog(false);
  };

  const handleDeleteAlert = (alertId) => {
    setSentAlerts(prev => prev.filter(alert => alert.id !== alertId));
  };

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Dashboard - Offline Mode
      </Typography>
      
      <Alert severity="info" sx={{ mb: 3 }}>
        Running in offline mode with demo data. Mesh network simulation active.
      </Alert>

      {/* Stats Cards */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Box display="flex" alignItems="center">
                <DevicesIcon color="primary" sx={{ mr: 2 }} />
                <Box>
                  <Typography color="textSecondary" gutterBottom>
                    Active Devices
                  </Typography>
                  <Typography variant="h4">
                    {onlineDevices}/{totalDevices}
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
                <PeopleIcon color="primary" sx={{ mr: 2 }} />
                <Box>
                  <Typography color="textSecondary" gutterBottom>
                    Active Users
                  </Typography>
                  <Typography variant="h4">
                    {activeUsers}
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
                <WarningIcon color="warning" sx={{ mr: 2 }} />
                <Box>
                  <Typography color="textSecondary" gutterBottom>
                    Alerts Sent
                  </Typography>
                  <Typography variant="h4">
                    {sentAlerts.length}
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
                <NetworkIcon color="success" sx={{ mr: 2 }} />
                <Box>
                  <Typography color="textSecondary" gutterBottom>
                    Network Status
                  </Typography>
                  <Typography variant="h6" color="success.main">
                    Mesh Active
                  </Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Grid container spacing={3}>
        {/* Send Alert Section */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Send Emergency Alert
              </Typography>
              <Button
                variant="contained"
                color="error"
                startIcon={<SendIcon />}
                onClick={() => setOpenAlertDialog(true)}
                fullWidth
                size="large"
              >
                Send Alert to All Devices
              </Button>
            </CardContent>
          </Card>
        </Grid>

        {/* Recent Alerts */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Recent Alerts ({sentAlerts.length})
              </Typography>
              {sentAlerts.length === 0 ? (
                <Typography color="textSecondary">
                  No alerts sent yet
                </Typography>
              ) : (
                <List dense>
                  {sentAlerts.slice(0, 3).map((alert) => (
                    <ListItem key={alert.id}>
                      <ListItemText
                        primary={alert.message}
                        secondary={
                          <Box>
                            <Typography variant="caption" display="block">
                              {alert.timestamp.toLocaleString()}
                            </Typography>
                            <Chip 
                              icon={<CheckIcon />}
                              label={`Sent to ${alert.recipients} devices`}
                              size="small"
                              color="success"
                              variant="outlined"
                            />
                          </Box>
                        }
                      />
                      <ListItemSecondaryAction>
                        <IconButton 
                          edge="end" 
                          onClick={() => handleDeleteAlert(alert.id)}
                          size="small"
                        >
                          <DeleteIcon />
                        </IconButton>
                      </ListItemSecondaryAction>
                    </ListItem>
                  ))}
                </List>
              )}
            </CardContent>
          </Card>
        </Grid>

        {/* Connected Devices */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Connected Devices
              </Typography>
              <List dense>
                {devices.map((device) => (
                  <ListItem key={device.id}>
                    <ListItemText
                      primary={device.name}
                      secondary={`${device.location} • ${device.status}`}
                    />
                    <Chip
                      label={device.status}
                      color={device.status === 'online' ? 'success' : 'error'}
                      size="small"
                    />
                  </ListItem>
                ))}
              </List>
            </CardContent>
          </Card>
        </Grid>

        {/* System Status */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                System Status
              </Typography>
              <Box sx={{ mb: 2 }}>
                <Typography variant="body2" color="textSecondary">
                  Mesh Network
                </Typography>
                <Chip label="Active" color="success" size="small" />
              </Box>
              <Box sx={{ mb: 2 }}>
                <Typography variant="body2" color="textSecondary">
                  Alert System
                </Typography>
                <Chip label="Ready" color="success" size="small" />
              </Box>
              <Box sx={{ mb: 2 }}>
                <Typography variant="body2" color="textSecondary">
                  Database
                </Typography>
                <Chip label="Offline Mode" color="warning" size="small" />
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Send Alert Dialog */}
      <Dialog open={openAlertDialog} onClose={() => setOpenAlertDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Send Emergency Alert</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            label="Alert Message"
            fullWidth
            multiline
            rows={4}
            variant="outlined"
            value={alertMessage}
            onChange={(e) => setAlertMessage(e.target.value)}
            placeholder="Enter emergency alert message..."
          />
          <Box sx={{ mt: 2 }}>
            <Typography variant="body2" gutterBottom>
              Alert Type:
            </Typography>
            <Box sx={{ display: 'flex', gap: 1 }}>
              <Chip
                label="Warning"
                color={alertType === 'warning' ? 'warning' : 'default'}
                onClick={() => setAlertType('warning')}
                clickable
              />
              <Chip
                label="Emergency"
                color={alertType === 'emergency' ? 'error' : 'default'}
                onClick={() => setAlertType('emergency')}
                clickable
              />
              <Chip
                label="Info"
                color={alertType === 'info' ? 'info' : 'default'}
                onClick={() => setAlertType('info')}
                clickable
              />
            </Box>
          </Box>
          <Alert severity="info" sx={{ mt: 2 }}>
            This alert will be sent to {onlineDevices} online devices via mesh network.
          </Alert>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenAlertDialog(false)}>Cancel</Button>
          <Button 
            onClick={handleSendAlert} 
            variant="contained" 
            color="error"
            disabled={!alertMessage.trim()}
          >
            Send Alert
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}