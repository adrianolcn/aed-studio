import { getApiClient } from '../api/client.js';

export async function requireAuthenticatedUser() {
  return getApiClient().requireAuth();
}

export function currentUserSnapshot() {
  return getApiClient().getCurrentUser();
}

export function clearSession() {
  return getApiClient().clearSession();
}
