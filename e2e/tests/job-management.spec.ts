import { test, expect } from './fixtures';

test.describe('Job Management', () => {
  test.beforeEach(async ({ page }) => {
    await page.getByRole('menuitem', { name: '任务管理' }).click();
    await expect(page).toHaveURL(/.*jobinfo/);
    await page.locator('tbody tr').first().waitFor({ timeout: 5000 });
  });

  test('should display job list with columns', async ({ page }) => {
    const headerRow = page.locator('thead tr').first();
    await expect(headerRow).toContainText('ID');
    await expect(headerRow).toContainText('任务描述');
    await expect(headerRow).toContainText('调度类型');
    await expect(headerRow).toContainText('运行模式');
    await expect(headerRow).toContainText('负责人');
    await expect(headerRow).toContainText('状态');
    await expect(headerRow).toContainText('操作');
  });

  test('should search jobs by description', async ({ page }) => {
    await page.getByRole('textbox', { name: '任务描述' }).fill('测试');
    await page.getByRole('button', { name: '查询' }).click();

    await expect(page.locator('tbody tr').first()).toBeVisible();
  });

  test('should open add job dialog', async ({ page }) => {
    await page.getByRole('button', { name: '新增' }).click();

    const dialog = page.locator('.el-dialog');
    await expect(dialog).toBeVisible();

    await dialog.getByRole('button', { name: '取消' }).click();
    await expect(dialog).not.toBeVisible();
  });

  test('should open edit job dialog', async ({ page }) => {
    const firstRow = page.locator('tbody tr').first();
    if (await firstRow.count() === 0) {
      test.skip();
      return;
    }

    await firstRow.getByRole('button', { name: '编辑' }).click();

    const dialog = page.locator('.el-dialog');
    await expect(dialog).toBeVisible();

    await dialog.getByRole('button', { name: '取消' }).click();
    await expect(dialog).not.toBeVisible();
  });

  test('should toggle job status', async ({ page }) => {
    const firstRow = page.locator('tbody tr').first();
    if (await firstRow.count() === 0) {
      test.skip();
      return;
    }

    const switchEl = firstRow.locator('.el-switch');
    await expect(switchEl).toBeVisible();

    await switchEl.click();

    const confirmDialog = page.locator('.el-message-box');
    await expect(confirmDialog).toBeVisible();

    await confirmDialog.getByRole('button', { name: '取消' }).click();
    await expect(confirmDialog).not.toBeVisible();
  });

  test('should open trigger dialog', async ({ page }) => {
    const firstRow = page.locator('tbody tr').first();
    if (await firstRow.count() === 0) {
      test.skip();
      return;
    }

    await firstRow.getByRole('button', { name: '执行' }).click();

    const dialog = page.locator('.el-dialog');
    await expect(dialog).toBeVisible();

    await dialog.getByRole('button', { name: '取消' }).click();
    await expect(dialog).not.toBeVisible();
  });

  test('should open more menu', async ({ page }) => {
    const firstRow = page.locator('tbody tr').first();
    if (await firstRow.count() === 0) {
      test.skip();
      return;
    }

    await firstRow.getByRole('button', { name: '更多' }).click();

    await expect(page.getByRole('menuitem', { name: '删除' })).toBeVisible();

    await page.keyboard.press('Escape');
  });

  test('should change page size', async ({ page }) => {
    await page.locator('.el-pagination .el-select').click();
    await page.getByRole('option', { name: '20条/页' }).click();
    await expect(page.locator('.el-pagination')).toContainText('20条/页');
  });
});
