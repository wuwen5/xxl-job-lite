import { test, expect } from './fixtures';

test.describe('Dashboard', () => {
  test('should display dashboard with statistics', async ({ page }) => {
    await expect(page.getByRole('heading', { name: '运行报表' })).toBeVisible();

    await expect(page.getByText('任务数量', { exact: true })).toBeVisible();
    await expect(page.getByText('调度次数', { exact: true })).toBeVisible();
    await expect(page.getByText('执行器数量', { exact: true })).toBeVisible();
  });

  test('should display scheduling report chart', async ({ page }) => {
    await expect(page.getByRole('heading', { name: '调度报表' })).toBeVisible();
  });

  test('should navigate to job management', async ({ page }) => {
    await page.getByRole('link', { name: /任务管理/ }).click();
    await expect(page).toHaveURL(/.*jobinfo/);
    await expect(page.getByRole('heading', { name: '任务管理' })).toBeVisible();
  });

  test('should navigate to trigger log', async ({ page }) => {
    await page.getByRole('link', { name: /调度日志/ }).click();
    await expect(page).toHaveURL(/.*joblog/);
    await expect(page.getByRole('heading', { name: '调度日志' })).toBeVisible();
  });

  test('should navigate to executor management', async ({ page }) => {
    await page.getByRole('link', { name: /执行器管理/ }).click();
    await expect(page).toHaveURL(/.*jobgroup/);
    await expect(page.getByRole('heading', { name: '执行器管理' })).toBeVisible();
  });

  test('should navigate to user management', async ({ page }) => {
    await page.getByRole('link', { name: /用户管理/ }).click();
    await expect(page).toHaveURL(/.*user/);
    await expect(page.getByRole('heading', { name: '用户管理' })).toBeVisible();
  });

  test('should navigate back to dashboard from logo', async ({ page }) => {
    await page.getByRole('link', { name: /任务管理/ }).click();
    await expect(page).toHaveURL(/.*jobinfo/);

    await page.getByRole('link', { name: '任务调度中心' }).click();
    await expect(page).toHaveURL(/.*xxl-job-admin\//);
  });
});
