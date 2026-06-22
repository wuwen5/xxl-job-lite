import { test, expect } from './fixtures';

test.describe('Job Management', () => {
  test.beforeEach(async ({ page }) => {
    await page.getByRole('menuitem', { name: '任务管理' }).click();
    await expect(page).toHaveURL(/.*jobinfo/);
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
    await page.getByPlaceholder('任务描述').fill('测试');
    await page.getByRole('button', { name: '查询' }).click();

    await expect(page.locator('tbody tr').first()).toBeVisible();
    await expect(page.locator('tbody tr').first()).toContainText('测试');
  });

  test('should add new job', async ({ page }) => {

    await page.getByRole('button', { name: '新增' }).click();

    const dialog = page.locator('#addModal');
    await expect(dialog).toBeVisible();

    await dialog.locator('input[name="jobDesc"]').fill('E2E测试任务');
    await dialog.locator('input[name="author"]').fill('testuser');
    await dialog.locator('input[name="alarmEmail"]').fill('test@example.com');
    await dialog.locator('input[name="cronGen_display"]').fill('0 0 0 * * ? *');
    await dialog.locator('input[name="executorHandler"]').fill('testJobHandler');

    await dialog.getByText('保存').click();

    await expect(page.getByText('系统提示')).toBeVisible();
    await expect(page.getByText('新增成功')).toBeVisible();
  });

  test('should edit existing job', async ({ page }) => {
    await page.getByPlaceholder('任务描述').fill('E2E测试任务');
    await page.getByRole('button', { name: '查询' }).click();
          
    const firstRow = page.locator('tbody tr').first();
    await firstRow.getByRole('button', { name: '更多' }).first().click();
    await page.getByRole('link', { name: '编辑' }).click();

    const dialog = page.locator('#updateModal');
    await expect(dialog).toBeVisible();

    await dialog.getByPlaceholder('请输入任务描述').clear();
    await dialog.getByPlaceholder('请输入任务描述').fill('E2E测试任务-更新后的任务描述');

    await dialog.getByText('保存').click();

    await expect(page.getByText('系统提示')).toBeVisible();
    await expect(page.getByText('更新成功')).toBeVisible();
  });

  test('should stop and start job', async ({ page }) => {
      
    await page.getByPlaceholder('任务描述').fill('E2E测试任务');
    await page.getByRole('button', { name: '查询' }).click();
    
    const firstRow = page.locator('tbody tr').first();
    
    const runningRow = page.locator('tbody tr').filter({ hasText: 'STOP' }).first();

    if (await runningRow.count() > 0) {
      await firstRow.getByRole('button', { name: '更多' }).first().click();
      await page.getByRole('link', { name: '启动' }).click();

      await expect(page.getByText('系统提示')).toBeVisible();
      await page.getByText('确定', { exact: true }).click();
      await expect(page.getByText('启动成功')).toBeVisible();
    }
  });

  test('should manually trigger job', async ({ page }) => {
    await page.getByPlaceholder('任务描述').fill('E2E测试任务');
    await page.getByRole('button', { name: '查询' }).click();
          
    const firstRow = page.locator('tbody tr').first();
    await firstRow.getByRole('button', { name: '执行' }).first().click();

    const dialog = page.locator('#jobTriggerModal');
    await expect(dialog).toBeVisible();

    await dialog.getByText('保存').click();

    await expect(page.getByText('成功')).toBeVisible();
  });

  test('should delete job', async ({ page }) => {
    await page.getByPlaceholder('任务描述').fill('E2E测试任务');
    await page.getByRole('button', { name: '查询' }).click();

    const firstRow = page.locator('tbody tr').first();
    await firstRow.getByRole('button', { name: '更多' }).first().click();
    await page.getByRole('link', { name: '删除' }).click();
    await page.getByText('确定取消').getByText('确定').click();
    await expect(page.getByText('删除成功')).toBeVisible();
  });

  test('should change page size', async ({ page }) => {
    await page.locator('select').filter({ hasText: '10' }).selectOption('25');
    await expect(page.locator('select').filter({ hasText: '25' })).toBeVisible();
  });
});
