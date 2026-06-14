import { test, expect } from '@playwright/test';

test.describe('Login Page', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/xxl-job-admin/toLogin');
  });

  test('should display login page with correct title', async ({ page }) => {
    await expect(page).toHaveTitle(/任务调度中心/);
  });

  test('should display login form elements', async ({ page }) => {
    await expect(page.locator('input[name="userName"]')).toBeVisible();
    await expect(page.locator('input[name="password"]')).toBeVisible();
    await expect(page.locator('button[type="submit"]')).toBeVisible();
    await expect(page.getByText('记住密码')).toBeVisible();
  });

  test('should login successfully with valid credentials', async ({ page }) => {
    await page.fill('input[name="userName"]', 'admin');
    await page.fill('input[name="password"]', '123456');
    await page.click('button[type="submit"]');

    await expect(page.locator('.sidebar-menu')).toBeVisible({ timeout: 10000 });
    await expect(page).toHaveURL(/.*xxl-job-admin\//);
  });

  test('should show error for invalid credentials', async ({ page }) => {
    await page.fill('input[name="userName"]', 'admin');
    await page.fill('input[name="password"]', 'wrongpassword');
    await page.click('button[type="submit"]');

    await expect(page.getByText('系统提示')).toBeVisible();
    await expect(page.getByText('账号或密码错误')).toBeVisible();
  });

  test('should logout successfully', async ({ page }) => {
    await page.fill('input[name="userName"]', 'admin');
    await page.fill('input[name="password"]', '123456');
    await page.click('button[type="submit"]');
    await expect(page.locator('.sidebar-menu')).toBeVisible();

    await page.getByRole('link', { name: '欢迎 admin' }).click();
    await page.getByRole('link', { name: '注销' }).click();

    await expect(page.getByText('确认注销登录?')).toBeVisible();
    await page.locator('.layui-layer-btn0').click();

    await expect(page).toHaveURL(/.*toLogin/);
  });
});
