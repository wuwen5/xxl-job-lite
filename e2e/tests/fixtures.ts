import { test as baseTest, expect } from '@playwright/test';

export { expect } from '@playwright/test';

export const test = baseTest.extend({
  page: async ({ page }, use) => {
    await page.goto('/login');
    await page.getByPlaceholder('请输入用户名').fill('admin');
    await page.getByPlaceholder('请输入密码').fill('123456');
    await page.getByRole('button', { name: '登 录' }).click();
    await expect(page.getByRole('menubar')).toBeVisible();
    await use(page);
  },
});
