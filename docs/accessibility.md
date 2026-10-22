# Accessibility

Checked the Angular screens in `frontend/src/app` on 21 Oct 2026: sign-in (including the show/hide password control), header, home, customer lookup, profile, create/update customer, interaction list, record interaction, interaction detail, error text, and the delete confetti.

Two checks. First, axe-core 4.10.3 in Chrome, signed in as the demo agent, against `ng serve` on port 4201 (`npm run a11y:pages`). Before the edits, every screen had one moderate violation: no level-one heading. The document title stayed "Northstar CRM". There was no skip link. The customer table had column headers with `scope`, and no caption. Inputs already had labels. Text colors already cleared WCAG AA (4.5:1). There were no serious or critical axe violations. After the edits, the same nine screens (login, home, lookup, new customer, profile, edit, interactions, record interaction, and one interaction detail) reported zero violations. Second, unit specs in `src/app/a11y.spec.ts` assert the headings, labels, table markup, error wiring, and save announcements, and run axe on those components.

What changed, without changing the demo sentences or `data-testid`s:

- Skip link, a `main` landmark, and a header list labeled "Primary". The current page is marked for assistive tech only.
- Each route sets its own document title, such as "Sign in | Northstar CRM". After a route change, focus moves to that page's heading.
- One `h1` per screen. Home cards and "Recent activity" are `h2`. Sizes match the old headings.
- Keyboard focus uses a dark outline. Empty field borders are darker so the box edge meets non-text contrast. Placeholder text uses the existing muted gray, which already passes.
- Failed sign-in, customer save, and interaction save set `aria-invalid` and `aria-describedby` on the fields and keep `role="alert"` on the message. "Changes saved." and a new "Note recorded." line use `role="status"`.
- Profile, Edit, Details, and Delete include the customer or note name for screen readers. The visible words are unchanged. The customer table has a caption.
- Record-interaction fields use `for`/`id` as well as the wrapping label. The password toggle still exposes Show/Hide, `aria-pressed`, and `aria-label`.
- Card hover motion is turned off when `prefers-reduced-motion` is set. Confetti already skipped that case; its canvas is now `aria-hidden`.

Not done: no pass with VoiceOver or a 200% zoom check. Back buttons, cards, and table rules kept their light borders so the demo looks the same; the text on those controls already has enough contrast. Required fields rely on the browser `required` state, with no new asterisks. Titles do not include the customer's name. The home choices stayed a grid rather than a list. Deleting an interaction still has no confirmation dialog.

`npm run a11y:pages` needs the app on port 4201 and Chrome. It does not run in `ng test`.
