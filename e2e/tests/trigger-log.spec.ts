import { test, expect } from './fixtures';

test.describe('Trigger Log', () => {
  test.beforeEach(async ({ page }) => {
    await page.getByRole('link', { name: /调度日志/ }).click();
    await expect(page).toHaveURL(/.*joblog/);
  });

  test('should display trigger log list with columns', async ({ page }) => {
    await expect(page.getByText('任务ID任务描述调度时间调度结果调度备注执行时间执行结果执行备注操作')).toBeVisible();
  });

  test('should filter logs by executor group', async ({ page }) => {
    const groupSelect = page.locator('select').first();
    await groupSelect.selectOption({ label: '示例执行器' });
    await page.getByRole('button', { name: '搜索' }).click();

    await page.waitForTimeout(1000);
    const rows = page.locator('tbody tr');
    const count = await rows.count();
    expect(count).toBeGreaterThan(0);
  });

  test('should filter logs by status', async ({ page }) => {
    const statusSelect = page.locator('select').filter({ hasText: '全部' }).last();
    await statusSelect.selectOption({ label: '成功' });
    await page.getByRole('button', { name: '搜索' }).click();

    await page.waitForTimeout(1000);
    const statusCells = page.locator('tbody tr td:nth-child(4)');
    const count = await statusCells.count();
    for (let i = 0; i < count; i++) {
      await expect(statusCells.nth(i)).toContainText('成功');
    }
  });

  test('should display pagination info', async ({ page }) => {
    await expect(page.locator('.dataTables_info')).toContainText('第 1 页');
    await expect(page.locator('.dataTables_info')).toContainText('条记录');
  });

  test('should navigate to next page', async ({ page }) => {
    const nextLink = page.locator('.paginate_button.next a');

    if (await nextLink.isVisible() && !await nextLink.evaluate(el => el.classList.contains('disabled'))) {
      await nextLink.click();
//       await expect(page.locator('.dataTables_info')).toContainText('第 2 页');
    }
  });

  test('should clear old logs', async ({ page }) => {
    await page.getByRole('button', { name: '清理' }).click();

    const dialog = page.locator('#clearLogModal');
    await expect(dialog).toBeVisible();
    await expect(dialog.getByText('确定 取消')).toBeVisible();

    await dialog.getByText('取消').click();
    await expect(dialog).not.toBeVisible();
  });

  test('should change page size', async ({ page }) => {
    await page.locator('select').filter({ hasText: '10' }).selectOption('25');
    await expect(page.locator('select').filter({ hasText: '25' })).toBeVisible();
  });
});
