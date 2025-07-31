import React, { useState, useEffect, useRef } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Grid,
  Chip,
  IconButton,
  Switch,
  FormControlLabel,
  LinearProgress,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  Divider
} from '@mui/material';
import {
  NetworkWifi as NetworkIcon,
  Router as RouterIcon,
  DeviceHub as HubIcon,
  SignalWifi4Bar as SignalIcon,
  Battery90 as BatteryIcon,
  Refresh as RefreshIcon
} from '@mui/icons-material';

/**
 * Mesh Network Visualization Component
 * Real-time mesh network topology နဲ့ status ကို display လုပ်တဲ့ component
 */
export default function MeshNetworkVisualization() {
  const canvasRef = useRef(null);
  const [networkData, setNetworkData] = useState(null);
  const [networkStats, setNetworkStats] = useState({});
  const [isAutoRefresh, setIsAutoRefresh] = useState(true);
  const [selectedNode, setSelectedNode] = useState(null);
  const [networkHealth, setNetworkHealth] = useState(85);

  // Mock network data - အစစ်မှာ API ကနေ လာမယ်
  const mockNetworkData = {
    nodes: [
      {
        id: 'admin-001',
        name: 'Admin Device',
        type: 'admin',
        x: 400,
        y: 200,
        status: 'online',
        batteryLevel: 85,
        signalStrength: 90,
        connections: 3,
        lastSeen: new Date()
      },
      {
        id: 'relay-001',
        name: 'Relay Device 1',
        type: 'relay',
        x: 200,
        y: 150,
        status: 'online',
        batteryLevel: 70,
        signalStrength: 75,
        connections: 2,
        lastSeen: new Date()
      },
      {
        id: 'relay-002',
        name: 'Relay Device 2',
        type: 'relay',
        x: 600,
        y: 150,
        status: 'online',
        batteryLevel: 65,
        signalStrength: 80,
        connections: 2,
        lastSeen: new Date()
      },
      {
        id: 'user-001',
        name: 'User Device 1',
        type: 'user',
        x: 100,
        y: 300,
        status: 'online',
        batteryLevel: 60,
        signalStrength: 65,
        connections: 1,
        lastSeen: new Date()
      },
      {
        id: 'user-002',
        name: 'User Device 2',
        type: 'user',
        x: 300,
        y: 350,
        status: 'online',
        batteryLevel: 80,
        signalStrength: 70,
        connections: 1,
        lastSeen: new Date()
      },
      {
        id: 'user-003',
        name: 'User Device 3',
        type: 'user',
        x: 700,
        y: 300,
        status: 'offline',
        batteryLevel: 20,
        signalStrength: 30,
        connections: 0,
        lastSeen: new Date(Date.now() - 300000)
      }
    ],
    edges: [
      { from: 'admin-001', to: 'relay-001', type: 'wifi_direct', strength: 85, active: true },
      { from: 'admin-001', to: 'relay-002', type: 'wifi_direct', strength: 80, active: true },
      { from: 'admin-001', to: 'user-002', type: 'bluetooth', strength: 70, active: true },
      { from: 'relay-001', to: 'user-001', type: 'bluetooth', strength: 70, active: true },
      { from: 'relay-002', to: 'user-003', type: 'wifi_direct', strength: 30, active: false }
    ],
    metadata: {
      topology_type: 'hybrid',
      total_nodes: 6,
      active_connections: 4,
      network_diameter: 3
    }
  };

  const mockNetworkStats = {
    messages_sent: 1247,
    messages_received: 1198,
    messages_forwarded: 89,
    network_uptime: 7200000, // 2 hours
    connected_devices: 5,
    network_health: 85,
    topology_type: 'hybrid',
    coverage_area: 50000, // 50 km²
    emergency_alerts_sent: 3,
    average_latency: 75
  };

  useEffect(() => {
    // Initialize network data
    setNetworkData(mockNetworkData);
    setNetworkStats(mockNetworkStats);
    setNetworkHealth(mockNetworkStats.network_health);

    // Auto refresh
    let interval;
    if (isAutoRefresh) {
      interval = setInterval(() => {
        refreshNetworkData();
      }, 5000);
    }

    return () => {
      if (interval) clearInterval(interval);
    };
  }, [isAutoRefresh]);

  useEffect(() => {
    if (networkData && canvasRef.current) {
      drawNetworkTopology();
    }
  }, [networkData, selectedNode]);

  const refreshNetworkData = () => {
    // Mock data refresh - အစစ်မှာ API call လုပ်မယ်
    console.log('Refreshing network data...');
    
    // Simulate some changes
    const updatedData = { ...mockNetworkData };
    updatedData.nodes = updatedData.nodes.map(node => ({
      ...node,
      batteryLevel: Math.max(10, node.batteryLevel - Math.random() * 2),
      signalStrength: Math.max(30, node.signalStrength + (Math.random() - 0.5) * 10),
      lastSeen: node.status === 'online' ? new Date() : node.lastSeen
    }));
    
    setNetworkData(updatedData);
    
    // Update stats
    const updatedStats = { ...mockNetworkStats };
    updatedStats.messages_sent += Math.floor(Math.random() * 5);
    updatedStats.messages_received += Math.floor(Math.random() * 5);
    updatedStats.network_uptime += 5000;
    
    setNetworkStats(updatedStats);
  };

  const drawNetworkTopology = () => {
    const canvas = canvasRef.current;
    if (!canvas || !networkData) return;

    const ctx = canvas.getContext('2d');
    const { width, height } = canvas;
    
    // Clear canvas
    ctx.clearRect(0, 0, width, height);
    
    // Draw connections first
    networkData.edges.forEach(edge => {
      const fromNode = networkData.nodes.find(n => n.id === edge.from);
      const toNode = networkData.nodes.find(n => n.id === edge.to);
      
      if (fromNode && toNode) {
        drawConnection(ctx, fromNode, toNode, edge);
      }
    });
    
    // Draw nodes
    networkData.nodes.forEach(node => {
      drawNode(ctx, node);
    });
  };

  const drawConnection = (ctx, fromNode, toNode, edge) => {
    ctx.beginPath();
    ctx.moveTo(fromNode.x, fromNode.y);
    ctx.lineTo(toNode.x, toNode.y);
    
    // Connection style based on type and status
    if (edge.active) {
      ctx.strokeStyle = edge.type === 'wifi_direct' ? '#2196F3' : '#9C27B0';
      ctx.lineWidth = Math.max(1, edge.strength / 30);
      ctx.setLineDash([]);
    } else {
      ctx.strokeStyle = '#BDBDBD';
      ctx.lineWidth = 1;
      ctx.setLineDash([5, 5]);
    }
    
    ctx.stroke();
    
    // Draw connection label
    const midX = (fromNode.x + toNode.x) / 2;
    const midY = (fromNode.y + toNode.y) / 2;
    
    ctx.fillStyle = '#666';
    ctx.font = '10px Arial';
    ctx.textAlign = 'center';
    ctx.fillText(`${edge.strength}%`, midX, midY - 5);
  };

  const drawNode = (ctx, node) => {
    const radius = node.type === 'admin' ? 25 : node.type === 'relay' ? 20 : 15;
    const isSelected = selectedNode && selectedNode.id === node.id;
    
    // Node circle
    ctx.beginPath();
    ctx.arc(node.x, node.y, radius, 0, 2 * Math.PI);
    
    // Node color based on type and status
    let fillColor = '#BDBDBD';
    if (node.status === 'online') {
      switch (node.type) {
        case 'admin': fillColor = '#F44336'; break;
        case 'relay': fillColor = '#FF9800'; break;
        case 'user': fillColor = '#4CAF50'; break;
      }
    }
    
    ctx.fillStyle = fillColor;
    ctx.fill();
    
    // Selection highlight
    if (isSelected) {
      ctx.strokeStyle = '#2196F3';
      ctx.lineWidth = 3;
      ctx.stroke();
    }
    
    // Node border
    ctx.strokeStyle = '#333';
    ctx.lineWidth = 1;
    ctx.stroke();
    
    // Node label
    ctx.fillStyle = '#333';
    ctx.font = '12px Arial';
    ctx.textAlign = 'center';
    ctx.fillText(node.name, node.x, node.y + radius + 15);
    
    // Status indicators
    if (node.status === 'online') {
      // Battery indicator
      const batteryWidth = 20;
      const batteryHeight = 6;
      const batteryX = node.x - batteryWidth / 2;
      const batteryY = node.y + radius + 20;
      
      ctx.fillStyle = node.batteryLevel > 30 ? '#4CAF50' : '#F44336';
      ctx.fillRect(batteryX, batteryY, (batteryWidth * node.batteryLevel) / 100, batteryHeight);
      
      ctx.strokeStyle = '#333';
      ctx.strokeRect(batteryX, batteryY, batteryWidth, batteryHeight);
    }
  };

  const handleCanvasClick = (event) => {
    if (!networkData) return;
    
    const canvas = canvasRef.current;
    const rect = canvas.getBoundingClientRect();
    const x = event.clientX - rect.left;
    const y = event.clientY - rect.top;
    
    // Find clicked node
    const clickedNode = networkData.nodes.find(node => {
      const distance = Math.sqrt((x - node.x) ** 2 + (y - node.y) ** 2);
      const radius = node.type === 'admin' ? 25 : node.type === 'relay' ? 20 : 15;
      return distance <= radius;
    });
    
    setSelectedNode(clickedNode);
  };

  const getNodeTypeIcon = (type) => {
    switch (type) {
      case 'admin': return <HubIcon color="error" />;
      case 'relay': return <RouterIcon color="warning" />;
      case 'user': return <NetworkIcon color="success" />;
      default: return <NetworkIcon />;
    }
  };

  const getHealthColor = (health) => {
    if (health >= 80) return 'success';
    if (health >= 60) return 'warning';
    return 'error';
  };

  const formatUptime = (milliseconds) => {
    const hours = Math.floor(milliseconds / (1000 * 60 * 60));
    const minutes = Math.floor((milliseconds % (1000 * 60 * 60)) / (1000 * 60));
    return `${hours}h ${minutes}m`;
  };

  return (
    <Box>
      <Grid container spacing={3}>
        {/* Network Overview */}
        <Grid item xs={12} md={8}>
          <Card>
            <CardContent>
              <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
                <Typography variant="h6">
                  Mesh Network Topology
                </Typography>
                <Box>
                  <FormControlLabel
                    control={
                      <Switch
                        checked={isAutoRefresh}
                        onChange={(e) => setIsAutoRefresh(e.target.checked)}
                        size="small"
                      />
                    }
                    label="Auto Refresh"
                  />
                  <IconButton onClick={refreshNetworkData} size="small">
                    <RefreshIcon />
                  </IconButton>
                </Box>
              </Box>
              
              {/* Network Health */}
              <Box mb={2}>
                <Box display="flex" alignItems="center" mb={1}>
                  <Typography variant="body2" color="textSecondary">
                    Network Health: {networkHealth}%
                  </Typography>
                  <Chip
                    label={networkHealth >= 80 ? 'Excellent' : networkHealth >= 60 ? 'Good' : 'Poor'}
                    color={getHealthColor(networkHealth)}
                    size="small"
                    sx={{ ml: 1 }}
                  />
                </Box>
                <LinearProgress
                  variant="determinate"
                  value={networkHealth}
                  color={getHealthColor(networkHealth)}
                />
              </Box>
              
              {/* Canvas */}
              <Box
                sx={{
                  border: '1px solid #ddd',
                  borderRadius: 1,
                  overflow: 'hidden'
                }}
              >
                <canvas
                  ref={canvasRef}
                  width={800}
                  height={400}
                  style={{ width: '100%', height: 'auto', cursor: 'pointer' }}
                  onClick={handleCanvasClick}
                />
              </Box>
              
              {/* Legend */}
              <Box mt={2} display="flex" gap={2} flexWrap="wrap">
                <Box display="flex" alignItems="center" gap={1}>
                  <Box width={20} height={20} bgcolor="#F44336" borderRadius="50%" />
                  <Typography variant="caption">Admin</Typography>
                </Box>
                <Box display="flex" alignItems="center" gap={1}>
                  <Box width={20} height={20} bgcolor="#FF9800" borderRadius="50%" />
                  <Typography variant="caption">Relay</Typography>
                </Box>
                <Box display="flex" alignItems="center" gap={1}>
                  <Box width={20} height={20} bgcolor="#4CAF50" borderRadius="50%" />
                  <Typography variant="caption">User</Typography>
                </Box>
                <Box display="flex" alignItems="center" gap={1}>
                  <Box width={20} height={3} bgcolor="#2196F3" />
                  <Typography variant="caption">Wi-Fi Direct</Typography>
                </Box>
                <Box display="flex" alignItems="center" gap={1}>
                  <Box width={20} height={3} bgcolor="#9C27B0" />
                  <Typography variant="caption">Bluetooth</Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Network Statistics */}
        <Grid item xs={12} md={4}>
          <Grid container spacing={2}>
            {/* Quick Stats */}
            <Grid item xs={12}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    Network Statistics
                  </Typography>
                  
                  <Grid container spacing={2}>
                    <Grid item xs={6}>
                      <Box textAlign="center">
                        <Typography variant="h4" color="primary">
                          {networkStats.connected_devices || 0}
                        </Typography>
                        <Typography variant="caption" color="textSecondary">
                          Connected Devices
                        </Typography>
                      </Box>
                    </Grid>
                    <Grid item xs={6}>
                      <Box textAlign="center">
                        <Typography variant="h4" color="success.main">
                          {networkStats.messages_sent || 0}
                        </Typography>
                        <Typography variant="caption" color="textSecondary">
                          Messages Sent
                        </Typography>
                      </Box>
                    </Grid>
                    <Grid item xs={6}>
                      <Box textAlign="center">
                        <Typography variant="h4" color="info.main">
                          {networkStats.average_latency || 0}ms
                        </Typography>
                        <Typography variant="caption" color="textSecondary">
                          Avg Latency
                        </Typography>
                      </Box>
                    </Grid>
                    <Grid item xs={6}>
                      <Box textAlign="center">
                        <Typography variant="h4" color="warning.main">
                          {Math.round((networkStats.coverage_area || 0) / 1000)}km²
                        </Typography>
                        <Typography variant="caption" color="textSecondary">
                          Coverage Area
                        </Typography>
                      </Box>
                    </Grid>
                  </Grid>
                  
                  <Divider sx={{ my: 2 }} />
                  
                  <List dense>
                    <ListItem>
                      <ListItemText
                        primary="Network Uptime"
                        secondary={formatUptime(networkStats.network_uptime || 0)}
                      />
                    </ListItem>
                    <ListItem>
                      <ListItemText
                        primary="Topology Type"
                        secondary={networkStats.topology_type || 'Unknown'}
                      />
                    </ListItem>
                    <ListItem>
                      <ListItemText
                        primary="Emergency Alerts"
                        secondary={`${networkStats.emergency_alerts_sent || 0} sent`}
                      />
                    </ListItem>
                  </List>
                </CardContent>
              </Card>
            </Grid>

            {/* Selected Node Details */}
            {selectedNode && (
              <Grid item xs={12}>
                <Card>
                  <CardContent>
                    <Box display="flex" alignItems="center" mb={2}>
                      {getNodeTypeIcon(selectedNode.type)}
                      <Typography variant="h6" sx={{ ml: 1 }}>
                        {selectedNode.name}
                      </Typography>
                    </Box>
                    
                    <List dense>
                      <ListItem>
                        <ListItemIcon>
                          <Chip
                            label={selectedNode.status}
                            color={selectedNode.status === 'online' ? 'success' : 'error'}
                            size="small"
                          />
                        </ListItemIcon>
                        <ListItemText primary="Status" />
                      </ListItem>
                      
                      <ListItem>
                        <ListItemIcon>
                          <BatteryIcon />
                        </ListItemIcon>
                        <ListItemText
                          primary="Battery"
                          secondary={`${selectedNode.batteryLevel}%`}
                        />
                      </ListItem>
                      
                      <ListItem>
                        <ListItemIcon>
                          <SignalIcon />
                        </ListItemIcon>
                        <ListItemText
                          primary="Signal Strength"
                          secondary={`${selectedNode.signalStrength}%`}
                        />
                      </ListItem>
                      
                      <ListItem>
                        <ListItemIcon>
                          <NetworkIcon />
                        </ListItemIcon>
                        <ListItemText
                          primary="Connections"
                          secondary={`${selectedNode.connections} active`}
                        />
                      </ListItem>
                      
                      <ListItem>
                        <ListItemText
                          primary="Last Seen"
                          secondary={selectedNode.lastSeen.toLocaleTimeString()}
                        />
                      </ListItem>
                    </List>
                  </CardContent>
                </Card>
              </Grid>
            )}
          </Grid>
        </Grid>
      </Grid>
    </Box>
  );
}