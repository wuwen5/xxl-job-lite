import { test, expect } from './fixtures';

test.describe('User Management', () => {
  test.beforeEach(async ({ page }) => {
    await page.getByRole('menuitem', { name: '用户管理' }).click();
    await expect(page).toHaveURL(/.*user/);
  });

  test('should display user list with columns', async ({ page }) => {
    await expect(page.getByRole('columnheader', { name: 'ID' })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: '账号' })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: '角色' })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: '操作' })).toBeVisible();
  });

  test('should search users by role', async ({ page }) => {
    const roleSelect = page.locator('select').first();
    await roleSelect.selectOption({ label: '管理员' });
    await page.getByRole('button', { name: '查询' }).click();

    await expect(page.locator('tbody tr')).toHaveCount(1);
    await expect(page.locator('tbody tr').first()).toContainText('admin');
    await expect(page.locator('tbody tr').first()).toContainText('管理员');
  });

  test('should search users by username', async ({ page }) => {
    await page.getByPlaceholder('账号').fill('admin');
    await page.getByRole('button', { name: '查询' }).click();

    await expect(page.locator('tbody tr')).toHaveCount(1);
    await expect(page.locator('tbody tr').first()).toContainText('admin');
  });

  test('should add new user', async ({ page }) => {
    await page.getByRole('button', { name: '新增' }).click();

    const dialog = page.locator('#addModal');
    await expect(dialog).toBeVisible();
    await dialog.getByPlaceholder('请输入账号').fill('e2euser');
    await dialog.getByPlaceholder('请输入密码').fill('password123');
    await dialog.locator('input[name="role"]').first().click();

    await dialog.getByText('保存').click();

    await expect(page.getByText('新增成功')).toBeVisible();
  });

  test('should edit existing user', async ({ page }) => {
    await page.getByPlaceholder('账号').fill('e2euser');
    await page.getByRole('button', { name: '查询' }).click();

    const testRow = page.locator('tbody tr').filter({ hasText: 'e2euser' });

    if (await testRow.count() > 0) {
      await testRow.getByRole('button', { name: '编辑' }).click();

      const dialog = page.locator('#updateModal');
      await expect(dialog).toBeVisible();

      await dialog.getByText('普通用户 管理员').locator('input[name="role"]').nth(1);

      await dialog.getByText('保存').click();

      await expect(page.getByText('更新成功')).toBeVisible();
    }
  });

  test('should delete user', async ({ page }) => {
    await page.getByPlaceholder('账号').fill('e2euser');
    await page.getByRole('button', { name: '查询' }).click();

    const testRow = page.locator('tbody tr').filter({ hasText: 'e2euser' });

    if (await testRow.count() > 0) {
      await testRow.getByRole('button', { name: '删除' }).click();

      page.on('dialog', dialog => dialog.accept());

      await expect(page.locator('#layui-layer1')).toBeVisible();
      await page.getByText('确定取消').getByText('确定').click();
      await expect(page.getByText('成功')).toBeVisible();
    }
  });

  test('should change page size', async ({ page }) => {
    await page.locator('select').filter({ hasText: '10' }).selectOption('25');
    await expect(page.locator('select').filter({ hasText: '25' })).toBeVisible();
  });
});
