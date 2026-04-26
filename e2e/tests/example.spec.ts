import { test, expect } from '@playwright/test';

test('has title and can login', async ({ page }) => {
  await page.goto('/xxl-job-admin/toLogin');
  
  // Expect a title "to contain" a substring.
  await expect(page).toHaveTitle(/任务调度中心/);

  // Fill login
  await page.fill('input[name="userName"]', 'admin');
  await page.fill('input[name="password"]', '123456');
  
  // Click login button
  await page.click('button[type="submit"]');

  // Verify successful login by checking side menu
  await expect(page.locator('.sidebar-menu')).toBeVisible({ timeout: 10000 });
});
