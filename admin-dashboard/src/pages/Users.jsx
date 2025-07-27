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
  Avatar,
  Alert,
  Paper
} from '@mui/material';
import {
  Edit as EditIcon,
  Delete as DeleteIcon,
  PersonAdd as AddUserIcon,
  Refresh as RefreshIcon
} from '@mui/icons-material';

// Mock users data for offline mode
const mockUsers = [
  {
    id: 1,
    name: 'Admin User',
    email: 'admin@thati.gov.mm',
    role: 'admin',
    status: 'active',
    lastLogin: new Date(Date.now() - 1800000),
    location: 'Yangon',
    deviceCount: 5
  },
  {
    id: 2,
    name: 'Regional Admin',
    email: 'regional@thati.gov.mm',
    role: 'regional',
    status: 'active',
    lastLogin: new Date(Date.now() - 3600000),
    location: 'Mandalay',
    deviceCount: 3
  },
  {
    id: 3,
    name: 'Local Operator',
    email: 'operator@thati.gov.mm',
    role: 'operator',
    status: 'active',
    lastLogin: new Date(Date.now() - 7200000),
    location: 'Naypyidaw',
    deviceCount: 2
  },
  {
    id: 4,
    name: 'Field Officer',
    email: 'field@thati.gov.mm',
    role: 'field',
    status: 'offline',
    lastLogin: new Date(Date.now() - 86400000),
    location: 'Bagan',
    deviceCount: 1
  }
];

export default function Users() {
  const [users, setUsers] = useState(mockUsers);

  const getRoleColor = (role) => {
    switch (role) {
      case 'admin': return 'error';
      case 'regional': return 'warning';
      case 'operator': return 'info';
      case 'field': return 'success';
      default: return 'default';
    }
  };

  const getStatusColor = (status) => {
    return status === 'active' ? 'success' : 'default';
  };

  const handleDeleteUser = (userId) => {
    setUsers(prev => prev.filter(user => user.id !== userId));
  };

  const getInitials = (name) => {
    return name.split(' ').map(n => n[0]).join('').toUpperCase();
  };

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">
          User Management
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
            startIcon={<AddUserIcon />}
          >
            Add User
          </Button>
        </Box>
      </Box>

      <Alert severity="info" sx={{ mb: 3 }}>
        Offline mode - User data is cached locally. Changes will sync when connection is restored.
      </Alert>

      {/* Stats Cards */}
      <Box display="flex" gap={2} mb={3}>
        <Card sx={{ minWidth: 200 }}>
          <CardContent>
            <Typography color="textSecondary" gutterBottom>
              Total Users
            </Typography>
            <Typography variant="h4">
              {users.length}
            </Typography>
          </CardContent>
        </Card>
        
        <Card sx={{ minWidth: 200 }}>
          <CardContent>
            <Typography color="textSecondary" gutterBottom>
              Active Users
            </Typography>
            <Typography variant="h4" color="success.main">
              {users.filter(u => u.status === 'active').length}
            </Typography>
          </CardContent>
        </Card>

        <Card sx={{ minWidth: 200 }}>
          <CardContent>
            <Typography color="textSecondary" gutterBottom>
              Admin Users
            </Typography>
            <Typography variant="h4" color="error.main">
              {users.filter(u => u.role === 'admin').length}
            </Typography>
          </CardContent>
        </Card>
      </Box>

      <Card>
        <CardContent>
          <TableContainer component={Paper}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>User</TableCell>
                  <TableCell>Role</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Location</TableCell>
                  <TableCell>Devices</TableCell>
                  <TableCell>Last Login</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {users.map((user) => (
                  <TableRow key={user.id}>
                    <TableCell>
                      <Box display="flex" alignItems="center">
                        <Avatar sx={{ mr: 2, bgcolor: 'primary.main' }}>
                          {getInitials(user.name)}
                        </Avatar>
                        <Box>
                          <Typography variant="body1" fontWeight="medium">
                            {user.name}
                          </Typography>
                          <Typography variant="body2" color="textSecondary">
                            {user.email}
                          </Typography>
                        </Box>
                      </Box>
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={user.role}
                        color={getRoleColor(user.role)}
                        size="small"
                      />
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={user.status}
                        color={getStatusColor(user.status)}
                        size="small"
                      />
                    </TableCell>
                    <TableCell>{user.location}</TableCell>
                    <TableCell>
                      <Chip
                        label={user.deviceCount}
                        variant="outlined"
                        size="small"
                      />
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2">
                        {user.lastLogin.toLocaleString()}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <IconButton size="small" color="primary">
                        <EditIcon />
                      </IconButton>
                      <IconButton 
                        size="small" 
                        color="error"
                        onClick={() => handleDeleteUser(user.id)}
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
    </Box>
  );
}