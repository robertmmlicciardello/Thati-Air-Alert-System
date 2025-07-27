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
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Alert,
  Paper,
  Tabs,
  Tab
} from '@mui/material';
import {
  Delete as DeleteIcon,
  Send as SendIcon,
  Visibility as ViewIcon,
  Refresh as RefreshIcon
} from '@mui/icons-material';

// Mock alert history data
const mockAlertHistory = [
  {
    id: 1,
    message: 'Air quality alert: High pollution detected in Yangon area',
    type: 'warning',
    timestamp: new Date(Date.now() - 3600000),
    status: 'delivered',
    recipients: 15,
    sender: 'Admin User'
  },
  {
    id: 2,
    message: 'Emergency: Severe air pollution - Stay indoors',
    type: 'emergency',
    timestamp: new Date(Date.now() - 7200000),
    status: 'delivered',
    recipients: 23,
    sender: 'Regional Admin'
  },
  {
    id: 3,
    message: 'Weather update: Heavy rain expected',
    type: 'info',
    timestamp: new Date(Date.now() - 10800000),
    status: 'sent',
    recipients: 18,
    sender: 'Admin User'
  }
];

export default function Alerts() {
  const [alerts, setAlerts] = useState(mockAlertHistory);
  const [tabValue, setTabValue] = useState(0);
  const [openDialog, setOpenDialog] = useState(false);
  const [newAlert, setNewAlert] = useState({ message: '', type: 'warning' });

  const handleSendAlert = () => {
    if (!newAlert.message.trim()) return;

    const alert = {
      id: Date.now(),
      message: newAlert.message,
      type: newAlert.type,
      timestamp: new Date(),
      status: 'sent',
      recipients: 20, // Mock recipient count
      sender: 'Admin User'
    };

    setAlerts(prev => [alert, ...prev]);
    setNewAlert({ message: '', type: 'warning' });
    setOpenDialog(false);
  };

  const handleDeleteAlert = (alertId) => {
    setAlerts(prev => prev.filter(alert => alert.id !== alertId));
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'delivered': return 'success';
      case 'sent': return 'info';
      case 'failed': return 'error';
      default: return 'default';
    }
  };

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
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">
          Alert Management
        </Typography>
        <Box>
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            sx={{ mr: 2 }}
          >
            Refresh
          </Button>
          <Button
            variant="contained"
            startIcon={<SendIcon />}
            onClick={() => setOpenDialog(true)}
          >
            Send New Alert
          </Button>
        </Box>
      </Box>

      <Alert severity="info" sx={{ mb: 3 }}>
        Mesh network active - Alerts will be distributed through peer-to-peer connections
      </Alert>

      <Card>
        <CardContent>
          <Tabs value={tabValue} onChange={(e, newValue) => setTabValue(newValue)} sx={{ mb: 2 }}>
            <Tab label={`All Alerts (${alerts.length})`} />
            <Tab label={`Emergency (${alerts.filter(a => a.type === 'emergency').length})`} />
            <Tab label={`Warnings (${alerts.filter(a => a.type === 'warning').length})`} />
          </Tabs>

          <TableContainer component={Paper}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Message</TableCell>
                  <TableCell>Type</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Recipients</TableCell>
                  <TableCell>Sender</TableCell>
                  <TableCell>Time</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {alerts
                  .filter(alert => {
                    if (tabValue === 1) return alert.type === 'emergency';
                    if (tabValue === 2) return alert.type === 'warning';
                    return true;
                  })
                  .map((alert) => (
                    <TableRow key={alert.id}>
                      <TableCell>
                        <Typography variant="body2" sx={{ maxWidth: 300 }}>
                          {alert.message}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={alert.type}
                          color={getTypeColor(alert.type)}
                          size="small"
                        />
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={alert.status}
                          color={getStatusColor(alert.status)}
                          size="small"
                        />
                      </TableCell>
                      <TableCell>{alert.recipients}</TableCell>
                      <TableCell>{alert.sender}</TableCell>
                      <TableCell>
                        <Typography variant="body2">
                          {alert.timestamp.toLocaleString()}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <IconButton size="small" color="primary">
                          <ViewIcon />
                        </IconButton>
                        <IconButton 
                          size="small" 
                          color="error"
                          onClick={() => handleDeleteAlert(alert.id)}
                        >
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

      {/* Send Alert Dialog */}
      <Dialog open={openDialog} onClose={() => setOpenDialog(false)} maxWidth="md" fullWidth>
        <DialogTitle>Send New Alert</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            label="Alert Message"
            fullWidth
            multiline
            rows={4}
            variant="outlined"
            value={newAlert.message}
            onChange={(e) => setNewAlert(prev => ({ ...prev, message: e.target.value }))}
            placeholder="Enter alert message..."
            sx={{ mb: 2 }}
          />
          
          <Typography variant="body2" gutterBottom>
            Alert Type:
          </Typography>
          <Box sx={{ display: 'flex', gap: 1, mb: 2 }}>
            {['info', 'warning', 'emergency'].map((type) => (
              <Chip
                key={type}
                label={type}
                color={newAlert.type === type ? getTypeColor(type) : 'default'}
                onClick={() => setNewAlert(prev => ({ ...prev, type }))}
                clickable
              />
            ))}
          </Box>

          <Alert severity="info">
            Alert will be sent to all connected devices via mesh network
          </Alert>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDialog(false)}>Cancel</Button>
          <Button 
            onClick={handleSendAlert} 
            variant="contained"
            disabled={!newAlert.message.trim()}
          >
            Send Alert
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}