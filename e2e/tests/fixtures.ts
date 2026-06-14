import { test as baseTest, expect } from '@playwright/test';

export { expect } from '@playwright/test';

export const test = baseTest.extend({
  page: async ({ page }, use) => {
    await page.goto('/xxl-job-admin/toLogin');
    await page.fill('input[name="userName"]', 'admin');
    await page.fill('input[name="password"]', '123456');
    await page.click('button[type="submit"]');
    await expect(page.locator('.sidebar-menu')).toBeVisible();
    await use(page);
  },
});
