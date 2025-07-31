import React, { useState, useEffect } from 'react';
import {
  Box,
  Grid,
  Card,
  CardContent,
  Typography,
  Button,
  Alert,
  Tabs,
  Tab,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  Chip,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Switch,
  FormControlLabel,
  Divider,
  CircularProgress
} from '@mui/material';
import {
  NetworkWifi as NetworkIcon,
  Router as RouterIcon,
  Settings as SettingsIcon,
  PlayArrow as StartIcon,
  Stop as StopIcon,
  Refresh as RefreshIcon,
  Warning as WarningIcon,
  CheckCircle as CheckIcon,
  Error as ErrorIcon,
  Speed as SpeedIcon,
  Timeline as TimelineIcon,
  DeviceHub as HubIcon
} from '@mui/icons-material';
import MeshNetworkVisualization from '../components/network/MeshNetworkVisualization';
import { useMeshNetwork } from '../hooks/useMeshNetwork';

/**
 * Network Management Page - Mesh network ကို manage လုပ်ရန်
 */
export default function Network() {
  const [currentTab, setCurrentTab] = useState(0);
  const [networkMode, setNetworkMode] = useState('admin');
  const [openSettings, setOpenSettings] = useState(false);
  const [networkConfig, setNetworkConfig] = useState({
    discoveryInterval: 30,
    heartbeatInterval: 30,
    maxHops: 5,
    enableWifiDirect: true,
    enableBluetooth: true,
    autoOptimize: true
  });

  // Use mesh network hook
  const {
    isConnected,
    networkStatus,
    topology,
    statistics,
    health,
    alerts,
    isLoading,
    isOptimizing,
    isUpdatingConfig,
    refreshAllData,
    optimizeNetwork,
    updateNetworkConfig,
    resolveAlert,
    getNetworkSummary
  } = useMeshNetwork();

  const networkSummary = getNetworkSummary();
  const networkIssues = alerts?.alerts?.filter(alert => !alert.resolved) || [];
  const networkRecommendations = health?.recommendations || [];

  const handleStartNetwork = () => {
    console.log(`Starting mesh network in ${networkMode} mode`);
    // In real implementation, call API to start network
    refreshAllData();
  };

  const handleStopNetwork = () => {
    console.log('Stopping mesh network');
    // In real implementation, call API to stop network
    refreshAllData();
  };

  const handleOptimizeNetwork = () => {
    optimizeNetwork();
  };

  const handleSaveSettings = () => {
    updateNetworkConfig(networkConfig);
    setOpenSettings(false);
  };

  const getStatusColor = (status) => {
    if (isConnected && networkSummary?.onlineNodes > 0) return 'success';
    if (isLoading) return 'warning';
    return 'error';
  };

  const getStatusIcon = (status) => {
    if (isLoading) return <CircularProgress size={20} />;
    if (isConnected && networkSummary?.onlineNodes > 0) return <CheckIcon color="success" />;
    return <ErrorIcon color="error" />;
  };

  const getNetworkStatusText = () => {
    if (isLoading) return 'LOADING';
    if (isConnected && networkSummary?.onlineNodes > 0) return 'RUNNING';
    return 'STOPPED';
  };

  const getIssueIcon = (type) => {
    switch (type) {
      case 'error': return <ErrorIcon color="error" />;
      case 'warning': return <WarningIcon color="warning" />;
      case 'info': return <CheckIcon color="info" />;
      default: return <NetworkIcon />;
    }
  };

  const TabPanel = ({ children, value, index }) => (
    <div hidden={value !== index}>
      {value === index && <Box sx={{ pt: 3 }}>{children}</Box>}
    </div>
  );

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Mesh Network Management
      </Typography>

      {/* Network Status Card */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Grid container spacing={3} alignItems="center">
            <Grid item xs={12} md={3}>
              <Box display="flex" alignItems="center">
                {getStatusIcon(networkStatus)}
                <Box ml={2}>
                  <Typography variant="h6">
                    Network Status
                  </Typography>
                  <Chip
                    label={getNetworkStatusText()}
                    color={getStatusColor()}
                    size="small"
                  />
                </Box>
              </Box>
            </Grid>
            
            <Grid item xs={12} md={3}>
              <FormControl fullWidth size="small">
                <InputLabel>Network Mode</InputLabel>
                <Select
                  value={networkMode}
                  onChange={(e) => setNetworkMode(e.target.value)}
                  disabled={isConnected && networkSummary?.onlineNodes > 0}
                >
                  <MenuItem value="admin">Admin Mode</MenuItem>
                  <MenuItem value="user">User Mode</MenuItem>
                  <MenuItem value="relay">Relay Mode</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            
            <Grid item xs={12} md={6}>
              <Box display="flex" gap={1} justifyContent="flex-end">
                {!(isConnected && networkSummary?.onlineNodes > 0) ? (
                  <Button
                    variant="contained"
                    color="success"
                    startIcon={<StartIcon />}
                    onClick={handleStartNetwork}
                    disabled={isLoading}
                  >
                    Start Network
                  </Button>
                ) : (
                  <Button
                    variant="contained"
                    color="error"
                    startIcon={<StopIcon />}
                    onClick={handleStopNetwork}
                    disabled={isLoading}
                  >
                    Stop Network
                  </Button>
                )}
                
                <Button
                  variant="outlined"
                  startIcon={isOptimizing ? <CircularProgress size={16} /> : <SpeedIcon />}
                  onClick={handleOptimizeNetwork}
                  disabled={!isConnected || isOptimizing || isLoading}
                >
                  {isOptimizing ? 'Optimizing...' : 'Optimize'}
                </Button>
                
                <IconButton
                  onClick={refreshAllData}
                  color="primary"
                  disabled={isLoading}
                >
                  <RefreshIcon />
                </IconButton>
                
                <IconButton
                  onClick={() => setOpenSettings(true)}
                  color="primary"
                >
                  <SettingsIcon />
                </IconButton>
              </Box>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {/* Network Issues Alert */}
      {networkIssues.length > 0 && (
        <Alert severity="warning" sx={{ mb: 3 }}>
          <Typography variant="subtitle2" gutterBottom>
            Network Issues Detected ({networkIssues.length})
          </Typography>
          {networkIssues.slice(0, 2).map(issue => (
            <Typography key={issue.id} variant="body2">
              • {issue.message}
            </Typography>
          ))}
          {networkIssues.length > 2 && (
            <Typography variant="body2" color="textSecondary">
              ... and {networkIssues.length - 2} more issues
            </Typography>
          )}
        </Alert>
      )}

      {/* Tabs */}
      <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 3 }}>
        <Tabs value={currentTab} onChange={(e, newValue) => setCurrentTab(newValue)}>
          <Tab label="Network Topology" icon={<HubIcon />} />
          <Tab label="Performance" icon={<TimelineIcon />} />
          <Tab label="Issues & Recommendations" icon={<WarningIcon />} />
        </Tabs>
      </Box>

      {/* Tab Panels */}
      <TabPanel value={currentTab} index={0}>
        <MeshNetworkVisualization />
      </TabPanel>

      <TabPanel value={currentTab} index={1}>
        <Grid container spacing={3}>
          {/* Performance Metrics */}
          <Grid item xs={12} md={6}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom>
                  Performance Metrics
                </Typography>
                
                <List>
                  <ListItem>
                    <ListItemText
                      primary="Message Delivery Rate"
                      secondary="95.2% (1,198 / 1,247 messages)"
                    />
                    <Chip label="Excellent" color="success" size="small" />
                  </ListItem>
                  
                  <ListItem>
                    <ListItemText
                      primary="Average Latency"
                      secondary="75ms"
                    />
                    <Chip label="Good" color="success" size="small" />
                  </ListItem>
                  
                  <ListItem>
                    <ListItemText
                      primary="Network Throughput"
                      secondary="2.3 MB/s"
                    />
                    <Chip label="Normal" color="warning" size="small" />
                  </ListItem>
                  
                  <ListItem>
                    <ListItemText
                      primary="Connection Stability"
                      secondary="98.7% uptime"
                    />
                    <Chip label="Excellent" color="success" size="small" />
                  </ListItem>
                </List>
              </CardContent>
            </Card>
          </Grid>

          {/* Network Load */}
          <Grid item xs={12} md={6}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom>
                  Network Load
                </Typography>
                
                <List>
                  <ListItem>
                    <ListItemText
                      primary="Messages per Minute"
                      secondary="12.5 avg"
                    />
                  </ListItem>
                  
                  <ListItem>
                    <ListItemText
                      primary="Peak Load"
                      secondary="45 messages/min at 14:30"
                    />
                  </ListItem>
                  
                  <ListItem>
                    <ListItemText
                      primary="Emergency Alerts"
                      secondary="3 sent today"
                    />
                  </ListItem>
                  
                  <ListItem>
                    <ListItemText
                      primary="Forwarded Messages"
                      secondary="89 (7.1% of total)"
                    />
                  </ListItem>
                </List>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </TabPanel>

      <TabPanel value={currentTab} index={2}>
        <Grid container spacing={3}>
          {/* Network Issues */}
          <Grid item xs={12} md={6}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom>
                  Network Issues
                </Typography>
                
                {networkIssues.length === 0 ? (
                  <Alert severity="success">
                    No network issues detected
                  </Alert>
                ) : (
                  <List>
                    {networkIssues.map(issue => (
                      <ListItem key={issue.id}>
                        <ListItemIcon>
                          {getIssueIcon(issue.type)}
                        </ListItemIcon>
                        <ListItemText
                          primary={issue.message}
                          secondary={issue.timestamp.toLocaleString()}
                        />
                        <Chip
                          label={issue.severity}
                          color={issue.severity === 'high' ? 'error' : 
                                issue.severity === 'medium' ? 'warning' : 'info'}
                          size="small"
                        />
                      </ListItem>
                    ))}
                  </List>
                )}
              </CardContent>
            </Card>
          </Grid>

          {/* Recommendations */}
          <Grid item xs={12} md={6}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom>
                  Recommendations
                </Typography>
                
                <List>
                  {networkRecommendations.map((recommendation, index) => (
                    <ListItem key={index}>
                      <ListItemIcon>
                        <CheckIcon color="info" />
                      </ListItemIcon>
                      <ListItemText primary={recommendation} />
                    </ListItem>
                  ))}
                </List>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </TabPanel>

      {/* Settings Dialog */}
      <Dialog open={openSettings} onClose={() => setOpenSettings(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Network Settings</DialogTitle>
        <DialogContent>
          <Grid container spacing={3} sx={{ mt: 1 }}>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Discovery Interval (seconds)"
                type="number"
                value={networkConfig.discoveryInterval}
                onChange={(e) => setNetworkConfig({
                  ...networkConfig,
                  discoveryInterval: parseInt(e.target.value)
                })}
              />
            </Grid>
            
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Heartbeat Interval (seconds)"
                type="number"
                value={networkConfig.heartbeatInterval}
                onChange={(e) => setNetworkConfig({
                  ...networkConfig,
                  heartbeatInterval: parseInt(e.target.value)
                })}
              />
            </Grid>
            
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Max Hops"
                type="number"
                value={networkConfig.maxHops}
                onChange={(e) => setNetworkConfig({
                  ...networkConfig,
                  maxHops: parseInt(e.target.value)
                })}
              />
            </Grid>
            
            <Grid item xs={12}>
              <Divider />
            </Grid>
            
            <Grid item xs={12}>
              <FormControlLabel
                control={
                  <Switch
                    checked={networkConfig.enableWifiDirect}
                    onChange={(e) => setNetworkConfig({
                      ...networkConfig,
                      enableWifiDirect: e.target.checked
                    })}
                  />
                }
                label="Enable Wi-Fi Direct"
              />
            </Grid>
            
            <Grid item xs={12}>
              <FormControlLabel
                control={
                  <Switch
                    checked={networkConfig.enableBluetooth}
                    onChange={(e) => setNetworkConfig({
                      ...networkConfig,
                      enableBluetooth: e.target.checked
                    })}
                  />
                }
                label="Enable Bluetooth"
              />
            </Grid>
            
            <Grid item xs={12}>
              <FormControlLabel
                control={
                  <Switch
                    checked={networkConfig.autoOptimize}
                    onChange={(e) => setNetworkConfig({
                      ...networkConfig,
                      autoOptimize: e.target.checked
                    })}
                  />
                }
                label="Auto-optimize Network"
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenSettings(false)}>Cancel</Button>
          <Button onClick={handleSaveSettings} variant="contained">
            Save Settings
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}