import { getApiClient, apiDomains } from './api/client.js';
import { topicStates, learningFlow, isUnlocked } from './learning/catalog.js';
import { progressPercent, formatXp } from './progress/progress-view.js';
import { simulatorTypes, supportsSimulator } from './simulators/registry.js';
import { accuracyLabel } from './analytics/analytics-view.js';
import { recommendationTone } from './recommendations/recommendation-view.js';
import { sandboxModes, isSafeDisplayStatus } from './sandbox/code-judge.js';

window.AedStudioModules = Object.freeze({
  getApiClient,
  apiDomains,
  topicStates,
  learningFlow,
  isUnlocked,
  progressPercent,
  formatXp,
  simulatorTypes,
  supportsSimulator,
  accuracyLabel,
  recommendationTone,
  sandboxModes,
  isSafeDisplayStatus,
});
