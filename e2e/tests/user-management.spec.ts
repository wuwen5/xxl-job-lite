import { test, expect } from './fixtures';

test.describe('User Management', () => {
  test.beforeEach(async ({ page }) => {
    await page.getByRole('menuitem', { name: '用户管理' }).click();
    await expect(page).toHaveURL(/.*user/);
  });

  async function deleteUserIfExists(page: import('@playwright/test').Page) {
    await page.getByRole('textbox', { name: '账号' }).fill('e2euser');
    await page.getByRole('button', { name: '查询' }).click();

    const testRow = page.locator('tbody tr').filter({ hasText: 'e2euser' });
    if (await testRow.count() > 0) {
      await testRow.getByRole('button', { name: '删除' }).click();
      await expect(page.locator('.el-message-box')).toBeVisible();
      await page.locator('.el-message-box').getByRole('button', { name: '确定' }).click();
      await expect(page.locator('.el-message-box')).not.toBeVisible();
    }
  }

  test('should display user list with columns', async ({ page }) => {
    const headerRow = page.locator('thead tr').first();
    await expect(headerRow).toContainText('ID');
    await expect(headerRow).toContainText('账号');
    await expect(headerRow).toContainText('角色');
    await expect(headerRow).toContainText('操作');
  });

  test('should search users by role', async ({ page }) => {
    const roleSelect = page.locator('.el-select').first();
    await roleSelect.click();
    await page.getByRole('option', { name: '管理员' }).click();
    await page.getByRole('button', { name: '查询' }).click();

    await expect(page.locator('tbody tr').first()).toContainText('admin');
    await expect(page.locator('tbody tr').first()).toContainText('管理员');
  });

  test('should search users by username', async ({ page }) => {
    await page.getByRole('textbox', { name: '账号' }).fill('admin');
    await page.getByRole('button', { name: '查询' }).click();

    await expect(page.locator('tbody tr').first()).toContainText('admin');
  });

  test('should add new user', async ({ page }) => {
    await deleteUserIfExists(page);

    await page.getByRole('button', { name: '新增' }).click();

    const dialog = page.locator('.el-dialog');
    await expect(dialog).toBeVisible();

    await dialog.getByRole('textbox', { name: '* 账号' }).fill('e2euser');
    await dialog.getByRole('textbox', { name: '* 密码' }).fill('password123');
    await dialog.locator('.el-radio').filter({ hasText: '管理员' }).click();

    await dialog.getByRole('button', { name: '确定' }).click();

    await expect(dialog).not.toBeVisible();
  });

  test('should edit existing user', async ({ page }) => {
    await page.getByRole('textbox', { name: '账号' }).fill('e2euser');
    await page.getByRole('button', { name: '查询' }).click();

    const testRow = page.locator('tbody tr').filter({ hasText: 'e2euser' });

    if (await testRow.count() > 0) {
      await testRow.getByRole('button', { name: '编辑' }).click();

      const dialog = page.locator('.el-dialog');
      await expect(dialog).toBeVisible();

      await dialog.locator('.el-radio').filter({ hasText: '管理员' }).click();

      await dialog.getByRole('button', { name: '确定' }).click();

      await expect(dialog).not.toBeVisible();
    }
  });

  test('should delete user', async ({ page }) => {
    await page.getByRole('textbox', { name: '账号' }).fill('e2euser');
    await page.getByRole('button', { name: '查询' }).click();

    const testRow = page.locator('tbody tr').filter({ hasText: 'e2euser' });

    if (await testRow.count() > 0) {
      await testRow.getByRole('button', { name: '删除' }).click();

      await expect(page.locator('.el-message-box')).toBeVisible();
      await page.locator('.el-message-box').getByRole('button', { name: '确定' }).click();

      await expect(page.locator('.el-message-box')).not.toBeVisible();
    }
  });

  test('should change page size', async ({ page }) => {
    await page.locator('.el-pagination .el-select').click();
    await page.getByRole('option', { name: '20条/页' }).click();
    await expect(page.locator('.el-pagination')).toContainText('20条/页');
  });
});
