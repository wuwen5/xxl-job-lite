import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  timeout: 10000,   
  expect: {
    timeout: 3000, 
  },
  testDir: './tests',
  fullyParallel: true,
  retries: 0,
  workers: 1,
  reporter: [
      ['html', { open: 'never' }],
      ['json', { outputFile: 'test-results/results.json' }],
      ['junit', { outputFile: 'test-results/junit.xml' }]
  ],
  use: {
    baseURL: 'http://xxl-job-admin:8080',
    trace: 'retain-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});
