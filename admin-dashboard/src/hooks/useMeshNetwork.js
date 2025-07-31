import { useState, useEffect, useCallback } from 'react';
import { useQuery, useMutation, useQueryClient } from 'react-query';
import axios from 'axios';
import io from 'socket.io-client';
import toast from 'react-hot-toast';

const API_BASE = process.env.REACT_APP_API_URL || 'http://localhost:3000/api';

/**
 * Custom hook for mesh network management
 */
export function useMeshNetwork() {
  const [socket, setSocket] = useState(null);
  const [isConnected, setIsConnected] = useState(false);
  const [networkStatus, setNetworkStatus] = useState('stopped');
  const queryClient = useQueryClient();

  // Initialize WebSocket connection
  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token) return;

    const socketInstance = io(process.env.REACT_APP_WS_URL || 'http://localhost:3000', {
      auth: { token },
      transports: ['websocket']
    });

    socketInstance.on('connect', () => {
      console.log('Connected to mesh network WebSocket');
      setIsConnected(true);
    });

    socketInstance.on('disconnect', () => {
      console.log('Disconnected from mesh network WebSocket');
      setIsConnected(false);
    });

    // Listen for network updates
    socketInstance.on('network:topology_update', (data) => {
      console.log('Network topology updated:', data);
      queryClient.setQueryData(['network', 'topology'], data);
    });

    socketInstance.on('network:statistics_update', (data) => {
      console.log('Network statistics updated:', data);
      queryClient.setQueryData(['network', 'statistics'], data);
    });

    socketInstance.on('network:alert', (alert) => {
      console.log('Network alert received:', alert);
      
      // Show toast notification
      const severity = alert.severity;
      const message = `Network Alert: ${alert.message}`;
      
      if (severity === 'critical' || severity === 'high') {
        toast.error(message);
      } else if (severity === 'medium') {
        toast(message, { icon: '⚠️' });
      } else {
        toast.success(message);
      }
      
      // Update alerts query
      queryClient.invalidateQueries(['network', 'alerts']);
    });

    socketInstance.on('network:health_update', (health) => {
      console.log('Network health updated:', health);
      queryClient.setQueryData(['network', 'health'], health);
    });

    setSocket(socketInstance);

    return () => {
      socketInstance.disconnect();
    };
  }, [queryClient]);

  // Fetch network topology
  const {
    data: topology,
    isLoading: topologyLoading,
    error: topologyError,
    refetch: refetchTopology
  } = useQuery(
    ['network', 'topology'],
    async () => {
      const response = await axios.get(`${API_BASE}/network/topology`, {
        headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
      });
      return response.data.data;
    },
    {
      refetchInterval: 30000, // Refetch every 30 seconds
      staleTime: 15000, // Consider data stale after 15 seconds
      onError: (error) => {
        console.error('Error fetching network topology:', error);
        toast.error('Failed to fetch network topology');
      }
    }
  );

  // Fetch network statistics
  const {
    data: statistics,
    isLoading: statisticsLoading,
    error: statisticsError,
    refetch: refetchStatistics
  } = useQuery(
    ['network', 'statistics'],
    async () => {
      const response = await axios.get(`${API_BASE}/network/statistics`, {
        headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
      });
      return response.data.data;
    },
    {
      refetchInterval: 30000,
      staleTime: 15000,
      onError: (error) => {
        console.error('Error fetching network statistics:', error);
        toast.error('Failed to fetch network statistics');
      }
    }
  );

  // Fetch network health
  const {
    data: health,
    isLoading: healthLoading,
    error: healthError,
    refetch: refetchHealth
  } = useQuery(
    ['network', 'health'],
    async () => {
      const response = await axios.get(`${API_BASE}/network/health`, {
        headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
      });
      return response.data.data;
    },
    {
      refetchInterval: 60000, // Refetch every minute
      staleTime: 30000,
      onError: (error) => {
        console.error('Error fetching network health:', error);
        toast.error('Failed to fetch network health');
      }
    }
  );

  // Fetch network alerts
  const {
    data: alerts,
    isLoading: alertsLoading,
    error: alertsError,
    refetch: refetchAlerts
  } = useQuery(
    ['network', 'alerts'],
    async () => {
      const response = await axios.get(`${API_BASE}/network/alerts`, {
        headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
      });
      return response.data.data;
    },
    {
      refetchInterval: 60000,
      staleTime: 30000,
      onError: (error) => {
        console.error('Error fetching network alerts:', error);
        toast.error('Failed to fetch network alerts');
      }
    }
  );

  // Network optimization mutation
  const optimizeNetworkMutation = useMutation(
    async () => {
      const response = await axios.post(`${API_BASE}/network/optimize`, {}, {
        headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
      });
      return response.data;
    },
    {
      onSuccess: (data) => {
        toast.success('Network optimization completed successfully');
        console.log('Network optimization result:', data);
        
        // Invalidate and refetch network data
        queryClient.invalidateQueries(['network']);
      },
      onError: (error) => {
        console.error('Network optimization failed:', error);
        toast.error('Network optimization failed');
      }
    }
  );

  // Update network configuration mutation
  const updateConfigMutation = useMutation(
    async (config) => {
      const response = await axios.put(`${API_BASE}/network/config`, config, {
        headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
      });
      return response.data;
    },
    {
      onSuccess: () => {
        toast.success('Network configuration updated successfully');
        queryClient.invalidateQueries(['network']);
      },
      onError: (error) => {
        console.error('Failed to update network configuration:', error);
        toast.error('Failed to update network configuration');
      }
    }
  );

  // Resolve alert mutation
  const resolveAlertMutation = useMutation(
    async ({ alertId, resolution }) => {
      const response = await axios.put(
        `${API_BASE}/network/alerts/${alertId}/resolve`,
        { resolution },
        {
          headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
        }
      );
      return response.data;
    },
    {
      onSuccess: () => {
        toast.success('Alert resolved successfully');
        queryClient.invalidateQueries(['network', 'alerts']);
      },
      onError: (error) => {
        console.error('Failed to resolve alert:', error);
        toast.error('Failed to resolve alert');
      }
    }
  );

  // Helper functions
  const refreshAllData = useCallback(() => {
    refetchTopology();
    refetchStatistics();
    refetchHealth();
    refetchAlerts();
  }, [refetchTopology, refetchStatistics, refetchHealth, refetchAlerts]);

  const optimizeNetwork = useCallback(() => {
    optimizeNetworkMutation.mutate();
  }, [optimizeNetworkMutation]);

  const updateNetworkConfig = useCallback((config) => {
    updateConfigMutation.mutate(config);
  }, [updateConfigMutation]);

  const resolveAlert = useCallback((alertId, resolution) => {
    resolveAlertMutation.mutate({ alertId, resolution });
  }, [resolveAlertMutation]);

  const getVisualizationData = useCallback(() => {
    if (!topology) return null;

    return {
      nodes: topology.nodes?.map(node => ({
        id: node.id,
        name: node.name,
        type: node.type,
        x: Math.random() * 800, // Random positioning for demo
        y: Math.random() * 400,
        status: node.status,
        batteryLevel: node.batteryLevel,
        signalStrength: node.signalStrength,
        connections: topology.connections?.filter(
          conn => conn.from === node.id || conn.to === node.id
        ).length || 0,
        lastSeen: new Date(node.lastSeen)
      })) || [],
      edges: topology.connections?.map(conn => ({
        from: conn.from,
        to: conn.to,
        type: conn.type,
        strength: conn.strength,
        active: conn.active
      })) || [],
      metadata: topology.metadata || {}
    };
  }, [topology]);

  const getNetworkSummary = useCallback(() => {
    if (!statistics || !health) return null;

    return {
      totalNodes: statistics.totalNodes || 0,
      onlineNodes: statistics.onlineNodes || 0,
      networkHealth: health.overallHealth || 0,
      averageLatency: statistics.averageLatency || 0,
      messagesSent: statistics.messagesSent || 0,
      activeAlerts: alerts?.summary?.active || 0,
      uptime: statistics.networkUptime || 0,
      coverageArea: statistics.coverageArea || 0
    };
  }, [statistics, health, alerts]);

  return {
    // Connection status
    isConnected,
    networkStatus,
    
    // Data
    topology,
    statistics,
    health,
    alerts,
    
    // Loading states
    isLoading: topologyLoading || statisticsLoading || healthLoading || alertsLoading,
    topologyLoading,
    statisticsLoading,
    healthLoading,
    alertsLoading,
    
    // Errors
    error: topologyError || statisticsError || healthError || alertsError,
    topologyError,
    statisticsError,
    healthError,
    alertsError,
    
    // Mutation states
    isOptimizing: optimizeNetworkMutation.isLoading,
    isUpdatingConfig: updateConfigMutation.isLoading,
    isResolvingAlert: resolveAlertMutation.isLoading,
    
    // Actions
    refreshAllData,
    optimizeNetwork,
    updateNetworkConfig,
    resolveAlert,
    
    // Helper functions
    getVisualizationData,
    getNetworkSummary,
    
    // WebSocket
    socket
  };
}

export default useMeshNetwork;