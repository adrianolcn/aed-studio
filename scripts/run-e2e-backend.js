const { spawn } = require('node:child_process');
const path = require('node:path');

const root = path.resolve(__dirname, '..');
const backendDir = path.join(root, 'backend');
const executable = process.env.MAVEN_EXECUTABLE || 'mvn';
const args = [];

if (process.env.MAVEN_REPO_LOCAL) {
  args.push(`-Dmaven.repo.local=${process.env.MAVEN_REPO_LOCAL}`);
}

if (process.env.E2E_USE_TEST_CLASSPATH !== 'false') {
  args.push('-Dspring-boot.run.useTestClasspath=true');
}

args.push('spring-boot:run');

const isWindows = process.platform === 'win32';
const command = isWindows
  ? `"${executable}" ${args.map((arg) => `"${arg}"`).join(' ')}`
  : executable;

const child = spawn(command, isWindows ? [] : args, {
  cwd: backendDir,
  env: {
    ...process.env,
    SPRING_PROFILES_ACTIVE: process.env.SPRING_PROFILES_ACTIVE || 'e2e',
  },
  stdio: 'inherit',
  shell: isWindows,
});

child.on('exit', (code, signal) => {
  if (signal) {
    process.kill(process.pid, signal);
    return;
  }
  process.exit(code ?? 0);
});
