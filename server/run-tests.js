#!/usr/bin/env node

/**
 * Comprehensive Test Runner for Thati Air Alert Server
 * Provides detailed test execution with reporting and analysis
 */

const { spawn } = require('child_process');
const fs = require('fs');
const path = require('path');

// ANSI color codes for console output
const colors = {
    reset: '\x1b[0m',
    bright: '\x1b[1m',
    red: '\x1b[31m',
    green: '\x1b[32m',
    yellow: '\x1b[33m',
    blue: '\x1b[34m',
    magenta: '\x1b[35m',
    cyan: '\x1b[36m'
};

// Test configuration
const testSuites = {
    unit: {
        name: 'Unit Tests',
        command: 'npm',
        args: ['run', 'test:unit'],
        timeout: 30000,
        description: 'Testing individual components and functions'
    },
    integration: {
        name: 'Integration Tests',
        command: 'npm',
        args: ['run', 'test:integration'],
        timeout: 45000,
        description: 'Testing API endpoints and workflows'
    },
    security: {
        name: 'Security Tests',
        command: 'npm',
        args: ['run', 'test:security'],
        timeout: 30000,
        description: 'Testing security vulnerabilities and authentication'
    },
    performance: {
        name: 'Performance Tests',
        command: 'npm',
        args: ['run', 'test:performance'],
        timeout: 60000,
        description: 'Testing load handling and performance benchmarks'
    }
};

class TestRunner {
    constructor() {
        this.results = {};
        this.startTime = Date.now();
        this.totalTests = 0;
        this.passedTests = 0;
        this.failedTests = 0;
    }

    /**
     * Print colored console output
     */
    log(message, color = 'reset') {
        console.log(`${colors[color]}${message}${colors.reset}`);
    }

    /**
     * Print test suite header
     */
    printHeader() {
        this.log('\n' + '='.repeat(80), 'cyan');
        this.log('🧪 THATI AIR ALERT - COMPREHENSIVE TEST SUITE', 'bright');
        this.log('='.repeat(80), 'cyan');
        this.log(`Started at: ${new Date().toISOString()}`, 'blue');
        this.log('');
    }

    /**
     * Print test suite footer with summary
     */
    printSummary() {
        const duration = Date.now() - this.startTime;
        const minutes = Math.floor(duration / 60000);
        const seconds = Math.floor((duration % 60000) / 1000);

        this.log('\n' + '='.repeat(80), 'cyan');
        this.log('📊 TEST EXECUTION SUMMARY', 'bright');
        this.log('='.repeat(80), 'cyan');

        // Overall results
        const overallStatus = this.failedTests === 0 ? 'PASSED' : 'FAILED';
        const statusColor = this.failedTests === 0 ? 'green' : 'red';
        
        this.log(`Overall Status: ${overallStatus}`, statusColor);
        this.log(`Total Execution Time: ${minutes}m ${seconds}s`, 'blue');
        this.log(`Total Test Suites: ${Object.keys(this.results).length}`, 'blue');
        this.log('');

        // Individual suite results
        Object.entries(this.results).forEach(([suite, result]) => {
            const status = result.success ? '✅ PASSED' : '❌ FAILED';
            const color = result.success ? 'green' : 'red';
            
            this.log(`${status} ${testSuites[suite].name}`, color);
            if (result.duration) {
                this.log(`  Duration: ${Math.floor(result.duration / 1000)}s`, 'blue');
            }
            if (result.error) {
                this.log(`  Error: ${result.error}`, 'red');
            }
        });

        this.log('');
        
        // Recommendations
        if (this.failedTests > 0) {
            this.log('🔧 RECOMMENDATIONS:', 'yellow');
            this.log('- Check failed test output above for specific issues', 'yellow');
            this.log('- Ensure database and Redis are running for integration tests', 'yellow');
            this.log('- Verify environment variables are properly configured', 'yellow');
            this.log('- Run individual test suites to isolate issues', 'yellow');
        } else {
            this.log('🎉 All tests passed! Your code is ready for deployment.', 'green');
        }

        this.log('='.repeat(80), 'cyan');
    }

    /**
     * Run a single test suite
     */
    async runTestSuite(suiteName, config) {
        return new Promise((resolve) => {
            this.log(`\n🚀 Running ${config.name}...`, 'bright');
            this.log(`   ${config.description}`, 'blue');
            this.log('   ' + '-'.repeat(60), 'blue');

            const startTime = Date.now();
            const child = spawn(config.command, config.args, {
                stdio: 'pipe',
                shell: true
            });

            let output = '';
            let errorOutput = '';

            child.stdout.on('data', (data) => {
                const text = data.toString();
                output += text;
                process.stdout.write(text);
            });

            child.stderr.on('data', (data) => {
                const text = data.toString();
                errorOutput += text;
                process.stderr.write(text);
            });

            // Set timeout for test suite
            const timeout = setTimeout(() => {
                child.kill('SIGTERM');
                this.log(`\n⏰ Test suite ${config.name} timed out after ${config.timeout}ms`, 'red');
            }, config.timeout);

            child.on('close', (code) => {
                clearTimeout(timeout);
                const duration = Date.now() - startTime;
                const success = code === 0;

                this.results[suiteName] = {
                    success,
                    duration,
                    output,
                    error: success ? null : errorOutput || `Exit code: ${code}`
                };

                if (success) {
                    this.log(`\n✅ ${config.name} completed successfully`, 'green');
                } else {
                    this.log(`\n❌ ${config.name} failed`, 'red');
                    this.failedTests++;
                }

                resolve();
            });

            child.on('error', (error) => {
                clearTimeout(timeout);
                this.log(`\n💥 Error running ${config.name}: ${error.message}`, 'red');
                this.results[suiteName] = {
                    success: false,
                    duration: Date.now() - startTime,
                    error: error.message
                };
                this.failedTests++;
                resolve();
            });
        });
    }

    /**
     * Check prerequisites before running tests
     */
    async checkPrerequisites() {
        this.log('🔍 Checking prerequisites...', 'yellow');

        const checks = [
            {
                name: 'Node.js version',
                check: () => {
                    const version = process.version;
                    const major = parseInt(version.slice(1).split('.')[0]);
                    return major >= 16;
                },
                message: 'Node.js 16+ required'
            },
            {
                name: 'Package dependencies',
                check: () => fs.existsSync(path.join(__dirname, 'node_modules')),
                message: 'Run "npm install" to install dependencies'
            },
            {
                name: 'Test directory',
                check: () => fs.existsSync(path.join(__dirname, 'tests')),
                message: 'Tests directory not found'
            }
        ];

        let allPassed = true;

        for (const check of checks) {
            try {
                const passed = await check.check();
                if (passed) {
                    this.log(`  ✅ ${check.name}`, 'green');
                } else {
                    this.log(`  ❌ ${check.name}: ${check.message}`, 'red');
                    allPassed = false;
                }
            } catch (error) {
                this.log(`  ❌ ${check.name}: ${error.message}`, 'red');
                allPassed = false;
            }
        }

        if (!allPassed) {
            this.log('\n❌ Prerequisites check failed. Please fix the issues above.', 'red');
            process.exit(1);
        }

        this.log('✅ All prerequisites satisfied\n', 'green');
    }

    /**
     * Generate test report
     */
    generateReport() {
        const report = {
            timestamp: new Date().toISOString(),
            duration: Date.now() - this.startTime,
            results: this.results,
            summary: {
                total_suites: Object.keys(this.results).length,
                passed_suites: Object.values(this.results).filter(r => r.success).length,
                failed_suites: Object.values(this.results).filter(r => r.success === false).length,
                overall_success: this.failedTests === 0
            }
        };

        const reportPath = path.join(__dirname, 'test-report.json');
        fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
        this.log(`📄 Test report saved to: ${reportPath}`, 'blue');

        return report;
    }

    /**
     * Main execution method
     */
    async run(suiteFilter = null) {
        this.printHeader();
        
        await this.checkPrerequisites();

        // Determine which suites to run
        const suitesToRun = suiteFilter 
            ? { [suiteFilter]: testSuites[suiteFilter] }
            : testSuites;

        if (suiteFilter && !testSuites[suiteFilter]) {
            this.log(`❌ Unknown test suite: ${suiteFilter}`, 'red');
            this.log(`Available suites: ${Object.keys(testSuites).join(', ')}`, 'blue');
            process.exit(1);
        }

        // Run test suites
        for (const [suiteName, config] of Object.entries(suitesToRun)) {
            await this.runTestSuite(suiteName, config);
        }

        // Generate report and summary
        this.generateReport();
        this.printSummary();

        // Exit with appropriate code
        process.exit(this.failedTests > 0 ? 1 : 0);
    }
}

// Parse command line arguments
const args = process.argv.slice(2);
const suiteFilter = args[0];

// Show help if requested
if (args.includes('--help') || args.includes('-h')) {
    console.log(`
Thati Air Alert Test Runner

Usage:
  node run-tests.js [suite]

Available test suites:
  unit         - Run unit tests only
  integration  - Run integration tests only
  security     - Run security tests only
  performance  - Run performance tests only
  
Examples:
  node run-tests.js              # Run all test suites
  node run-tests.js unit         # Run only unit tests
  node run-tests.js security     # Run only security tests

Options:
  --help, -h   Show this help message
`);
    process.exit(0);
}

// Run the test suite
const runner = new TestRunner();
runner.run(suiteFilter).catch((error) => {
    console.error('💥 Test runner failed:', error);
    process.exit(1);
});