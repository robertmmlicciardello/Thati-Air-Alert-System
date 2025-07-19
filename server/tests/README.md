# Thati Air Alert - Server Testing Suite

## 🧪 Overview

This comprehensive testing suite ensures the reliability, security, and performance of the Thati Air Alert server infrastructure. The test suite includes unit tests, integration tests, security tests, and performance benchmarks.

## 📁 Test Structure

```
tests/
├── unit/                    # Unit tests for individual components
│   ├── alertProcessor.test.js
│   ├── authService.test.js
│   ├── userService.test.js
│   └── deviceService.test.js
├── integration/             # End-to-end integration tests
│   ├── alerts.integration.test.js
│   ├── auth.integration.test.js
│   └── websocket.integration.test.js
├── security/                # Security and vulnerability tests
│   └── security.test.js
├── performance/             # Load and performance tests
│   └── load.test.js
├── helpers/                 # Test utilities and helpers
│   └── testHelpers.js
├── setup.js                 # Global test setup and configuration
└── README.md               # This file
```

## 🚀 Running Tests

### Prerequisites

1. **Node.js** (v16 or higher)
2. **PostgreSQL** database for testing
3. **Redis** server for caching tests
4. **Environment variables** configured

### Environment Setup

Create a `.env.test` file in the server directory:

```env
NODE_ENV=test
JWT_SECRET=test-jwt-secret-key-for-testing
DB_HOST=localhost
DB_PORT=5432
DB_NAME=thati_alert_test
DB_USER=test_user
DB_PASSWORD=test_password
REDIS_URL=redis://localhost:6379/1
```

### Install Dependencies

```bash
npm install
```

### Run All Tests

```bash
npm test
```

### Run Specific Test Suites

```bash
# Unit tests only
npm run test:unit

# Integration tests only
npm run test:integration

# Security tests only
npm run test:security

# Performance tests only
npm run test:performance

# Watch mode for development
npm run test:watch

# Test coverage report
npm run test:coverage
```

### Run Individual Test Files

```bash
# Run specific test file
npx mocha tests/unit/alertProcessor.test.js --require tests/setup.js

# Run with specific grep pattern
npx mocha tests/unit/*.test.js --require tests/setup.js --grep "should send alert"
```

## 📊 Test Categories

### Unit Tests

Test individual functions and modules in isolation:

- **Alert Processor**: Alert creation, validation, and processing logic
- **Authentication Service**: Login, token generation, and validation
- **User Service**: User management and profile operations
- **Device Service**: Device registration and management
- **Database Operations**: CRUD operations and data validation
- **Utility Functions**: Encryption, validation, and helper functions

### Integration Tests

Test complete workflows and API endpoints:

- **Alert API**: End-to-end alert sending and retrieval
- **Authentication Flow**: Login, registration, and token refresh
- **WebSocket Communication**: Real-time alert broadcasting
- **Database Integration**: Multi-table operations and transactions
- **External Service Integration**: Firebase, Twilio, email services

### Security Tests

Comprehensive security vulnerability testing:

- **Authentication Security**: Token validation, session management
- **Authorization**: Role-based access control
- **Input Validation**: SQL injection, XSS prevention
- **Rate Limiting**: API abuse prevention
- **Data Encryption**: Sensitive data protection
- **CORS Configuration**: Cross-origin request security
- **Password Security**: Strong password enforcement

### Performance Tests

Load testing and performance benchmarking:

- **API Load Testing**: Concurrent request handling
- **WebSocket Performance**: Multiple connection management
- **Database Performance**: Query optimization and concurrent access
- **Memory Usage**: Memory leak detection and optimization
- **Response Time Analysis**: Latency distribution and optimization

## 🛠️ Test Utilities

### Test Helpers

The `testHelpers.js` file provides utilities for:

- **Authentication**: Token generation, password hashing
- **Data Generation**: Mock users, alerts, and devices
- **Database Operations**: Test data insertion and cleanup
- **HTTP Requests**: Authenticated request helpers
- **WebSocket Testing**: Connection and message utilities
- **Time Manipulation**: Date/time testing utilities
- **Validation**: Data format and constraint validation

### Global Test Setup

The `setup.js` file handles:

- **Environment Configuration**: Test-specific environment variables
- **Database Setup**: Test database creation and seeding
- **Redis Configuration**: Test cache setup
- **Global Hooks**: Before/after test suite execution
- **Test Data Management**: Automatic cleanup and seeding

## 📈 Coverage Reports

Generate detailed test coverage reports:

```bash
npm run test:coverage
```

Coverage reports include:

- **Line Coverage**: Percentage of code lines executed
- **Function Coverage**: Percentage of functions called
- **Branch Coverage**: Percentage of code branches taken
- **Statement Coverage**: Percentage of statements executed

## 🐛 Debugging Tests

### Debug Individual Tests

```bash
# Run with debug output
DEBUG=* npm run test:unit

# Run specific test with detailed output
npx mocha tests/unit/alertProcessor.test.js --require tests/setup.js --reporter tap
```

### Common Issues

1. **Database Connection**: Ensure test database is running and accessible
2. **Redis Connection**: Verify Redis server is available for caching tests
3. **Environment Variables**: Check all required environment variables are set
4. **Port Conflicts**: Ensure test ports are not in use by other services
5. **Async Operations**: Use proper async/await patterns in tests

## 🔧 Configuration

### Mocha Configuration

The `.mocharc.json` file configures:

- **Test Timeout**: Default timeout for test execution
- **Reporter**: Test output format (spec, json, tap, etc.)
- **Setup Files**: Global setup and teardown scripts
- **Test Patterns**: File patterns for test discovery

### ESLint Configuration

Code quality and style enforcement:

```bash
# Check code style
npm run lint

# Fix auto-fixable issues
npm run lint:fix
```

## 📝 Writing Tests

### Test Structure

Follow this structure for new tests:

```javascript
const { expect } = require('chai');
const sinon = require('sinon');

describe('Component Name', () => {
    let componentInstance;
    let mockDependency;
    
    beforeEach(() => {
        // Setup before each test
        mockDependency = sinon.stub();
        componentInstance = new Component(mockDependency);
    });
    
    afterEach(() => {
        // Cleanup after each test
        sinon.restore();
    });
    
    describe('method name', () => {
        it('should perform expected behavior', async () => {
            // Arrange
            const input = { test: 'data' };
            mockDependency.returns('expected result');
            
            // Act
            const result = await componentInstance.method(input);
            
            // Assert
            expect(result).to.equal('expected result');
            expect(mockDependency.calledOnce).to.be.true;
        });
    });
});
```

### Best Practices

1. **Descriptive Test Names**: Use clear, descriptive test names
2. **Arrange-Act-Assert**: Structure tests with clear sections
3. **Mock External Dependencies**: Isolate units under test
4. **Test Edge Cases**: Include boundary conditions and error cases
5. **Async Testing**: Properly handle promises and async operations
6. **Cleanup**: Always clean up test data and mocks
7. **Independent Tests**: Each test should be independent and repeatable

## 🚨 Continuous Integration

### GitHub Actions

Example workflow for automated testing:

```yaml
name: Test Suite
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:13
        env:
          POSTGRES_PASSWORD: test_password
          POSTGRES_DB: thati_alert_test
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
      redis:
        image: redis:6
        options: >-
          --health-cmd "redis-cli ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
    steps:
      - uses: actions/checkout@v2
      - uses: actions/setup-node@v2
        with:
          node-version: '16'
      - run: npm install
      - run: npm test
      - run: npm run test:coverage
```

## 📞 Support

For questions about the testing suite:

- **Documentation**: Check this README and inline code comments
- **Issues**: Create GitHub issues for bugs or feature requests
- **Team Chat**: Contact the development team for urgent issues

---

*Last updated: July 19, 2025*