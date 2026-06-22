import { test, expect } from '@playwright/test';

test.describe('Login Page', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
  });

  test('should display login page with correct title', async ({ page }) => {
    await expect(page).toHaveTitle(/XXL-JOB/);
  });

  test('should display login form elements', async ({ page }) => {
    await expect(page.getByPlaceholder('请输入用户名')).toBeVisible();
    await expect(page.getByPlaceholder('请输入密码')).toBeVisible();
    await expect(page.getByRole('button', { name: '登 录' })).toBeVisible();
    await expect(page.getByText('记住密码')).toBeVisible();
  });

  test('should login successfully with valid credentials', async ({ page }) => {
    await page.getByPlaceholder('请输入用户名').fill('admin');
    await page.getByPlaceholder('请输入密码').fill('123456');
    await page.getByRole('button', { name: '登 录' }).click();

    await expect(page.getByRole('menubar')).toBeVisible({ timeout: 10000 });
    await expect(page).toHaveURL(/.*dashboard/);
  });

  test('should show error for invalid credentials', async ({ page }) => {
    await page.getByPlaceholder('请输入用户名').fill('admin');
    await page.getByPlaceholder('请输入密码').fill('wrongpassword');
    await page.getByRole('button', { name: '登 录' }).click();

    await expect(page.getByText('账号或密码错误')).toBeVisible();
  });

  test('should logout successfully', async ({ page }) => {
    await page.getByPlaceholder('请输入用户名').fill('admin');
    await page.getByPlaceholder('请输入密码').fill('123456');
    await page.getByRole('button', { name: '登 录' }).click();
    await expect(page.getByRole('menubar')).toBeVisible();

    await page.getByRole('button', { name: 'admin' }).click();
    await page.getByRole('menuitem', { name: '退出登录' }).click();

    await expect(page).toHaveURL(/.*login/);
  });
});
