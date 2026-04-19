const { defineConfig, devices } = require('@playwright/test');

const isWindows = process.platform === 'win32';
const backendCommand = isWindows
  ? 'cd backend && .\\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=e2e"'
  : 'cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=e2e';

module.exports = defineConfig({
  testDir: './frontend/tests/e2e',
  timeout: 120_000,
  expect: { timeout: 15_000 },
  fullyParallel: false,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: 'http://127.0.0.1:5500',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: [
    {
      command: backendCommand,
      url: 'http://127.0.0.1:8080/api/health',
      reuseExistingServer: !process.env.CI,
      timeout: 120_000,
    },
    {
      command: 'node frontend/tests/e2e/static-server.js',
      url: 'http://127.0.0.1:5500/login.html',
      reuseExistingServer: !process.env.CI,
      timeout: 30_000,
    },
  ],
});
