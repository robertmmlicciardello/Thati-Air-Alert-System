import React, { useState } from 'react';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Grid,
  Switch,
  FormControlLabel,
  TextField,
  Button,
  Divider,
  Alert,
  Chip,
  List,
  ListItem,
  ListItemText,
  ListItemSecondaryAction
} from '@mui/material';
import {
  Save as SaveIcon,
  Refresh as RefreshIcon,
  NetworkWifi as NetworkIcon
} from '@mui/icons-material';

export default function Settings() {
  const [settings, setSettings] = useState({
    meshNetwork: {
      enabled: true,
      autoReconnect: true,
      maxConnections: 10,
      signalThreshold: 30
    },
    alerts: {
      autoForward: true,
      soundEnabled: true,
      vibrationEnabled: true,
      maxRetries: 3
    },
    system: {
      offlineMode: true,
      dataSync: false,
      debugMode: false,
      logLevel: 'info'
    }
  });

  const handleSettingChange = (category, setting, value) => {
    setSettings(prev => ({
      ...prev,
      [category]: {
        ...prev[category],
        [setting]: value
      }
    }));
  };

  const handleSave = () => {
    // Save settings to local storage in offline mode
    localStorage.setItem('thati-settings', JSON.stringify(settings));
    alert('Settings saved successfully!');
  };

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">
          System Settings
        </Typography>
        <Button
          variant="contained"
          startIcon={<SaveIcon />}
          onClick={handleSave}
        >
          Save Settings
        </Button>
      </Box>

      <Alert severity="warning" sx={{ mb: 3 }}>
        Offline mode active - Settings are stored locally and will sync when connection is restored.
      </Alert>

      <Grid container spacing={3}>
        {/* Mesh Network Settings */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                <NetworkIcon sx={{ mr: 1, verticalAlign: 'middle' }} />
                Mesh Network Configuration
              </Typography>
              
              <FormControlLabel
                control={
                  <Switch
                    checked={settings.meshNetwork.enabled}
                    onChange={(e) => handleSettingChange('meshNetwork', 'enabled', e.target.checked)}
                  />
                }
                label="Enable Mesh Network"
              />
              
              <FormControlLabel
                control={
                  <Switch
                    checked={settings.meshNetwork.autoReconnect}
                    onChange={(e) => handleSettingChange('meshNetwork', 'autoReconnect', e.target.checked)}
                  />
                }
                label="Auto Reconnect"
              />

              <Box sx={{ mt: 2 }}>
                <TextField
                  label="Max Connections"
                  type="number"
                  value={settings.meshNetwork.maxConnections}
                  onChange={(e) => handleSettingChange('meshNetwork', 'maxConnections', parseInt(e.target.value))}
                  fullWidth
                  margin="normal"
                  inputProps={{ min: 1, max: 20 }}
                />
                
                <TextField
                  label="Signal Threshold (%)"
                  type="number"
                  value={settings.meshNetwork.signalThreshold}
                  onChange={(e) => handleSettingChange('meshNetwork', 'signalThreshold', parseInt(e.target.value))}
                  fullWidth
                  margin="normal"
                  inputProps={{ min: 10, max: 90 }}
                />
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Alert Settings */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Alert Configuration
              </Typography>
              
              <FormControlLabel
                control={
                  <Switch
                    checked={settings.alerts.autoForward}
                    onChange={(e) => handleSettingChange('alerts', 'autoForward', e.target.checked)}
                  />
                }
                label="Auto Forward Alerts"
              />
              
              <FormControlLabel
                control={
                  <Switch
                    checked={settings.alerts.soundEnabled}
                    onChange={(e) => handleSettingChange('alerts', 'soundEnabled', e.target.checked)}
                  />
                }
                label="Sound Notifications"
              />
              
              <FormControlLabel
                control={
                  <Switch
                    checked={settings.alerts.vibrationEnabled}
                    onChange={(e) => handleSettingChange('alerts', 'vibrationEnabled', e.target.checked)}
                  />
                }
                label="Vibration Alerts"
              />

              <TextField
                label="Max Retry Attempts"
                type="number"
                value={settings.alerts.maxRetries}
                onChange={(e) => handleSettingChange('alerts', 'maxRetries', parseInt(e.target.value))}
                fullWidth
                margin="normal"
                inputProps={{ min: 1, max: 10 }}
              />
            </CardContent>
          </Card>
        </Grid>

        {/* System Settings */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                System Configuration
              </Typography>
              
              <FormControlLabel
                control={
                  <Switch
                    checked={settings.system.offlineMode}
                    onChange={(e) => handleSettingChange('system', 'offlineMode', e.target.checked)}
                  />
                }
                label="Offline Mode"
              />
              
              <FormControlLabel
                control={
                  <Switch
                    checked={settings.system.dataSync}
                    onChange={(e) => handleSettingChange('system', 'dataSync', e.target.checked)}
                    disabled={settings.system.offlineMode}
                  />
                }
                label="Auto Data Sync"
              />
              
              <FormControlLabel
                control={
                  <Switch
                    checked={settings.system.debugMode}
                    onChange={(e) => handleSettingChange('system', 'debugMode', e.target.checked)}
                  />
                }
                label="Debug Mode"
              />

              <TextField
                label="Log Level"
                select
                value={settings.system.logLevel}
                onChange={(e) => handleSettingChange('system', 'logLevel', e.target.value)}
                fullWidth
                margin="normal"
                SelectProps={{
                  native: true,
                }}
              >
                <option value="error">Error</option>
                <option value="warn">Warning</option>
                <option value="info">Info</option>
                <option value="debug">Debug</option>
              </TextField>
            </CardContent>
          </Card>
        </Grid>

        {/* Network Status */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Network Status
              </Typography>
              
              <List dense>
                <ListItem>
                  <ListItemText primary="Mesh Network" />
                  <ListItemSecondaryAction>
                    <Chip 
                      label={settings.meshNetwork.enabled ? "Active" : "Disabled"} 
                      color={settings.meshNetwork.enabled ? "success" : "default"}
                      size="small"
                    />
                  </ListItemSecondaryAction>
                </ListItem>
                
                <ListItem>
                  <ListItemText primary="Connected Devices" />
                  <ListItemSecondaryAction>
                    <Chip label="5" color="info" size="small" />
                  </ListItemSecondaryAction>
                </ListItem>
                
                <ListItem>
                  <ListItemText primary="Signal Strength" />
                  <ListItemSecondaryAction>
                    <Chip label="85%" color="success" size="small" />
                  </ListItemSecondaryAction>
                </ListItem>
                
                <ListItem>
                  <ListItemText primary="Last Sync" />
                  <ListItemSecondaryAction>
                    <Chip label="Offline" color="warning" size="small" />
                  </ListItemSecondaryAction>
                </ListItem>
              </List>

              <Button
                variant="outlined"
                startIcon={<RefreshIcon />}
                fullWidth
                sx={{ mt: 2 }}
              >
                Refresh Network Status
              </Button>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
}