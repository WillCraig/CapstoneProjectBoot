// Selenium happy path against the running Angular app (not part of `ng test`).
//
// Journey (capstone brief, agent workflow): sign in as the AGENT demo user ->
// land on the home screen -> open customer lookup -> search "Amina" -> open
// CUS-1001 -> record an interaction -> see it on the timeline -> soft-delete it
// again so the run is repeatable.
//
// Prereqs: backend + PostgreSQL + Kafka running, and `npm run e2e:serve`
// (port 4300, proxies /api to CRM_E2E_API_TARGET). See e2e/README.md.
import assert from 'node:assert/strict';
import { after, before, test } from 'node:test';
import { Builder, By, until } from 'selenium-webdriver';
import chrome from 'selenium-webdriver/chrome.js';

const BASE_URL = (process.env.CRM_E2E_BASE_URL ?? 'http://localhost:4300').replace(/\/$/, '');
const USERNAME = process.env.CRM_E2E_USERNAME ?? 'agent';
// Default is the publicly-known throwaway value from application-local.yml, not a secret.
const PASSWORD = process.env.CRM_E2E_PASSWORD ?? 'agent-local-dev';
const HEADLESS = process.env.CRM_E2E_HEADLESS !== 'false';
const WAIT_MS = 15_000;

const byTestId = (id) => By.css(`[data-testid="${id}"]`);

let driver;

before(async () => {
  const options = new chrome.Options();
  if (HEADLESS) options.addArguments('--headless=new');
  options.addArguments('--window-size=1280,900', '--disable-gpu', '--no-sandbox');
  // Selenium Manager resolves a chromedriver matching the installed Chrome.
  driver = await new Builder().forBrowser('chrome').setChromeOptions(options).build();
});

after(async () => {
  await driver?.quit();
});

async function visible(testId) {
  const el = await driver.wait(until.elementLocated(byTestId(testId)), WAIT_MS);
  return driver.wait(until.elementIsVisible(el), WAIT_MS);
}

async function rowContaining(testId, text) {
  let match;
  await driver.wait(async () => {
    for (const row of await driver.findElements(byTestId(testId))) {
      if ((await row.getText()).includes(text)) {
        match = row;
        return true;
      }
    }
    return false;
  }, WAIT_MS, `no [data-testid=${testId}] containing "${text}"`);
  return match;
}

test('agent records an interaction for Amina and sees it on her timeline', async () => {
  const summary = `Selenium e2e ${new Date().toISOString()}`;

  // 1. Sign in (real JWT from POST /api/v1/auth/login).
  await driver.get(`${BASE_URL}/login`);
  await (await visible('login-username')).sendKeys(USERNAME);
  await (await visible('login-password')).sendKeys(PASSWORD);
  await (await visible('login-submit')).click();
  await driver.wait(until.urlContains('/home'), WAIT_MS);
  assert.match(await (await visible('signed-in-user')).getText(), /agent \(AGENT\)/);

  // 2. From the home screen, open customer lookup, search for Amina, open her profile.
  await (await visible('choice-lookup')).click();
  await driver.wait(until.urlContains('/customers'), WAIT_MS);
  const search = await visible('customer-search');
  await search.clear();
  await search.sendKeys('Amina');
  await (await visible('search-customers')).click();
  const aminaRow = await rowContaining('customer-row', 'CUS-1001');
  await aminaRow.findElement(byTestId('customer-profile')).click();
  await driver.wait(until.urlContains('/customers/CUS-1001'), WAIT_MS);
  assert.equal(await (await visible('profile-id')).getText(), 'CUS-1001');
  assert.equal(await (await visible('profile-name')).getText(), 'Amina Khan');
  // AGENT cannot close customers; the UI says so (the API enforces it with 403).
  assert.ok(await visible('close-requires-admin'));

  // 3. Record an interaction from the profile.
  await (await visible('record-interaction')).click();
  const summaryBox = await visible('create-summary');
  await summaryBox.clear();
  await summaryBox.sendKeys(summary);
  await (await visible('create')).click();
  await driver.wait(until.urlMatches(/\/interactions\/[0-9a-f-]{36}/), WAIT_MS);
  assert.equal(await (await visible('detail-customer')).getText(), 'CUS-1001');

  // 4. It appears on Amina's timeline.
  await (await visible('back-to-interactions')).click();
  const row = await rowContaining('interaction-row', summary);
  assert.match(await row.getText(), /NOTE/);

  // 5. Clean up: soft-delete it (audit row stays in PostgreSQL by design).
  await row.findElement(byTestId('delete-interaction')).click();
  await driver.wait(async () => {
    for (const r of await driver.findElements(byTestId('interaction-row'))) {
      if ((await r.getText()).includes(summary)) return false;
    }
    return true;
  }, WAIT_MS, 'smoke interaction was not removed from the timeline');
});
