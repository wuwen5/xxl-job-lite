import { test, expect } from './fixtures';

test.describe('Dashboard', () => {
  test('should display dashboard with statistics', async ({ page }) => {
    await expect(page.getByRole('menuitem', { name: '运行报表' })).toBeVisible();

    await expect(page.getByText('任务数量', { exact: true })).toBeVisible();
    await expect(page.getByText('调度次数', { exact: true })).toBeVisible();
    await expect(page.getByText('执行器数量', { exact: true })).toBeVisible();
  });

  test('should display scheduling report chart', async ({ page }) => {
    await expect(page.getByText('调度趋势图')).toBeVisible();
  });

  test('should navigate to job management', async ({ page }) => {
    await page.getByRole('menuitem', { name: '任务管理' }).click();
    await expect(page).toHaveURL(/.*jobinfo/);
  });

  test('should navigate to trigger log', async ({ page }) => {
    await page.getByRole('menuitem', { name: '调度日志' }).click();
    await expect(page).toHaveURL(/.*joblog/);
  });

  test('should navigate to executor management', async ({ page }) => {
    await page.getByRole('menuitem', { name: '执行器管理' }).click();
    await expect(page).toHaveURL(/.*jobgroup/);
  });

  test('should navigate to user management', async ({ page }) => {
    await page.getByRole('menuitem', { name: '用户管理' }).click();
    await expect(page).toHaveURL(/.*user/);
  });
});
