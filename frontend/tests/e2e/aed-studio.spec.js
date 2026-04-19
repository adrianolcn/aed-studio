const { test, expect } = require('@playwright/test');

const apiBase = 'http://127.0.0.1:8080';

async function completeCurrentExercise(page, topicId) {
  const option = page.locator('.page.active .exercise-panel .exercise-option[data-answer="B"]').first();
  await expect(option).toBeVisible();
  await option.click();
  await expect.poll(
    () => page.evaluate(topicId => window.AedApi.getProgress().then(p => p.topicStates?.[topicId]?.state), topicId),
    { timeout: 20_000 }
  ).toBe('COMPLETED');
}

async function openTopic(page, topicId, path) {
  await page.evaluate(({ topicId, path }) => window.nav(null, topicId, path), { topicId, path });
  await expect(page.locator(`#page-${topicId}.active`)).toBeVisible();
}

test('fluxo completo: login, dashboard, recomendação, simulador, código, progresso e analytics', async ({ page }) => {
  const stamp = Date.now();
  const email = `e2e-${stamp}@aedstudio.test`;
  const username = `e2e_${stamp}`;
  const password = 'Senha1234';

  await page.goto(`/login.html?apiBase=${encodeURIComponent(apiBase)}`);
  await page.locator('#tab-register').click();
  await page.locator('#reg-name').fill('Aluno E2E');
  await page.locator('#reg-username').fill(username);
  await page.locator('#reg-email').fill(email);
  await page.locator('#reg-password').fill(password);
  await page.locator('#btn-register').click();

  await page.waitForURL(/aed-studio\.html/, { timeout: 20_000 });
  await expect(page.locator('#student-dashboard')).toBeVisible();
  await expect(page.locator('#recommendation-primary')).toBeVisible();

  await openTopic(page, 'algoritmos', 'algoritmos/estrutura-de-dados/inicio');
  await completeCurrentExercise(page, 'algoritmos');
  await expect(page.locator('#xp-label')).toContainText(/XP/);

  await openTopic(page, 'tad', 'algoritmos/estrutura-de-dados/tad');
  await completeCurrentExercise(page, 'tad');

  await openTopic(page, 'arrays', 'algoritmos/estruturas-lineares/arrays');
  await expect(page.locator('.page.active .simulator-panel')).toBeVisible();
  await page.locator('.page.active .simulator-panel .sim-value').fill('42');
  await page.locator('.page.active .simulator-panel [data-sim-action="insert"]').click();
  await expect(page.locator('.page.active .sim-stage')).toContainText('42');

  await expect(page.locator('.page.active .code-sandbox-panel')).toBeVisible();
  const editor = page.locator('.page.active .code-sandbox-panel textarea');
  await editor.fill('return -1;');
  await page.locator('.page.active .code-run').click();
  await expect(page.locator('.page.active .code-sandbox-panel .exercise-feedback')).toContainText(/não passou|ajustar/i);

  await editor.fill('for (int i = 0; i < values.length; i++) { if (values[i] == target) return i; } return -1;');
  await page.locator('.page.active .code-run').click();
  await expect(page.locator('.page.active .code-sandbox-panel .exercise-feedback')).toContainText(/Todos os 3 cenários/i);
  await expect(page.locator('.page.active .code-history-row').first()).toContainText(/sucesso|melhor/i);

  const analytics = await page.evaluate(() => window.AedApi.getAnalyticsOverview());
  expect(analytics.codeSubmissions).toBeGreaterThanOrEqual(2);
  expect(analytics.codeSuccessPercent).toBeGreaterThan(0);

  await openTopic(page, 'home', 'algoritmos/estrutura-de-dados/inicio');
  await expect(page.locator('#dash-progress')).toContainText(/%/);
});
