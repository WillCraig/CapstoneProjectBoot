// Signs in as the demo agent and runs axe-core against the main screens.
// Start the app first (`npx ng serve --port 4201`). Not part of `ng test`.
import { writeFileSync } from 'node:fs';
import { createRequire } from 'node:module';
import { Builder, By, until } from 'selenium-webdriver';
import chrome from 'selenium-webdriver/chrome.js';

const require = createRequire(import.meta.url);
const axeSource = require('axe-core').source;

const base = (process.env.CRM_E2E_BASE_URL ?? 'http://127.0.0.1:4201').replace(/\/$/, '');
const username = process.env.CRM_E2E_USERNAME ?? 'agent';
const password = process.env.CRM_E2E_PASSWORD ?? 'agent-local-dev';

const options = new chrome.Options();
if (process.env.CRM_E2E_HEADLESS !== 'false') {
  options.addArguments('--headless=new');
}
options.addArguments('--window-size=1280,900', '--disable-gpu', '--no-sandbox');

const driver = await new Builder().forBrowser('chrome').setChromeOptions(options).build();
const pages = [];

async function scan(name) {
  await driver.executeScript(axeSource);
  const violations = await driver.executeAsyncScript(`
    const done = arguments[arguments.length - 1];
    axe.run(document, { resultTypes: ['violations'] }).then((results) => {
      done(results.violations.map((v) => ({
        id: v.id,
        impact: v.impact,
        help: v.help,
        nodes: v.nodes.slice(0, 12).map((n) => ({
          target: n.target.join(' '),
          html: (n.html || '').slice(0, 220),
          failure: (n.failureSummary || '').split('\\n')[0],
        })),
      })));
    }).catch((err) => done([{ id: 'axe-error', impact: 'critical', help: String(err), nodes: [] }]));
  `);
  const structure = await driver.executeScript(`
    return {
      title: document.title,
      headings: [...document.querySelectorAll('h1,h2,h3,h4')].map((el) =>
        el.tagName + ': ' + el.textContent.trim().replace(/\\s+/g, ' ').slice(0, 90)),
      skip: [...document.querySelectorAll('a')].some((a) => /skip to content/i.test(a.textContent)),
      navLabel: document.querySelector('nav')?.getAttribute('aria-label') ?? null,
      unlabeled: [...document.querySelectorAll('input, select, textarea')].filter((el) => {
        const id = el.id;
        const explicit = id && document.querySelector('label[for="' + CSS.escape(id) + '"]');
        return !(explicit || el.closest('label') || el.getAttribute('aria-label') || el.getAttribute('aria-labelledby'));
      }).map((el) => el.name || el.id || el.tagName),
      tables: [...document.querySelectorAll('table')].map((table) => ({
        caption: Boolean(table.querySelector('caption')),
        scopes: [...table.querySelectorAll('th')].map((th) => th.getAttribute('scope')),
      })),
    };
  `);
  return {
    name,
    url: await driver.getCurrentUrl(),
    structure,
    violations,
  };
}

try {
  await driver.get(`${base}/login`);
  await driver.wait(until.elementLocated(By.css('[data-testid="login-submit"]')), 15000);
  pages.push(await scan('login'));

  // Full page loads drop the in-memory token, so stay inside the Angular router.
  await driver.findElement(By.css('[data-testid="login-username"]')).sendKeys(username);
  await driver.findElement(By.css('[data-testid="login-password"]')).sendKeys(password);
  await driver.findElement(By.css('[data-testid="login-submit"]')).click();
  await driver.wait(until.urlContains('/home'), 15000);
  await driver.wait(until.elementLocated(By.css('[data-testid="choice-lookup"]')), 15000);
  pages.push(await scan('home'));

  await driver.findElement(By.css('[data-testid="choice-lookup"]')).click();
  await driver.wait(until.elementLocated(By.css('[data-testid="customer-search"]')), 15000);
  await driver.wait(
    until.elementLocated(By.css('[data-testid="customer-row"], [data-testid="empty-state"], [data-testid="list-error"]')),
    15000,
  );
  pages.push(await scan('customers'));

  await driver.findElement(By.css('[data-testid="new-customer"]')).click();
  await driver.wait(until.elementLocated(By.css('[data-testid="customer-name"]')), 15000);
  pages.push(await scan('customer-new'));

  await driver.navigate().back();
  await driver.wait(until.elementLocated(By.css('[data-testid="customer-search"]')), 15000);
  await driver.wait(
    until.elementLocated(By.css('[data-testid="customer-row"], [data-testid="empty-state"], [data-testid="list-error"]')),
    15000,
  );
  const rows = await driver.findElements(By.css('[data-testid="customer-row"]'));
  let openedProfile = false;
  for (const row of rows) {
    if ((await row.getText()).includes('CUS-1001')) {
      await row.findElement(By.css('[data-testid="customer-profile"]')).click();
      openedProfile = true;
      break;
    }
  }
  if (!openedProfile && rows.length) {
    await rows[0].findElement(By.css('[data-testid="customer-profile"]')).click();
    openedProfile = true;
  }
  if (openedProfile) {
    await driver.wait(until.elementLocated(By.css('[data-testid="profile-name"]')), 15000);
    pages.push(await scan('customer-profile'));
    await driver.findElement(By.css('[data-testid="edit-customer"]')).click();
    await driver.wait(until.elementLocated(By.css('[data-testid="customer-name"]')), 15000);
    pages.push(await scan('customer-edit'));
    await driver.navigate().back();
    await driver.wait(until.elementLocated(By.css('[data-testid="record-interaction"]')), 15000);
    await driver.findElement(By.css('[data-testid="record-interaction"]')).click();
    await driver.wait(until.elementLocated(By.css('[data-testid="create-summary"]')), 15000);
    pages.push(await scan('interaction-new'));
  } else {
    pages.push({ name: 'customer-profile', skipped: 'customer table was empty' });
  }

  await driver.findElement(By.css('nav a[href="/interactions"]')).click();
  await driver.wait(until.elementLocated(By.css('[data-testid="customer-id"]')), 15000);
  await driver.wait(
    until.elementLocated(By.css('[data-testid="interaction-row"], [data-testid="empty-state"], [data-testid="list-error"]')),
    15000,
  );
  pages.push(await scan('interactions'));

  let links = await driver.findElements(By.css('[data-testid="interaction-detail"]'));
  if (!links.length && process.env.CRM_AXE_CUSTOMER) {
    const input = await driver.findElement(By.css('[data-testid="customer-id"]'));
    await input.clear();
    await input.sendKeys(process.env.CRM_AXE_CUSTOMER);
    await driver.findElement(By.css('[data-testid="load-interactions"]')).click();
    const selected = await driver.findElement(By.css('[data-testid="selected-customer"]'));
    await driver.wait(until.elementTextContains(selected, process.env.CRM_AXE_CUSTOMER), 15000);
    await driver.wait(
      until.elementLocated(By.css('[data-testid="interaction-row"], [data-testid="empty-state"], [data-testid="list-error"]')),
      15000,
    );
    links = await driver.findElements(By.css('[data-testid="interaction-detail"]'));
  }
  if (links.length) {
    await links[0].click();
    await driver.wait(until.elementLocated(By.css('[data-testid="edit-summary"]')), 15000);
    pages.push(await scan('interaction-detail'));
  } else {
    pages.push({ name: 'interaction-detail', skipped: 'no interaction on the timeline' });
  }
} catch (error) {
  pages.push({ name: 'script-error', error: String(error && error.message ? error.message : error) });
} finally {
  await driver.quit();
}

const summary = pages.map((page) => ({
  name: page.name,
  url: page.url,
  skipped: page.skipped,
  error: page.error,
  title: page.structure?.title,
  headings: page.structure?.headings,
  skip: page.structure?.skip,
  navLabel: page.structure?.navLabel,
  unlabeled: page.structure?.unlabeled,
  tables: page.structure?.tables,
  violations: (page.violations ?? []).map((violation) => ({
    id: violation.id,
    impact: violation.impact,
    help: violation.help,
    targets: (violation.nodes ?? []).map((node) => node.target),
  })),
}));

const out = process.env.AXE_OUT ?? 'axe-report.json';
writeFileSync(out, JSON.stringify(summary, null, 2));
const serious = summary.reduce(
  (count, page) =>
    count +
    (page.violations ?? []).filter((violation) => violation.impact === 'serious' || violation.impact === 'critical')
      .length,
  0,
);
const violationCount = summary.reduce((count, page) => count + (page.violations?.length ?? 0), 0);
console.log(JSON.stringify(summary, null, 2));
console.error(`Wrote ${out}. Violations: ${violationCount}. Serious or critical rule hits: ${serious}.`);
process.exit(pages.some((page) => page.error) || violationCount > 0 ? 1 : 0);
