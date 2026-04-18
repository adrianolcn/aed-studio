const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { spawnSync } = require('node:child_process');
const test = require('node:test');

function chromePath() {
  const candidates = [
    'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
    'C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe',
    '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
    '/usr/bin/google-chrome',
    '/usr/bin/chromium',
    '/usr/bin/chromium-browser',
  ];
  return candidates.find(p => fs.existsSync(p));
}

test('visual E2E smoke abre tela principal em Chrome headless e captura screenshot não vazio', { skip: !chromePath() }, t => {
  const chrome = chromePath();
  const screenshot = path.join(os.tmpdir(), 'aed-studio-visual-e2e.png');
  const profile = path.join(os.tmpdir(), 'aed-studio-visual-profile');
  fs.rmSync(screenshot, { force: true });
  fs.rmSync(profile, { recursive: true, force: true });

  const target = 'file:///' + path.resolve(__dirname, '..', 'aed-studio.html').replace(/\\/g, '/');
  const result = spawnSync(chrome, [
    '--headless=new',
    '--no-sandbox',
    '--disable-gpu',
    '--disable-dev-shm-usage',
    '--disable-crash-reporter',
    '--disable-features=Crashpad',
    '--run-all-compositor-stages-before-draw',
    '--virtual-time-budget=2500',
    '--hide-scrollbars',
    '--window-size=390,844',
    `--user-data-dir=${profile}`,
    `--screenshot=${screenshot}`,
    target,
  ], { encoding: 'utf8', timeout: 30000 });

  if (result.error?.code === 'EPERM') {
    t.skip('o ambiente bloqueou spawn do Chrome; execute fora do sandbox para validar screenshot');
    return;
  }
  assert.ifError(result.error);
  if (process.env.CI && result.status !== 0) {
    t.skip(`Chrome headless indisponível neste runner: ${result.stderr || result.stdout || `status=${result.status}`}`);
    return;
  }
  assert.equal(result.status, 0, result.stderr || result.stdout || `signal=${result.signal}`);
  const stat = fs.statSync(screenshot);
  assert.ok(stat.size > 5000, `screenshot pequeno demais: ${stat.size}`);
});
