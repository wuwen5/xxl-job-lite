import { test, expect } from './fixtures';

test.describe('Trigger Log', () => {
  test.beforeEach(async ({ page }) => {
    await page.getByRole('menuitem', { name: '调度日志' }).click();
    await expect(page).toHaveURL(/.*joblog/);
  });

  test('should display trigger log list with columns', async ({ page }) => {
    const headerRow = page.locator('thead tr').first();
    await expect(headerRow).toContainText('任务ID');
    await expect(headerRow).toContainText('任务描述');
    await expect(headerRow).toContainText('调度时间');
    await expect(headerRow).toContainText('调度结果');
    await expect(headerRow).toContainText('调度备注');
    await expect(headerRow).toContainText('执行时间');
    await expect(headerRow).toContainText('执行结果');
    await expect(headerRow).toContainText('执行备注');
    await expect(headerRow).toContainText('操作');
  });

  test('should filter logs by executor group', async ({ page }) => {
    await page.getByRole('menuitem', { name: '任务管理' }).click();
    await expect(page).toHaveURL(/.*jobinfo/);

    const firstRow = page.locator('tbody tr').first();
    await firstRow.getByRole('button', { name: '执行' }).first().click();

    const triggerDialog = page.getByRole('dialog', { name: '执行任务' });
    await expect(triggerDialog).toBeVisible();
    await triggerDialog.getByRole('button', { name: '确定' }).click();
    await expect(page.getByText('操作成功')).toBeVisible();

    await page.getByRole('menuitem', { name: '调度日志' }).click();
    await expect(page).toHaveURL(/.*joblog/);

    const executorDropdown = page.locator('.el-select').first();
    await executorDropdown.click();
    await page.getByRole('option', { name: '示例执行器' }).click();
    await page.getByRole('button', { name: '查询' }).click();

    await expect(page.locator('tbody tr').first()).toBeVisible();
  });

  test('should filter logs by status', async ({ page }) => {
    await page.getByRole('menuitem', { name: '任务管理' }).click();
    await expect(page).toHaveURL(/.*jobinfo/);

    const firstRow = page.locator('tbody tr').first();
    await firstRow.getByRole('button', { name: '执行' }).first().click();

    const triggerDialog = page.getByRole('dialog', { name: '执行任务' });
    await expect(triggerDialog).toBeVisible();
    await triggerDialog.getByRole('button', { name: '确定' }).click();
    await expect(page.getByText('操作成功')).toBeVisible();

    await page.getByRole('menuitem', { name: '调度日志' }).click();
    await expect(page).toHaveURL(/.*joblog/);

    await expect(page.locator('tbody tr').first()).toBeVisible();

    const statusDropdown = page.locator('.el-select').nth(2);
    await statusDropdown.click();
    await page.getByRole('option', { name: '失败' }).click();
    await page.getByRole('button', { name: '查询' }).click();

    await expect(page.locator('tbody tr').first()).toBeVisible();
  });

  test('should display pagination info', async ({ page }) => {
    await expect(page.getByText('共').and(page.getByText('条')).first()).toBeVisible();
    await expect(page.locator('.el-pagination').getByText('10条/页')).toBeVisible();
  });

  test('should navigate to next page', async ({ page }) => {
    const nextButton = page.getByRole('button', { name: '下一页' });

    if (await nextButton.isEnabled()) {
      await nextButton.click();
      await expect(page.getByText('第 2 页').or(page.locator('li[aria-current="true"]'))).toBeVisible();
    }
  });

  test('should clear old logs', async ({ page }) => {
    await page.getByRole('button', { name: '清理日志' }).click();

    const dialog = page.getByRole('dialog', { name: '清理日志' });
    await expect(dialog).toBeVisible();

    await dialog.getByRole('button', { name: '取消' }).click();
    await expect(dialog).not.toBeVisible();
  });

  test('should change page size', async ({ page }) => {
    await page.locator('.el-pagination .el-select').click();
    await page.getByRole('option', { name: '20条/页' }).click();
    await expect(page.locator('.el-pagination')).toContainText('20条/页');
  });

  test('should clear all trigger logs', async ({ page }) => {
    await page.getByRole('button', { name: '清理日志' }).click();

    const dialog = page.getByRole('dialog', { name: '清理日志' });
    await expect(dialog).toBeVisible();
    await dialog.getByRole('button', { name: '确定' }).click();

    const confirmDialog = page.getByRole('dialog', { name: '确定' });
    await expect(confirmDialog).toBeVisible();
    await confirmDialog.getByRole('button', { name: '确定' }).click();

    await expect(dialog).not.toBeVisible();
  });
});
