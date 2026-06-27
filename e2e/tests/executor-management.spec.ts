import { test, expect } from './fixtures';

test.describe('Executor Management', () => {
  test.beforeEach(async ({ page }) => {
    await page.getByRole('menuitem', { name: '执行器管理' }).click();
    await expect(page).toHaveURL(/.*jobgroup/);
  });

  test('should display executor list with columns', async ({ page }) => {
    await expect(page.getByRole('columnheader', { name: 'AppName' })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: '名称' })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: '地址类型' })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: '机器地址' })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: '操作', exact: true })).toBeVisible();
  });

  test('should search executors by AppName', async ({ page }) => {
    await page.getByRole('textbox', { name: 'AppName' }).fill('xxl-job-executor-sample');
    await page.getByRole('button', { name: '查询' }).click();

    await expect(page.locator('tbody tr')).toHaveCount(1);
    await expect(page.locator('tbody tr').first()).toContainText('xxl-job-executor-sample');
  });

  test('should search executors by name', async ({ page }) => {
    await page.getByRole('textbox', { name: '名称' }).fill('示例执行器');
    await page.getByRole('button', { name: '查询' }).click();

    await expect(page.locator('tbody tr')).toHaveCount(1);
    await expect(page.locator('tbody tr').first()).toContainText('示例执行器');
  });

  test('should add new executor', async ({ page }) => {
    await page.getByRole('button', { name: '新增' }).click();

    const dialog = page.getByRole('dialog', { name: '新增执行器' });
    await expect(dialog).toBeVisible();

    await dialog.getByPlaceholder('执行器AppName').fill('e2e-test-executor');
    await dialog.getByPlaceholder('执行器名称').fill('E2E测试执行器');

    const [response] = await Promise.all([
    page.waitForResponse(resp => 
      resp.url().includes('/admin-api/v1/jobgroup') && 
      resp.request().method() === 'POST' &&
      resp.status() === 200
    ),
      dialog.getByRole('button', { name: '确定' }).click()
    ]);
    const json = await response.json();
    expect(json.code).toBe(200);

    await expect(page.getByText('操作成功').first()).toBeVisible()
    await expect(dialog).toBeVisible();
  });

  test('should edit existing executor', async ({ page }) => {
    await page.getByRole('textbox', { name: 'AppName' }).fill('e2e-test-executor');
    await page.getByRole('button', { name: '查询' }).click();

    const testRow = page.locator('tbody tr').filter({ hasText: 'e2e-test-executor' });
    if (await testRow.count() > 0) {
      await testRow.getByRole('button', { name: '编辑' }).first().click();

      const dialog = page.getByRole('dialog', { name: '编辑执行器' });
      await expect(dialog).toBeVisible();

      await dialog.getByPlaceholder('执行器名称').clear();
      await dialog.getByPlaceholder('执行器名称').fill('E2E测试执行器-更新');

      const [response] = await Promise.all([
        page.waitForResponse(resp => 
          resp.url().includes('/admin-api/v1/jobgroup/') && 
          resp.request().method() === 'PUT' &&
          resp.status() === 200
        ),
        dialog.getByRole('button', { name: '确定' }).click()
      ]);
      const json = await response.json();
      expect(json.code).toBe(200);

      await expect(page.getByText('操作成功').first()).toBeVisible();
    }
  });

  test('should delete executor', async ({ page }) => {
    await page.getByRole('textbox', { name: 'AppName' }).fill('e2e-test-executor');
    await page.getByRole('button', { name: '查询' }).click();

    const testRow = page.locator('tbody tr').filter({ hasText: 'e2e-test-executor' });
    if (await testRow.count() > 0) {
      await testRow.getByRole('button', { name: '删除' }).first().click();

      const dialog = page.getByRole('dialog', { name: '删除' });
      await expect(dialog).toBeVisible();
      await expect(dialog.getByText('确认删除？')).toBeVisible();

      const [response] = await Promise.all([
          page.waitForResponse(resp => 
            resp.url().includes('/admin-api/v1/jobgroup/') && 
            resp.request().method() === 'DELETE' &&
            resp.status() === 200
          ),
          dialog.getByRole('button', { name: '确定' }).click()
        ]);
      const json = await response.json();
      expect(json.code).toBe(200);
            
      await expect(page.getByText('操作成功').first()).toBeVisible();
    }
  });

  test('should view online machines', async ({ page }) => {
    const viewButtons = page.locator('tbody tr').first().getByRole('button', { name: /查看/ });
    if (await viewButtons.count() > 0) {
      await viewButtons.first().click();

      const dialog = page.getByRole('dialog', { name: /注册节点/ });
      await expect(dialog).toBeVisible();

      await dialog.getByRole('button', { name: '确定' }).click();
      await expect(dialog).not.toBeVisible();
    }
  });

  test('should change page size', async ({ page }) => {
    await page.locator('.el-pagination .el-select').click();
    await page.getByRole('option', { name: '20条/页' }).click();
    await expect(page.locator('.el-pagination')).toContainText('20条/页');
  });
});
