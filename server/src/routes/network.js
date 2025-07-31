const express = require('express');
const router = express.Router();
const { authenticateToken, requireRole } = require('../middleware/auth');
const logger = require('../utils/logger');

// Mock mesh network data - အစစ်မှာ database ကနေ လာမယ်
let meshNetworkData = {
  nodes: [
    {
      id: 'admin-001',
      name: 'Admin Device',
      type: 'admin',
      status: 'online',
      location: { lat: 16.8661, lng: 96.1951, address: 'Yangon Central' },
      batteryLevel: 85,
      signalStrength: 90,
      connections: 3,
      lastSeen: new Date(),
      capabilities: ['broadcast', 'relay', 'admin'],
      networkRole: 'coordinator'
    },
    {
      id: 'relay-001',
      name: 'Relay Device 1',
      type: 'relay',
      status: 'online',
      location: { lat: 16.8551, lng: 96.1851, address: 'Yangon North' },
      batteryLevel: 70,
      signalStrength: 75,
      connections: 2,
      lastSeen: new Date(),
      capabilities: ['relay', 'forward'],
      networkRole: 'relay'
    },
    {
      id: 'relay-002',
      name: 'Relay Device 2',
      type: 'relay',
      status: 'online',
      location: { lat: 16.8771, lng: 96.2051, address: 'Yangon East' },
      batteryLevel: 65,
      signalStrength: 80,
      connections: 2,
      lastSeen: new Date(),
      capabilities: ['relay', 'forward'],
      networkRole: 'relay'
    },
    {
      id: 'user-001',
      name: 'User Device 1',
      type: 'user',
      status: 'online',
      location: { lat: 16.8461, lng: 96.1751, address: 'Yangon West' },
      batteryLevel: 60,
      signalStrength: 65,
      connections: 1,
      lastSeen: new Date(),
      capabilities: ['receive'],
      networkRole: 'endpoint'
    },
    {
      id: 'user-002',
      name: 'User Device 2',
      type: 'user',
      status: 'online',
      location: { lat: 16.8761, lng: 96.1951, address: 'Yangon South' },
      batteryLevel: 80,
      signalStrength: 70,
      connections: 1,
      lastSeen: new Date(),
      capabilities: ['receive'],
      networkRole: 'endpoint'
    },
    {
      id: 'user-003',
      name: 'User Device 3',
      type: 'user',
      status: 'offline',
      location: { lat: 16.8861, lng: 96.2151, address: 'Yangon Southeast' },
      batteryLevel: 20,
      signalStrength: 30,
      connections: 0,
      lastSeen: new Date(Date.now() - 300000),
      capabilities: ['receive'],
      networkRole: 'endpoint'
    }
  ],
  connections: [
    { 
      from: 'admin-001', 
      to: 'relay-001', 
      type: 'wifi_direct', 
      strength: 85, 
      active: true,
      latency: 45,
      bandwidth: 150,
      established: new Date(Date.now() - 3600000)
    },
    { 
      from: 'admin-001', 
      to: 'relay-002', 
      type: 'wifi_direct', 
      strength: 80, 
      active: true,
      latency: 50,
      bandwidth: 140,
      established: new Date(Date.now() - 3500000)
    },
    { 
      from: 'admin-001', 
      to: 'user-002', 
      type: 'bluetooth', 
      strength: 70, 
      active: true,
      latency: 85,
      bandwidth: 50,
      established: new Date(Date.now() - 2400000)
    },
    { 
      from: 'relay-001', 
      to: 'user-001', 
      type: 'bluetooth', 
      strength: 70, 
      active: true,
      latency: 90,
      bandwidth: 45,
      established: new Date(Date.now() - 1800000)
    },
    { 
      from: 'relay-002', 
      to: 'user-003', 
      type: 'wifi_direct', 
      strength: 30, 
      active: false,
      latency: 200,
      bandwidth: 0,
      established: new Date(Date.now() - 600000)
    }
  ],
  statistics: {
    totalNodes: 6,
    onlineNodes: 5,
    offlineNodes: 1,
    activeConnections: 4,
    inactiveConnections: 1,
    networkHealth: 85,
    averageLatency: 75,
    totalBandwidth: 385,
    messagesSent: 1247,
    messagesReceived: 1198,
    messagesForwarded: 89,
    emergencyAlerts: 3,
    networkUptime: 7200000, // 2 hours in milliseconds
    coverageArea: 50000, // in square meters
    topologyType: 'hybrid'
  },
  alerts: [
    {
      id: 'alert-001',
      type: 'connectivity',
      severity: 'medium',
      message: 'Device user-003 has been offline for 5 minutes',
      timestamp: new Date(Date.now() - 300000),
      resolved: false,
      affectedNodes: ['user-003']
    },
    {
      id: 'alert-002',
      type: 'performance',
      severity: 'low',
      message: 'High latency detected on relay-002 connection',
      timestamp: new Date(Date.now() - 180000),
      resolved: false,
      affectedNodes: ['relay-002', 'user-003']
    }
  ]
};

// Get mesh network topology
router.get('/topology', authenticateToken, async (req, res) => {
  try {
    logger.info('Fetching mesh network topology');
    
    // Simulate real-time updates
    updateNetworkData();
    
    const topology = {
      nodes: meshNetworkData.nodes,
      connections: meshNetworkData.connections,
      metadata: {
        topology_type: meshNetworkData.statistics.topologyType,
        total_nodes: meshNetworkData.statistics.totalNodes,
        active_connections: meshNetworkData.statistics.activeConnections,
        network_diameter: calculateNetworkDiameter(),
        last_updated: new Date()
      }
    };
    
    res.json({
      success: true,
      data: topology
    });
    
  } catch (error) {
    logger.error('Error fetching network topology:', error);
    res.status(500).json({
      success: false,
      message: 'Failed to fetch network topology'
    });
  }
});

// Get network statistics
router.get('/statistics', authenticateToken, async (req, res) => {
  try {
    logger.info('Fetching network statistics');
    
    updateNetworkData();
    
    const stats = {
      ...meshNetworkData.statistics,
      performance: {
        messageDeliveryRate: calculateMessageDeliveryRate(),
        networkEfficiency: calculateNetworkEfficiency(),
        averageBatteryLevel: calculateAverageBatteryLevel(),
        signalQuality: calculateAverageSignalStrength()
      },
      trends: {
        healthTrend: generateTrend('health'),
        latencyTrend: generateTrend('latency'),
        connectivityTrend: generateTrend('connectivity')
      },
      lastUpdated: new Date()
    };
    
    res.json({
      success: true,
      data: stats
    });
    
  } catch (error) {
    logger.error('Error fetching network statistics:', error);
    res.status(500).json({
      success: false,
      message: 'Failed to fetch network statistics'
    });
  }
});

// Get network health report
router.get('/health', authenticateToken, async (req, res) => {
  try {
    logger.info('Generating network health report');
    
    updateNetworkData();
    
    const healthReport = {
      overallHealth: meshNetworkData.statistics.networkHealth,
      connectedNodes: meshNetworkData.statistics.onlineNodes,
      averageLatency: meshNetworkData.statistics.averageLatency,
      messageDeliveryRate: calculateMessageDeliveryRate(),
      networkCoverage: meshNetworkData.statistics.coverageArea,
      criticalIssues: identifyCriticalIssues(),
      recommendations: generateRecommendations(),
      performanceMetrics: {
        throughput: meshNetworkData.statistics.totalBandwidth,
        reliability: calculateNetworkReliability(),
        efficiency: calculateNetworkEfficiency(),
        scalability: calculateScalabilityScore()
      },
      timestamp: new Date()
    };
    
    res.json({
      success: true,
      data: healthReport
    });
    
  } catch (error) {
    logger.error('Error generating health report:', error);
    res.status(500).json({
      success: false,
      message: 'Failed to generate health report'
    });
  }
});

// Get network alerts
router.get('/alerts', authenticateToken, async (req, res) => {
  try {
    const { severity, resolved, limit = 50 } = req.query;
    
    logger.info('Fetching network alerts', { severity, resolved, limit });
    
    let alerts = [...meshNetworkData.alerts];
    
    // Filter by severity
    if (severity) {
      alerts = alerts.filter(alert => alert.severity === severity);
    }
    
    // Filter by resolved status
    if (resolved !== undefined) {
      const isResolved = resolved === 'true';
      alerts = alerts.filter(alert => alert.resolved === isResolved);
    }
    
    // Sort by timestamp (newest first)
    alerts.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
    
    // Limit results
    alerts = alerts.slice(0, parseInt(limit));
    
    res.json({
      success: true,
      data: {
        alerts,
        summary: {
          total: meshNetworkData.alerts.length,
          active: meshNetworkData.alerts.filter(a => !a.resolved).length,
          resolved: meshNetworkData.alerts.filter(a => a.resolved).length,
          critical: meshNetworkData.alerts.filter(a => a.severity === 'critical').length,
          high: meshNetworkData.alerts.filter(a => a.severity === 'high').length,
          medium: meshNetworkData.alerts.filter(a => a.severity === 'medium').length,
          low: meshNetworkData.alerts.filter(a => a.severity === 'low').length
        }
      }
    });
    
  } catch (error) {
    logger.error('Error fetching network alerts:', error);
    res.status(500).json({
      success: false,
      message: 'Failed to fetch network alerts'
    });
  }
});

// Optimize network topology
router.post('/optimize', authenticateToken, requireRole(['admin', 'regional_admin']), async (req, res) => {
  try {
    logger.info('Starting network optimization');
    
    // Simulate optimization process
    const optimizationResult = await performNetworkOptimization();
    
    // Update network statistics
    meshNetworkData.statistics.networkHealth = Math.min(100, meshNetworkData.statistics.networkHealth + optimizationResult.improvement);
    meshNetworkData.statistics.averageLatency = Math.max(30, meshNetworkData.statistics.averageLatency - optimizationResult.latencyReduction);
    
    // Add optimization alert
    meshNetworkData.alerts.push({
      id: `alert-${Date.now()}`,
      type: 'optimization',
      severity: 'low',
      message: `Network optimization completed. Health improved by ${optimizationResult.improvement}%`,
      timestamp: new Date(),
      resolved: false,
      affectedNodes: optimizationResult.affectedNodes
    });
    
    res.json({
      success: true,
      data: {
        message: 'Network optimization completed successfully',
        result: optimizationResult,
        newHealth: meshNetworkData.statistics.networkHealth,
        newLatency: meshNetworkData.statistics.averageLatency
      }
    });
    
  } catch (error) {
    logger.error('Error optimizing network:', error);
    res.status(500).json({
      success: false,
      message: 'Failed to optimize network'
    });
  }
});

// Update network configuration
router.put('/config', authenticateToken, requireRole(['admin']), async (req, res) => {
  try {
    const { discoveryInterval, heartbeatInterval, maxHops, enableWifiDirect, enableBluetooth, autoOptimize } = req.body;
    
    logger.info('Updating network configuration', req.body);
    
    // Validate configuration
    if (discoveryInterval && (discoveryInterval < 10 || discoveryInterval > 300)) {
      return res.status(400).json({
        success: false,
        message: 'Discovery interval must be between 10 and 300 seconds'
      });
    }
    
    if (heartbeatInterval && (heartbeatInterval < 10 || heartbeatInterval > 300)) {
      return res.status(400).json({
        success: false,
        message: 'Heartbeat interval must be between 10 and 300 seconds'
      });
    }
    
    if (maxHops && (maxHops < 1 || maxHops > 10)) {
      return res.status(400).json({
        success: false,
        message: 'Max hops must be between 1 and 10'
      });
    }
    
    // Update configuration (in real app, save to database)
    const config = {
      discoveryInterval: discoveryInterval || 30,
      heartbeatInterval: heartbeatInterval || 30,
      maxHops: maxHops || 5,
      enableWifiDirect: enableWifiDirect !== undefined ? enableWifiDirect : true,
      enableBluetooth: enableBluetooth !== undefined ? enableBluetooth : true,
      autoOptimize: autoOptimize !== undefined ? autoOptimize : true,
      updatedAt: new Date()
    };
    
    res.json({
      success: true,
      data: {
        message: 'Network configuration updated successfully',
        config
      }
    });
    
  } catch (error) {
    logger.error('Error updating network configuration:', error);
    res.status(500).json({
      success: false,
      message: 'Failed to update network configuration'
    });
  }
});

// Resolve network alert
router.put('/alerts/:alertId/resolve', authenticateToken, requireRole(['admin', 'regional_admin']), async (req, res) => {
  try {
    const { alertId } = req.params;
    const { resolution } = req.body;
    
    logger.info(`Resolving network alert: ${alertId}`);
    
    const alert = meshNetworkData.alerts.find(a => a.id === alertId);
    if (!alert) {
      return res.status(404).json({
        success: false,
        message: 'Alert not found'
      });
    }
    
    alert.resolved = true;
    alert.resolvedAt = new Date();
    alert.resolution = resolution || 'Manually resolved';
    
    res.json({
      success: true,
      data: {
        message: 'Alert resolved successfully',
        alert
      }
    });
    
  } catch (error) {
    logger.error('Error resolving alert:', error);
    res.status(500).json({
      success: false,
      message: 'Failed to resolve alert'
    });
  }
});

// Helper functions
function updateNetworkData() {
  // Simulate real-time data updates
  meshNetworkData.nodes.forEach(node => {
    if (node.status === 'online') {
      // Simulate battery drain
      node.batteryLevel = Math.max(10, node.batteryLevel - Math.random() * 0.5);
      
      // Simulate signal fluctuation
      node.signalStrength = Math.max(30, Math.min(100, node.signalStrength + (Math.random() - 0.5) * 5));
      
      // Update last seen
      node.lastSeen = new Date();
    }
  });
  
  // Update statistics
  meshNetworkData.statistics.networkUptime += 30000; // Add 30 seconds
  meshNetworkData.statistics.messagesSent += Math.floor(Math.random() * 3);
  meshNetworkData.statistics.messagesReceived += Math.floor(Math.random() * 3);
}

function calculateNetworkDiameter() {
  // Simple calculation - in real scenario, use graph algorithms
  return 3;
}

function calculateMessageDeliveryRate() {
  const sent = meshNetworkData.statistics.messagesSent;
  const received = meshNetworkData.statistics.messagesReceived;
  return sent > 0 ? (received / sent) : 1.0;
}

function calculateNetworkEfficiency() {
  const health = meshNetworkData.statistics.networkHealth;
  const deliveryRate = calculateMessageDeliveryRate();
  const latency = meshNetworkData.statistics.averageLatency;
  
  // Simple efficiency calculation
  return Math.round((health * 0.4 + deliveryRate * 100 * 0.4 + (200 - latency) / 2 * 0.2));
}

function calculateAverageBatteryLevel() {
  const onlineNodes = meshNetworkData.nodes.filter(n => n.status === 'online');
  const totalBattery = onlineNodes.reduce((sum, node) => sum + node.batteryLevel, 0);
  return onlineNodes.length > 0 ? Math.round(totalBattery / onlineNodes.length) : 0;
}

function calculateAverageSignalStrength() {
  const onlineNodes = meshNetworkData.nodes.filter(n => n.status === 'online');
  const totalSignal = onlineNodes.reduce((sum, node) => sum + node.signalStrength, 0);
  return onlineNodes.length > 0 ? Math.round(totalSignal / onlineNodes.length) : 0;
}

function calculateNetworkReliability() {
  const uptime = meshNetworkData.statistics.networkUptime;
  const totalTime = uptime + 300000; // Assume 5 minutes downtime
  return Math.round((uptime / totalTime) * 100);
}

function calculateScalabilityScore() {
  const currentNodes = meshNetworkData.statistics.totalNodes;
  const maxNodes = 50; // Theoretical maximum
  return Math.round((1 - currentNodes / maxNodes) * 100);
}

function generateTrend(type) {
  // Mock trend generation
  return Math.random() * 10 - 5; // -5 to +5
}

function identifyCriticalIssues() {
  const issues = [];
  
  if (meshNetworkData.statistics.onlineNodes < 3) {
    issues.push('Low device connectivity');
  }
  
  if (meshNetworkData.statistics.networkHealth < 50) {
    issues.push('Poor network health');
  }
  
  if (meshNetworkData.statistics.averageLatency > 150) {
    issues.push('High network latency');
  }
  
  const avgBattery = calculateAverageBatteryLevel();
  if (avgBattery < 30) {
    issues.push('Low battery levels across network');
  }
  
  return issues;
}

function generateRecommendations() {
  const recommendations = [];
  
  if (meshNetworkData.statistics.onlineNodes < 5) {
    recommendations.push('Add more relay devices to improve coverage');
  }
  
  if (meshNetworkData.statistics.averageLatency > 100) {
    recommendations.push('Optimize connection paths to reduce latency');
  }
  
  if (meshNetworkData.statistics.networkHealth < 80) {
    recommendations.push('Enable auto-optimization for better performance');
  }
  
  recommendations.push('Regular network health monitoring recommended');
  
  return recommendations;
}

async function performNetworkOptimization() {
  // Simulate optimization process
  await new Promise(resolve => setTimeout(resolve, 2000));
  
  const improvement = Math.floor(Math.random() * 15) + 5; // 5-20% improvement
  const latencyReduction = Math.floor(Math.random() * 20) + 10; // 10-30ms reduction
  const affectedNodes = meshNetworkData.nodes
    .filter(n => n.status === 'online')
    .map(n => n.id)
    .slice(0, 3);
  
  return {
    improvement,
    latencyReduction,
    affectedNodes,
    optimizationTime: 2000,
    changes: [
      'Optimized connection paths',
      'Adjusted transmission power',
      'Updated routing tables'
    ]
  };
}

module.exports = router;