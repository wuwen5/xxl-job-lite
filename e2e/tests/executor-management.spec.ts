import { test, expect } from './fixtures';

test.describe('Executor Management', () => {
  test.beforeEach(async ({ page }) => {
    await page.getByRole('link', { name: /执行器管理/ }).click();
    await expect(page).toHaveURL(/.*jobgroup/);
  });

  test('should display executor list with columns', async ({ page }) => {
    await expect(page.getByRole('gridcell', { name: 'AppName' })).toBeVisible();
    await expect(page.getByRole('gridcell', { name: '名称' })).toBeVisible();
    await expect(page.getByRole('gridcell', { name: '注册方式' })).toBeVisible();
    await expect(page.getByRole('gridcell', { name: /OnLine 机器地址/ })).toBeVisible();
    await expect(page.getByRole('gridcell', { name: '操作', exact: true })).toBeVisible();
  });

  test('should search executors by AppName', async ({ page }) => {
    await page.getByRole('textbox', { name: '请输入AppName' }).fill('xxl-job-executor-sample');
    await page.getByRole('button', { name: '搜索' }).click();

    await expect(page.locator('tbody tr')).toHaveCount(1);
    await expect(page.locator('tbody tr').first()).toContainText('xxl-job-executor-sample');
  });

  test('should search executors by name', async ({ page }) => {
    await page.getByPlaceholder('名称', { exact: true }).fill('示例执行器');
    await page.getByRole('button', { name: '搜索' }).click();

    await expect(page.locator('tbody tr')).toHaveCount(1);
    await expect(page.locator('tbody tr').first()).toContainText('示例执行器');
  });

  test('should add new executor', async ({ page }) => {
    await page.getByRole('button', { name: '新增' }).click();

    const dialog = page.locator('#addModal');
    await expect(dialog).toBeVisible();

    await dialog.getByPlaceholder('请输入AppName').fill('e2e-test-executor');
    await dialog.getByPlaceholder('请输入名称').fill('E2E测试执行器');

    await dialog.getByText('保存').click();


    await expect(page.locator('#layui-layer-shade1')).toBeVisible();
    await expect(page.getByText('新增成功')).toBeVisible();
  });

  test('should edit existing executor', async ({ page }) => {
    await page.getByRole('textbox', { name: '请输入AppName' }).fill('e2e-test-executor');
    await page.getByRole('button', { name: '搜索' }).click();

    const testRow = page.getByRole('button', { name: 'Toggle Dropdown' });

    if (await testRow.count() > 0) {
      await testRow.first().click();
      await page.getByRole('link', { name: '编辑' }).click();

      const dialog = page.locator('#updateModal');
      await expect(dialog).toBeVisible();

      await dialog.getByPlaceholder('请输入名称').clear();
      await dialog.getByPlaceholder('请输入名称').fill('E2E测试执行器-更新');

      await dialog.getByText('保存').click();

      await expect(page.getByText('更新成功')).toBeVisible();
    }
  });

  test('should delete executor', async ({ page }) => {
    await page.getByRole('textbox', { name: '请输入AppName' }).fill('e2e-test-executor');
    await page.getByRole('button', { name: '搜索' }).click();

    const testRow = page.locator('tbody tr').filter({ hasText: 'e2e-test-executor' });
    if (await testRow.count() > 0) {
      await testRow.locator('.btn-group > button:nth-child(2)').first().click();
      await page.getByRole('link', { name: '删除' }).click();

      page.on('dialog', dialog => dialog.accept());

      await expect(page.locator('#layui-layer1')).toBeVisible();
      await page.getByText('确定取消').getByText('确定').click();
      await expect(page.getByText('成功')).toBeVisible();
    }
  });

  test('should view online machines', async ({ page }) => {
    const viewLink = page.locator('tbody tr').first().getByRole('link', { name: /查看/ });

    if (await viewLink.isVisible()) {
      await viewLink.click();

      const dialog = page.locator('#showRegistryListModal');
      await expect(dialog).toBeVisible();

      await dialog.getByText('确定').click();
      await expect(dialog).not.toBeVisible();
    }
  });

  test('should change page size', async ({ page }) => {
    await page.locator('select').filter({ hasText: '10' }).selectOption('25');
    await expect(page.locator('select').filter({ hasText: '25' })).toBeVisible();
  });
});
