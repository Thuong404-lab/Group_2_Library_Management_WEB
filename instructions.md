# UI Modification Instructions

## Mandatory Source of Truth

`DESIGN-claude.md` is the ONLY design specification for this project. The filename and casing are exact.

- Always read `instructions.md` first and `DESIGN-claude.md` second, completely, before editing UI.
- Never substitute `design.md`, `CLAUDE_design.md`, screenshots from another branch, personal taste, Bootstrap defaults, or a generated interpretation.
- If an existing page conflicts with `DESIGN-claude.md`, the design file wins for presentation only; existing functionality still wins for behavior.
- If two design rules appear to conflict, the later **Application UI Canonical Format** and **Deterministic Implementation Contract** sections in `DESIGN-claude.md` take precedence over general marketing-style guidance.
- Equivalent Admin and Librarian pages MUST use the same shared CSS component/classes. Copying similar-looking CSS into two templates is not considered compliant.

## Codex Workspace-First Inspection

Before creating or changing UI, inspect the actual repository; never infer project structure from the open file. Use repository search, VS Code search, or `rg` to identify at minimum:

- the feature route/URL and its controller mapping;
- the controller-returned template and active layout;
- related Thymeleaf fragments, JavaScript, and every stylesheet loaded by the template or layout;
- an existing equivalent component and the equivalent page for another role;
- selectors, IDs, classes, `data-*`, `aria-*`, and Thymeleaf attributes used as behavior hooks.

Do not create a file merely because it is absent from the currently open editor. Verify the repository first.

## Existing Page and Workflow Ownership

One business workflow MUST have one primary page owner. Before creating HTML, determine whether the feature belongs to an existing business-domain page. The existing page has priority and MUST be extended with an appropriate section, card, toolbar, tab, accordion, modal, drawer, detail panel, conditional block, or Thymeleaf fragment.

POST, PUT, and DELETE endpoints do not require separate HTML pages. A new page is allowed only when no page owns the workflow, the workflow is clearly independent, the user explicitly requests a separate page, or the controller already returns a separate template that the task must edit. Different use cases, contributors, forms, modals, actions, or implementation convenience are not reasons to create separate pages.

The primary template remains the integration owner and defines fragment order. Use a fragment when a region is reused, has independent display logic, is a substantial modal/table/form/toolbar/empty state, or separates contributors' ownership and reduces conflicts. Do not mechanically extract a few simple, single-use lines that provide neither reuse nor ownership value.

## Existing Implementation Continuation

The current workspace is the source of truth for the active implementation. When a repository already contains an HTML/Thymeleaf template, layout, component, fragment, or CSS owner for the target business workflow, continue that implementation instead of creating an alternative.

### Existing HTML reuse

An existing workflow template is the canonical implementation of that workflow. For example, when `templates/librarian/dashboard.html` already owns Librarian Dashboard, every additional dashboard feature MUST continue in `dashboard.html` and be integrated through an appropriate section, card, modal, tab, accordion, drawer, or Thymeleaf fragment.

Do not create `dashboard-v2.html`, `dashboard-new.html`, `revenue-dashboard.html`, `dashboard-report.html`, or another template merely because a new feature is being added. A completely new page is permitted only when the repository establishes a separate workflow owner or the user explicitly requests one.

### Existing CSS reuse

When an existing page has a CSS owner, all subsequent presentation changes for that page family MUST continue in that owner. If `dashboard.html` is owned by `librarian-dashboard.css`, reuse that file and add rules in its appropriate section. Do not create, split, rename, or replace the owner merely because another feature is being added.

Reject competing names such as `dashboard-new.css`, `dashboard-v2.css`, `revenue-dashboard.css`, `dashboard-custom.css`, and `dashboard-style.css` when a suitable owner exists. Create CSS only after workspace inspection proves that no appropriate owner exists.

### Continue existing UI

If a page already follows `DESIGN-claude.md` and functions correctly, preserve its layout, component hierarchy, naming convention, CSS ownership, behavior hooks, and architecture. Extend only the UI required by the task. Do not redesign the whole page, substitute a new layout or implementation, or change ownership because another design appears preferable.

### Workspace implementation check

Before creating HTML, a fragment, or CSS, search the workspace and determine whether the workflow template, CSS owner, equivalent component, and corresponding layout already exist. If any applicable implementation exists, reuse and continue it. The open editor tab alone is not evidence that a repository implementation is absent.

### Feature extension rule

A new feature does not imply a new page. Features in the same business workflow belong to its existing primary page. For example, if Librarian Dashboard already contains Overview Cards, Statistics, and Quick Actions, then Revenue Widget, Recent Payments, Revenue Chart, and Membership Summary MUST be integrated into the same `dashboard.html`; they must not produce another dashboard implementation.

## Duplicate Page Prevention

Do not create differently named templates for the same business screen (for example `books.html`, `book-list.html`, `manage-books.html`, and `book-management.html`). Before creating a template, search by route, controller return value, business term, heading, fragment, form action, model attribute, and similar filename. Reuse or extend an equivalent page when found.

Never infer the active template from a URL or filename. Do not edit demos, backups, unused duplicates, or files suffixed `old`, `copy`, `backup`, or `temp`. If duplicates exist, edit only the template actually returned by the controller, report the duplication, and do not delete it without explicit scope.

## Deterministic Output Requirement

The goal is not merely “similar style.” The goal is the same component structure and measurable format regardless of which team member performs the work.

For equivalent screens, these values MUST match exactly:

- Page header structure, padding, radius, border, title hierarchy, and CTA placement
- Search/filter composition, control height, gaps, and button placement
- Card radius, border, shadow policy, padding, and background
- Table header, cell padding, row actions, badges, and empty state
- Pagination dimensions, colors, gaps, and active/disabled behavior
- Modal shell radius, clipping, header/body/footer backgrounds, and action alignment
- Responsive breakpoint behavior
- Visual frame ownership and horizontal-overflow ownership

“Close enough,” visually similar alternatives, and page-local variations are failures unless the business content genuinely requires a different component.

## Objective

Your task is to improve the user interface (UI) and user experience (UX) of this Library Management System by following the rules defined in `DESIGN-claude.md`.

The application is already functional. Your responsibility is ONLY to improve the presentation layer.

---

# Allowed Changes

You MAY modify:

- HTML structure
- Thymeleaf templates (.html)
- Bootstrap classes
- CSS files
- JavaScript related to UI interactions
- Icons (Bootstrap Icons, Font Awesome)
- Colors
- Typography
- Spacing
- Layout
- Responsive design
- Animations
- Card design
- Tables
- Forms
- Buttons
- Navigation bar
- Sidebar
- Footer
- Modals
- Toast notifications
- Empty states
- Loading indicators

---

# Strictly Forbidden

DO NOT modify any business logic.

Never edit:

- Controller
- Service
- Repository
- Entity
- DTO
- Mapper
- Security configuration
- Database schema
- SQL scripts
- API endpoints
- Routes
- Session logic
- Authentication
- Authorization
- Validation logic
- Payment logic
- Email logic
- Borrow/Return business rules

---

# Preserve Functionality

You MUST preserve all existing functionality.

Do NOT:

- Rename form field names
- Rename input IDs
- Rename element names used by JavaScript
- Change form action URLs
- Change HTTP methods
- Change controller mappings
- Remove Thymeleaf attributes
- Remove model bindings
- Remove hidden fields
- Remove CSRF tokens
- Break Bootstrap modals
- Break pagination
- Break sorting
- Break searching
- Break filtering

Every existing feature must continue working exactly as before.

---

# Thymeleaf Rules

Keep all Thymeleaf syntax intact.

Never remove or alter unless absolutely necessary:

- th:each
- th:if
- th:unless
- th:text
- th:value
- th:field
- th:href
- th:src
- th:action
- th:object
- th:replace
- th:insert
- th:fragment

If the HTML structure changes, preserve every Thymeleaf expression.

---

# JavaScript Rules

Only improve UI behavior.

Allowed:

- Sidebar toggle
- Dropdown animation
- Modal animation
- Toast animation
- Loading spinner
- Confirmation dialog
- Client-side visual effects

Do NOT change application logic.

Permission to change a file type is not permission to change every file of that type. The task-specific file scope below always takes precedence.

## Internal Pre-Implementation Decision Record

Before editing code, determine and internally record:

```text
Target business workflow:
Target URL:
Controller mapping:
Controller-returned template:
Existing primary page:
Need a new page: yes/no
Reason a new page is necessary:
Existing fragment/component:
Existing CSS owner:
Create new CSS: yes/no
Embedded CSS found:
Inline style found:
CSS migration required:
Files allowed to modify:
Files explicitly excluded:
```

`Need a new page: yes` requires a repository-backed or user-specified reason. If an existing CSS owner exists, `Create new CSS` MUST be `no`.

## Codex Minimal Diff Rule

Keep every UI diff surgical for safe team merges. Do not reformat or re-indent an entire file, reorder unrelated blocks, rename unrelated classes, mass-change quote style or line endings, edit unrelated comments, sort whole stylesheets, clean up out-of-scope components, or move files for architectural preference. Do not modify shared layouts, sidebar, header, footer, navigation fragments, global CSS, or shared JavaScript for one page unless the request targets that shared contract or the page cannot function without the narrowly justified change.

## Mandatory UI File-Scope Manifest

Before editing, identify and internally record:

```text
Target URL:
Controller-returned template:
Page-family root class:
Canonical stylesheet owner:
Allowed files to modify:
Explicitly excluded files:
Reason for any scope expansion:
```

### Default scope: maximum two implementation files

For a normal request to restyle one page, modify only:

1. the actual controller-returned Thymeleaf template; and
2. one canonical stylesheet owner for that page family.

Use this exact CSS-owner decision tree:

1. Reuse the page-family stylesheet already linked by the target template.
2. If none exists, reuse the existing shared family stylesheet linked by an equivalent updated page.
3. If neither exists, create exactly one `src/main/resources/static/css/{page-family}.css` and link it from the target template.

Never create or choose `app-components.css`, `global.css`, `common.css`, a role-prefixed stylesheet, or another broad shared file during a single-page task unless `DESIGN-claude.md` already names that exact file as owner or the user explicitly requests cross-page migration.

Once a family stylesheet exists, always reuse it. Do not create a competing second stylesheet and do not relocate its rules during an unrelated task.

### Excluded by default

Do not modify these during a presentation-only page task:

- Java controllers, services, repositories, entities, DTOs, exceptions, configuration;
- base layouts, navigation fragments, unrelated templates;
- CSS owned by another page family;
- external JavaScript unless interaction behavior is explicitly in scope;
- dependencies, properties, SQL, tests, generated/build output, images/assets;
- `DESIGN-claude.md` or `instructions.md` unless the user explicitly asks to update rules.

### Third-file gate

An additional file is allowed only when the requested page cannot work correctly without it. Before touching it:

1. identify the exact dependency;
2. explain why the template and canonical stylesheet are insufficient;
3. name the additional file and its owner responsibility;
4. limit the edit to that dependency.

Do not expand scope for cleanup, architectural preference, future reuse, or because another file is convenient.

### Changed-file enforcement

At completion, compare `git status`/the changed-file list with the manifest. Every modified or newly created file must be declared and directly related to the request. Undeclared changes must be reverted or explicitly justified before reporting completion.

## Mandatory Legacy CSS Extraction Procedure

The project already contains functional templates with `<style>` blocks and inline presentation. When a page/component is selected for UI work, separate its presentation while preserving behavior.

### Mandatory HTML embedded CSS inspection

Whenever a task touches HTML or a Thymeleaf template, inspect the entire template for `<style>`, `style="..."`, `th:style`, JavaScript `element.style`, CSS variables declared in markup, complex presentation workarounds made from Bootstrap utilities, and presentation duplicated between HTML and external CSS. This inspection is mandatory even for a small component task, but migration remains limited to the page/component in scope.

### Mandatory CSS owner search

Before creating CSS, search in this order: the stylesheet linked by the target template; stylesheets loaded by its layout; the stylesheet family used by an equivalent page; files named for the business domain; files containing the root/canonical classes; and shared styles for the equivalent role. Reuse the first appropriate owner and add rules to its correct section. Create exactly one new family stylesheet only after repository inspection proves no appropriate owner exists.

### Single CSS owner and no duplication

Each page family or component family has one visual CSS owner. Do not create competing near-synonym files, redefine a selector across loaded stylesheets, create synonymous classes, copy shared rules into page CSS, or add specificity/`!important` merely to conceal ownership conflict. When duplicate definitions already exist, identify and edit only the canonical owner within task scope; do not perform an unrequested project-wide migration. Never rename, split, or relocate the owner during an unrelated task.

### Directory and naming standard

```text
templates/{role}/{page}.html → structure, Thymeleaf, form/data/JS hooks
static/css/{family}.css      → presentation owned by that page family
```

Name stylesheet owners deterministically:

1. Equivalent cross-role pages: `{domain-family}.css` such as `member-management.css`.
2. Unique feature family: `{feature}.css` such as `return-desk.css` or `borrow-counter.css`.
3. Detail-only additions: `{feature}-detail.css`, loaded after the parent family CSS.
4. Use a role prefix only for genuinely non-equivalent filename collisions; document why.

Never use vague names: `style.css`, `styles.css`, `custom.css`, `new.css`, `temp.css`, `fix.css`, `page.css`, `common.css`, or an unrequested global/app-components file.

Every new CSS owner starts with:

```css
/*
 * Page family: <human-readable family>
 * Templates: templates/<role>/<page>.html
 * Routes: <route list>
 * Root scope: .<family>-page
 */
```

### HTML/CSS responsibility split

HTML keeps:

- semantic DOM and canonical wrappers;
- Thymeleaf conditions, loops, expressions, fragments, and field bindings;
- content and accessibility attributes;
- form actions/methods/names/values, CSRF, validation;
- IDs/classes/data attributes used by scripts, Bootstrap, labels, tests, and navigation;
- `<link>` and script references.

CSS owns:

- colors, typography, dimensions, spacing, alignment;
- border, radius, background, shadow, opacity;
- grid/flex/table layout and overflow;
- responsive breakpoints;
- hover, focus, active, selected, disabled, valid, invalid, loading, and empty visuals.

Do not replace inline CSS with dozens of arbitrary Bootstrap utilities. Replace it with meaningful canonical/domain classes.

Replace finite-state `th:style` with `th:classappend` semantic classes. Direct JavaScript `element.style` is allowed only for a calculated/transient value that cannot be represented by class toggling, and the reason must be documented.

### Behavior lock: never alter during UI-only work

Preserve exactly:

- routes, form actions/methods, query parameters, field names, hidden fields, CSRF;
- all `th:field`, `th:name`, `th:value`, `th:checked`, `th:selected`, conditions, loops, expressions;
- IDs referenced by labels, JavaScript, tabs, modals, tests, and deep links;
- `data-bs-*`, `aria-*`, validation attributes, button/submit types;
- form/modal ownership, event hooks, pagination/filter parameters;
- business data, status mapping, permission conditions, and defaults.

Presentation wrappers may change only after checking they are not behavior hooks. If behavior or Java must change, stop and request explicit scope expansion.

### Required extraction steps

1. Identify the route/controller template, CSS owner, scripts, and behavior hooks.
2. Inventory forms, IDs, names, Thymeleaf/data attributes, modals, tabs, and event listeners.
3. Reuse or create exactly one correctly named stylesheet owner.
4. Add one page-family root scope.
5. Move the relevant `<style>` rules into the owner CSS.
6. Replace inline presentation with meaningful classes component-by-component.
7. Apply canonical visual rules without changing behavior inventory.
8. Delete migrated inline/style-block CSS; never keep duplicate fallback rules.
9. Test all conditional/runtime states and existing interactions.
10. Compare actual changed files with the scope manifest.

### Separation acceptance gate

For every edited component, require all applicable checks:

- no `<style>` block remains for that component;
- no literal inline presentation remains;
- no finite-state `th:style` remains when classes can represent it;
- no CSS duplication across template, owner, and other stylesheets;
- owner CSS link loads successfully in correct order;
- functionality and behavior-lock inventory are unchanged;
- default, conditional, validation, modal, tab, empty, and responsive states remain functional.

Do not migrate unrelated legacy components merely to clean the whole repository. Separation proceeds page/component by page/component under the normal file-scope manifest.

---

# CSS Rules

Follow exact file `DESIGN-claude.md`, especially its **Application UI Canonical Format**.

Prefer:

- Bootstrap utilities
- CSS variables
- Reusable classes

Do not add inline presentation styles to edited components. Move presentation into the applicable shared stylesheet.

---

# Design Consistency

Every page should share the same design language.

Consistency means shared implementation, not repeated approximation:

- Search the repository for an already-updated equivalent page before writing CSS.
- Reuse its shared stylesheet and canonical classes.
- Do not create a new page-specific token namespace when a shared component stylesheet already exists.
- Do not change the global sidebar/topbar/layout state while styling page content unless the task explicitly targets the layout.
- Sidebar collapsed/expanded state is interactive state and must not be baked into a page design.

Maintain consistent:

- Color palette
- Typography
- Border radius
- Shadows
- Button styles
- Form styles
- Table styles
- Card styles
- Icons
- Spacing
- Animations

## Canonical Color, Overlay, and Feedback Contract

All authenticated and guest UI MUST use the semantic tokens and canonical overlay components in `DESIGN-claude.md`. Bootstrap contextual colors, browser defaults, copied hex values, and role-specific palettes are not design authority.

### Color ownership and semantic use

- Search the target template, its layout, and every loaded stylesheet before adding a color. Reuse the canonical token/class owner; do not paste the same hex into another page stylesheet.
- Use canvas/surface tokens only for hierarchy, ink/body/muted tokens only for text hierarchy, application-primary only for the main authenticated create/confirm action, and semantic tokens only for their named meaning.
- Success means a completed/available/valid state; warning means attention/pending; danger means destructive/error/overdue; info means neutral guidance or informational status. A business category, role, table column, or decorative card MUST NOT receive a semantic color merely to look distinct.
- Never communicate meaning by color alone. Pair semantic color with text and, where the canonical component requires it, an icon. Preserve at least 4.5:1 contrast for normal text and 3:1 for large text, borders, icons, and focus indicators.
- Do not use `bg-*`, `text-*`, `border-*`, inline hex/RGB/HSL, or opacity utilities inside an edited canonical component when a design token/class exists. Do not create lighter/darker colors with CSS filters or arbitrary alpha; use the documented soft, border, ink, hover, active, disabled, overlay, and focus tokens.
- Hover, focus, active, selected, loading, and disabled states MUST use the documented state token and MUST NOT change component geometry, typography, or surrounding layout.

### Mandatory token-only color enforcement

`DESIGN-claude.md` is the only authority for UI color. A color is compliant only when its semantic token is reused through the canonical CSS owner; writing the same hex value again in another selector is still duplication and is not compliant.

- Before editing, inventory every foreground, background, border, shadow, focus ring, icon, SVG fill/stroke, overlay, placeholder, and state color used by the target component. Map each one to an existing `{colors.*}` token.
- In CSS implementation, expose the approved design colors once through the established shared custom properties/component classes. Page-family CSS consumes those properties/classes; it MUST NOT declare a second page-local palette or redefine the same token under another name.
- New raw `#hex`, `rgb()`, `rgba()`, `hsl()`, `hsla()`, named colors, gradients, `currentColor` overrides, SVG `fill`/`stroke`, or Bootstrap contextual color utilities are forbidden in an edited component unless they are the single canonical token declaration in the authorized design-token owner.
- Do not create synonyms such as `--admin-brown`, `--librarian-primary`, `--book-accent`, or `--custom-danger`. Equivalent components across Admin, Librarian, Member, and Guest consume the same canonical token. Role and business domain never create a new palette.
- Do not derive undocumented colors with `color-mix()`, filters, opacity, alpha overlays, or preprocessor functions. If a required interaction/state color is missing, update `DESIGN-claude.md` deliberately and add exactly one named token before using it.
- Transparent is allowed only where the canonical component explicitly requires no painted surface. `inherit` and `currentColor` are allowed only when the parent/component already owns the documented token and the result is verified in computed styles.
- Images, book covers, user-uploaded media, publisher logos, and official brand assets may retain source colors. Their containers, borders, controls, captions, fallbacks, overlays, and surrounding UI still use design tokens. Do not sample media colors into application UI.
- Data visualization series may add colors only through a documented chart palette in `DESIGN-claude.md`; arbitrary per-chart colors are forbidden. Semantic success/warning/danger colors must not be reused merely to distinguish unrelated chart series.

For every edited UI, run a color audit over the target template and every loaded owner stylesheet. Any raw color or contextual utility found in the edited component must be either migrated to the canonical token/class or reported as an explicitly out-of-scope legacy issue. Do not approve by visual similarity alone; verify browser computed `color`, `background-color`, `border-color`, `box-shadow`, `fill`, `stroke`, and focus-ring values.

### Mandatory popup inventory

Whenever a task touches a modal, confirmation dialog, toast, tooltip, popover, dropdown, offcanvas, or loading overlay, inspect all equivalent overlays in the repository and record internally:

```text
Overlay type:
Trigger and close behavior:
Canonical component/class owner:
Size variant:
Backdrop and z-index owner:
Header/body/footer structure:
Primary/secondary/destructive action order:
Focus-return target:
Responsive behavior:
Existing inline/contextual presentation to migrate:
```

Do not use a legacy popup with inline style, `bg-danger`, `bg-white`, `bg-light`, arbitrary shadow/radius, or Bootstrap default color as a visual reference. Preserve its behavior, bindings, IDs, and events while migrating presentation to the canonical owner when that popup is in scope.

### Canonical modal and confirmation dialog

- Use one Bootstrap modal DOM contract: `.modal > .modal-dialog > .modal-content > (.modal-header + .modal-body + optional .modal-footer)`. Do not nest modal shells or place a modal inside a horizontally scrolling table owner when it can be rendered at the page/fragment overlay level.
- Size variants are exact: small confirmation `400px`, default form/detail `560px`, large data/detail `800px`; maximum width is `calc(100vw - 32px)`. Do not invent page-specific widths. Use the smallest variant that fits the content without horizontal scrolling.
- `.modal-content` owns the single frame: canvas background, 12px radius, 1px hairline border, canonical overlay shadow, and clipped corners. Header/body/footer MUST NOT add competing outer radius or shadow.
- Header and footer use surface-soft; body uses canvas. Header/body/footer padding is 20px/24px/20px on desktop and 16px on mobile. Dividers use hairline-soft. Do not use a solid red/green/blue header for semantic dialogs.
- Modal title follows the typography matrix. The close control is a canonical 36×36px icon button at the header end with an accessible label. It MUST NOT use a decorative colored circle or move between modal types.
- Footer actions are right-aligned in one non-wrapping row with 8px gap. Order is secondary/cancel first, primary/confirm last. For destructive confirmation, cancel remains secondary and the final action alone uses the destructive variant. Do not color both actions as destructive.
- Confirmation dialogs contain a concise title, explicit consequence in the body, and action labels that name the operation (`Xóa`, `Hủy`, `Từ chối`) rather than a vague `OK`. Do not rely on `window.alert()` or `window.confirm()` when the application already has a canonical modal mechanism.
- Opening traps focus within the modal; Escape and backdrop close only when business behavior permits. Closing returns focus to the trigger. Preserve `aria-labelledby`, `aria-describedby` when useful, `aria-hidden`, and existing static-backdrop/business constraints.
- Only `.modal-body` may scroll vertically. The header and footer remain visible; the page behind the modal does not scroll. No modal content may require horizontal scrolling at supported viewports.

### Toasts, tooltips, dropdowns, and other overlays

- Toasts use one canonical top-right stack on desktop and a 16px left/right inset full-width stack on mobile. Width is 360px maximum, canvas surface, 12px radius, hairline border, canonical shadow, 16px padding, and a 4px semantic leading accent. Do not create a different toast position or color block per page.
- Success/error/warning/info toast variants change only the semantic accent/icon and message; geometry and typography remain identical. Toasts must be dismissible, must not cover navigation or modal actions, and must pause dismissal while hovered or keyboard-focused when the implementation supports timeout.
- Tooltips are for short accessible labels only: dark surface, on-dark text, 12px/500, 6px radius, maximum width 240px. Do not use role-specific brown/red tooltip backgrounds or put essential instructions only in a tooltip.
- Dropdown/popover surfaces use canvas, 8px radius, hairline border, canonical shadow, 8px internal padding, and 40px minimum item height. Destructive menu items use danger ink only; the entire menu does not become red.
- Offcanvas/drawer surfaces follow modal colors and spacing, use one backdrop, trap/restore focus, and have one vertical scroll owner. Loading overlays use the standard backdrop and progress indicator without replacing the page palette.
- Overlay stacking order is owned centrally: dropdown/tooltip/popover below modal, modal backdrop below modal content, toast above ordinary page content but never above an active modal requiring action. Do not add arbitrary `z-index:9999` or create a new stacking scale in page CSS.
- Motion uses the canonical 150–200ms ease transition and respects `prefers-reduced-motion: reduce`; reduced motion removes translation/scale while preserving visibility state.

---

# Responsive Design

Support:

- Desktop
- Laptop
- Tablet
- Mobile

Do not break existing layouts.

---

# Accessibility

Improve accessibility whenever possible.

Include:

- aria-label where appropriate
- Proper button semantics
- Proper form labels
- Keyboard accessibility
- Color contrast

---

# Performance

Avoid unnecessary libraries.

Reuse existing Bootstrap components whenever possible.

Do not introduce heavy UI frameworks.

---

# Before Editing

## Route and Template Verification

Verify the controller mapping and actual returned template. Do not infer either from the URL or filename. Preserve and inventory all form actions, methods, CSRF/hidden fields, validation, model bindings, JavaScript hooks, selectors, IDs, classes, `data-*`, `aria-*`, and Thymeleaf attributes before changing structure.

## Existing Component First

Search the repository before creating a page header, search bar, toolbar, data table, pagination, modal, alert, badge, row action, empty/loading state, card, form group, validation message, or confirmation dialog. Reuse the equivalent component's DOM, canonical classes, spacing/size contract, interaction, and responsive behavior. Change only business text, data/bindings, routes/form actions, and permission conditions. Do not introduce a visual variant because it appears preferable.

Equivalent Admin, Librarian, and Member features MUST share the same component structure, CSS owner, and visual contract when behavior is equivalent. Role differences belong only to data, permissions, allowed actions, content, routes, and business state.

Always:

1. Read `instructions.md` and exact file `DESIGN-claude.md` completely.
2. Identify the actual template returned by the controller; do not edit unused placeholders.
3. Find equivalent updated pages for other roles and identify their shared stylesheet/classes.
4. Inventory every repeated component on the target page: header, CTA, search/filter, table, actions, pagination, modal, alert, and empty state.
5. Map each component to the canonical format in `DESIGN-claude.md` before editing.
6. Keep existing functionality and apply only UI improvements.

## Implementation Rules for Consistent Output

- Add one page-family root class to scope styles.
- Use the canonical class names from `DESIGN-claude.md`; do not invent synonyms.
- Use a shared static CSS file for any component used by more than one equivalent page or role.
- Templates should contain structure, content, Bootstrap layout utilities, and Thymeleaf—not large duplicate `<style>` blocks.
- Do not use inline `style` for color, spacing, radius, shadow, width, or button alignment. Existing inline presentation styles should be migrated to the shared class when that component is edited.
- Do not use arbitrary spacing utilities (`ms-*`, one-off margin, custom gap) to compensate for component structure. The parent component owns spacing.
- Do not add decorative icons. Follow the exact action icon mapping in `DESIGN-claude.md`.
- Do not create a new variant because it “looks better.” Only documented variants are allowed.
- When an equivalent component already exists, copy its DOM structure and class list, then change only text, Thymeleaf bindings, URLs, and permission-dependent actions.
- Before adding a border/radius/background, identify the component's single **visual shell owner**. A child inside an already-framed component must not create another full-size frame.
- Before adding `overflow`, identify the component's single **overflow owner**. For data tables this is `.app-table-wrap`; parent cards, sections, and table cells must not create competing scroll containers.
- Every flex/grid/card/table ancestor in an edited data region must be checked for `min-width: 0`, `width: 100%`, and `max-width: 100%` as applicable.
- Do not use fixed/min widths on action columns or forms unless the total width budget is proven to fit the content container at the target breakpoint.

## Mandatory Component Size Matrix

Use these exact computed measurements. Do not reinterpret them per page or role.

| Component | Required size | Required internals |
|---|---:|---|
| Standard text button | 40px height | 0 18px padding, 14px/600, 8px radius |
| Search/filter input | 42px height | 0 14px padding, 14px, 8px radius |
| Compact text button | 36px height | 0 14px padding, 13px/600, 8px radius |
| View/Edit/Delete icon button | 36×36px | 0 padding, 16px icon, line-height 1, pill radius |
| Status/tier badge | 24px minimum height | 5px 11px padding, 12px/600, line-height 1, pill radius |
| Pagination item | 40×40px minimum | 0 12px padding, 14px/500, 8px radius |
| Modal footer text button | 40px height | identical to standard text button |

Rules:

- Use `box-sizing:border-box`; declared heights include borders.
- Equivalent links and buttons use the same class and computed dimensions.
- Do not use `btn-sm`, `btn-lg`, `px-*`, `py-*`, `fs-*`, inline dimensions, or page-local sizing overrides on canonical controls.
- Text buttons use centered inline-flex and `line-height:1`.
- View=`bi-eye`, Edit=`bi-pencil-square`, Delete=`bi-trash`; each icon is 16px with no margin.
- Badge state may change only semantic color. Badge size, padding, font, radius, and line height remain identical.

## Canonical Row Actions and Primary Add Button Contract

Use the current Admin Book Inventory implementation (`/admin/books`, `admin/books.html`) as the locked application reference for table actions and page-level create actions. Equivalent pages MUST reuse the same DOM order, classes, measurements, colors, placement, and interaction states; do not reinterpret them per role or business domain.

### View, edit, and delete row actions

The canonical order is always View → Edit → Delete when all three actions are permitted:

```html
<div class="app-row-actions">
  <button class="app-icon-action app-icon-action--view" title="Xem chi tiết" aria-label="Xem chi tiết">
    <i class="bi bi-eye" aria-hidden="true"></i>
  </button>
  <button class="app-icon-action app-icon-action--edit" title="Sửa" aria-label="Sửa">
    <i class="bi bi-pencil-square" aria-hidden="true"></i>
  </button>
  <button class="app-icon-action app-icon-action--delete" title="Xóa" aria-label="Xóa">
    <i class="bi bi-trash" aria-hidden="true"></i>
  </button>
</div>
```

- `.app-row-actions` is centered, non-wrapping, full-width, and uses an exact 8px gap.
- Every `.app-icon-action` is exactly 36×36px, `flex:0 0 36px`, zero padding, a 1px hairline border, pill radius, and a centered 16px icon with no margin.
- Default surface is canvas `#faf9f5` with hairline `#e6dfd8`. View and Edit use ink `#141413`; Delete alone uses danger `#c64545`. Do not use Bootstrap contextual button fills, colored circles, text labels, or role-specific colors.
- Hover for an enabled action uses border `#cc785c`, surface `#f5f0e8`, and foreground `#a9583e`. Apply a short `150ms ease` transition to background-color, border-color, color, and transform; hover may translate upward by at most 1px and MUST NOT change dimensions or layout.
- Focus-visible uses a 3px `rgba(204,120,92,.20)` outer ring with no geometry change. Active/pressed removes the lift and uses surface `#efe9de`. Disabled actions remain in place, use muted `#8e8b82`, and have no hover lift.
- Keep semantic `title` and `aria-label`; use native `<button>` for modal/form actions and `<a>` only for navigation. Preserve all `data-*`, form, Thymeleaf, permission, and JavaScript hooks.
- If a role exposes fewer actions, omit unauthorized actions without placeholders and select the documented one/two/three-action column width.

### Primary add/create action

The canonical page-level create action follows the Admin Book Inventory “Thêm sách mới” button:

```html
<div class="app-table-toolbar">
  <h5 class="app-section-title">...</h5>
  <button type="button" class="btn btn-classic app-primary-action">Thêm ... mới</button>
</div>
```

- `.app-table-toolbar` is one flex row with `align-items:center`, `justify-content:space-between`, 16px gap, and 16px bottom margin. The section title stays left and the single primary create action stays right on desktop.
- `.app-primary-action` is centered inline-flex, exactly 40px high, content-width with `0 18px` padding, 8px radius, `14px/600`, line-height 1, and no wrapping. Do not use `btn-sm`, `btn-lg`, arbitrary width, margin utilities, or inline positioning.
- Default background is application brown `#8b5a2b` with text `#fdfbf7`; hover/focus background is `#6b4423` with the same text color. Use a `150ms ease` color/background/transform transition; hover may lift by at most 1px without changing size. Active/pressed removes the lift and uses `#58371d`. Focus-visible adds a 3px `rgba(139,90,43,.22)` ring. Disabled uses `#c9b8a7`, keeps light text, has no lift, and communicates disabled state semantically.
- At widths below 768px, the toolbar stacks and `.app-primary-action` becomes `width:100%`; it remains 40px high. Do not keep it floating right or force a desktop fixed pixel width on mobile.
- Every equivalent list/management page uses this placement and geometry for its primary “Thêm/Tạo mới” action. Change only label, icon policy, permission condition, and behavior binding.
- A local page may use different colors only when `DESIGN-claude.md` explicitly defines a semantic/destructive variant; it may not invent another primary-create color.

### Required row-action column widths

`.app-row-actions` uses 8px gap, no wrapping, and these exact widths:

| Visible 36px icon buttons | `--app-action-column-width` |
|---:|---:|
| 1 | 72px |
| 2 | 88px |
| 3 | 132px |

Apply `.app-action-column` to both `<th>` and `<td>`. Select the documented width from the number of actions actually visible for that role; do not render invisible placeholders or invent another width. Include this width in the table budget.

## Mandatory Application Typography Matrix

Use these exact computed font values on authenticated application pages:

| Text role | Family | Size | Weight | Line-height | Extra |
|---|---|---:|---:|---:|---|
| Page title | Display serif | `clamp(30px, 3vw, 40px)` | 400 | 1.15 | `-.02em` tracking |
| Eyebrow | Sans | 12px | 600 | 1.4 | uppercase, 1.5px tracking |
| Page description | Sans | 16px | 400 | 1.5 | muted |
| Sidebar brand/product name | Display serif | 20px | 500 | 1.2 | no uppercase, no synthetic bold |
| Sidebar/navigation item | Sans | 14px | 500 | 1.4 | active state changes color/background, not weight |
| Breadcrumb | Sans | 13px | 500 | 1.4 | current item ink, ancestors muted |
| Tab/filter label | Sans | 14px | 600 | 1.4 | no size/weight change between states |
| Card/section title | Display serif | 20px | 500 | 1.3 | no automatic uppercase |
| Subsection title | Sans | 14px | 600 | 1.4 | uppercase only if specified |
| Dashboard statistic value | Display serif | 28px | 500 | 1.15 | tabular numerals when values must align |
| Dashboard statistic label | Sans | 13px | 600 | 1.4 | muted; uppercase only if canonical |
| Body/default | Sans | 14px | 400 | 1.5 | body/ink color |
| Form label | Sans | 14px | 600 | 1.4 | 8px before control |
| Input/select/textarea | Sans | 14px | 400 | 1.4 | placeholder same size |
| Standard text button | Sans | 14px | 600 | 1 | centered |
| Compact text button | Sans | 13px | 600 | 1 | documented variant only |
| Icon-only action | Sans/icon font | 16px | 400 | 1 | icon only; accessible label is not visual text |
| Dropdown/menu item | Sans | 14px | 500 | 1.4 | state changes color/background only |
| Table header | Sans | 12px | 600 | 1.4 | uppercase, `.08em` tracking |
| Table body | Sans | 14px | 400 | 1.5 | primary value may use 600 |
| Secondary/helper | Sans | 13px | 400 | 1.4 | muted |
| Validation/error | Sans | 12px | 500 | 1.4 | danger color |
| Status/tier badge | Sans | 12px | 600 | 1 | nowrap by default |
| Pagination | Sans | 14px | 500 | 1 | state changes color only |
| Tooltip | Sans | 12px | 500 | 1.4 | short sentence case label |
| Modal title | Display serif | 20px | 500 | 1.3 | no decorative icon |
| Modal/body | Sans | 14px | 400 | 1.5 | normal content |
| Toast title | Sans | 14px | 600 | 1.4 | semantic color does not alter weight |
| Toast/body | Sans | 13px | 400 | 1.4 | normal sentence case |
| Alert | Sans | 14px | 500 | 1.5 | semantic color |
| Empty-state message | Sans | 14px | 400 | 1.5 | muted |

Enforcement:

- Use one display stack and one UI stack across the application. Display serif: `Copernicus, "Tiempos Headline", "Playfair Display", "Cormorant Garamond", Garamond, serif`. UI sans: `StyreneB, Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif`. Code/data requiring monospace uses `"JetBrains Mono", ui-monospace, monospace` only.
- Display serif is reserved for page titles, card/section titles, modal titles, brand names, and prominent dashboard values explicitly listed above. Navigation, sidebar items, tabs, buttons, forms, tables, badges, breadcrumbs, alerts, tooltips, pagination, helper text, and ordinary body content MUST use the UI sans stack.
- Do not choose fonts per page or role. Admin, Librarian, Member, Guest, shared layouts, modals, and fragments use the same stack for the same text role.
- Canonical classes own typography. Do not rely on default `h1`–`h6`, `<small>`, `<strong>`, table, input, or button styling.
- Do not use Bootstrap `fs-*`, `.small`, `.lead`, display classes, inline `font-size`, percentages, or arbitrary `rem` sizes inside an edited canonical component.
- Do not use `fw-*` to override the documented weight.
- The same text role must have identical computed values across roles and pages.
- Interaction states (`hover`, `focus`, `active`, `selected`, `disabled`) MUST NOT change font family, font size, font weight, line-height, letter-spacing, or text casing. Emphasis comes from the documented color, background, border, or underline state.
- Vietnamese text MUST retain full diacritics and must be tested with representative long labels. Do not reduce font size, letter-spacing, or line-height to make Vietnamese content fit.
- Avoid synthetic bold/italic. Use only documented weights that the loaded font actually provides; when unavailable, use the approved fallback at the same role and weight.
- Placeholder text remains 14px/400/1.4; only its color is muted.
- Verify intended fonts loaded in the browser. Fallback-font geometry is not an acceptable final result.
- Responsive rules must not shrink body, controls, tables, buttons, badges, helpers, or modal text. Only the page title scales through `clamp()`.
- Never reduce font size to solve wrapping or overflow; fix the column/layout owner instead.

## No Nested Frame / No Overflow Contract

This contract is mandatory for every edited component:

1. One data region has exactly one visible outer border and one outer radius.
2. If `.app-card` owns the visible frame, nested `.app-table-wrap` is borderless and does not add another radius/background.
3. If a standalone `.app-table-wrap` owns the visible frame, do not wrap it in a visually framed `.app-card`.
4. Never nest `.card-classic`, `.app-card`, `.table-responsive`, or another bordered shell solely for appearance.
5. Only `.app-table-wrap` may use `overflow-x: auto` for a table. Ancestors use `overflow: visible` unless clipping is explicitly required by a modal shell.
6. Parent flex/grid items use `min-width: 0`; region shells use `width: 100%; max-width: 100%; box-sizing: border-box`.
7. Inputs, textareas, forms, and buttons inside a table cell use `max-width: 100%; box-sizing: border-box`.
8. A desktop layout at 1024px or wider must not show a horizontal scrollbar for ordinary application data. Horizontal table scrolling is a mobile/tablet fallback, not a desktop layout strategy.
9. Do not solve overflow by hiding content with `overflow: hidden`. Fix the width source; use clipping only for modal corner rendering or deliberate media crops.
10. Inspect at 100% browser zoom before completion. Browser zoom or a collapsed sidebar must not be required for the page to fit.

## Form Action Row Contract

Forms with a primary text action and a secondary icon action MUST use one canonical visual action row. Equivalent forms may not place the icon below, above, or outside that row.

- Use exact visual wrapper class `.app-form-actions` immediately after the final input/textarea/error message.
- `.app-form-actions` is one horizontal, non-wrapping row: `display:flex`, `align-items:center`, `flex-wrap:nowrap`, gap 8px, width 100%.
- The primary text button uses `.app-form-primary`: `flex:1 1 auto`, `min-width:0`, height 40px.
- The icon action uses `.app-icon-action`: `flex:0 0 36px`, width/height 36px.
- Do not make the delete form itself a visible block below the reply/update form.
- HTML forms cannot be nested. When actions submit different forms, keep the secondary owning form outside the primary form, give it a stable unique `id`, and place its visible submit button inside `.app-form-actions` using the HTML `form="..."` attribute.
- The external owning form contains hidden/CSRF fields only and MUST use `.app-detached-form`, which has `display:none` and no layout dimensions.
- Never rely on DOM adjacency, default form display, margins, floats, absolute positioning, or `flex-wrap` to align related actions.
- At desktop and mobile widths, the primary button and icon button remain on the same row. The primary button shrinks; the 36px icon button does not.
- If there is no primary action for a row, `.app-form-actions` aligns the icon action to the right; do not leave an empty primary button placeholder.

## Loan Status and Table Action Contract

This contract is mandatory and must be copied without visual reinterpretation on every Admin/Librarian loan table.

### Exact loan-status mapping

| Normalized status | Label | Required class | Required tokens |
|---|---|---|---|
| `PENDING` | Chờ duyệt | `status-badge status-pending` | warning-soft / warning-ink |
| `RENEW_PENDING` | Chờ gia hạn | `status-badge status-renew-pending` | warning-soft / warning-ink |
| `RETURN_PENDING` | Xin trả sách | `status-badge status-return-pending` | warning-soft / warning-ink |
| `BORROWED` | Đang mượn | `status-badge status-borrowed` | surface-card / ink |
| `OVERDUE` | Quá hạn | `status-badge status-overdue` | danger-soft / danger-ink |
| `RETURNED` | Đã hoàn trả | `status-badge status-returned` | success-soft / success-ink |
| `CANCELED` | Đã hủy | `status-badge status-canceled` | danger-soft / danger-ink |
| `REJECTED` | Đã từ chối | `status-badge status-rejected` | danger-soft / danger-ink |
| unknown/null | Chưa rõ | `status-badge status-unknown` | surface-card / muted |

- Trim and uppercase the backend value before mapping.
- Every loan badge uses the same geometry: `inline-flex`, centered content, padding `5px 11px`, pill radius, `12px/600`, and `white-space:nowrap`.
- Do not use blue, teal, coral, Bootstrap contextual classes, or page-specific colors for loan statuses.
- Lock the two reference states to exact computed colors: `PENDING` = text `#795d12`, background `#fbf3d9`, border `#eadfb8`; `BORROWED` = text `#2f2b27`, background `#efe9de`, border `#e1d9cd`.
- Search the base layout and all loaded stylesheets for `.status-badge` and `.status-*` before declaring the task complete. Existing layouts may contain legacy solid colors with `!important`.
- If a legacy `!important` rule cannot be safely removed project-wide, override `color`, `background-color`, and `border-color` in the shared/page-family status selector with the exact reference values and matching `!important`. Merely writing a correct non-important rule is a failure if computed styles remain wrong.
- Inspect browser computed styles: `Chờ duyệt` must be pale warm yellow and `Đang mượn` pale neutral cream. Blue, teal, dark amber, and solid-fill badges are forbidden for these states.

### Exact view-action structure and alignment

```html
<table class="app-table app-table--one-action">
  <colgroup>
    <col class="app-data-column">
    <!-- repeat for each preceding data column -->
    <col class="app-action-col">
  </colgroup>
  <thead><tr>
<th class="app-action-column">Hành động</th>
  </tr></thead>
  <tbody><tr>
<td class="app-action-column">
  <div class="app-row-actions">
    <a class="app-icon-action" title="Xem chi tiết" aria-label="Xem chi tiết">
      <i class="bi bi-eye" aria-hidden="true"></i>
    </a>
  </div>
</td>
  </tr></tbody>
</table>
```

- Use `table-layout:fixed`. Add exactly one `<col>` for every visual column and make the final `<col class="app-action-col">` the sole owner of `width:var(--app-action-column-width)`.
- Select exactly one canonical table modifier: `.app-table--one-action` = 72px, `.app-table--two-actions` = 88px, `.app-table--three-actions` = 132px. Never set the variable inline.
- The shared CSS defines those modifiers as `--app-action-column-width:72px`, `88px`, and `132px`; `.app-action-col` consumes the variable. Templates must not duplicate these declarations.
- Apply `.app-action-column` to the header AND every body cell in that column.
- `.app-action-column` owns alignment only: zero left/right padding with `!important`, `text-align:center`, `vertical-align:middle`, `box-sizing:border-box`, and `white-space:nowrap`.
- `.app-row-actions` is `display:flex; align-items:center; justify-content:center; gap:8px; width:100%; margin:0`.
- `.app-icon-action` remains exactly 36×36px with `padding:0` and `line-height:1`; the inner icon has no margin.
- Do not add text beside the eye. Do not use `text-end`, end justification, margin utilities, `ps-*`, `pe-*`, transforms, floats, absolute positioning, or unequal horizontal cell padding.
- Include the 72px action column in the table width budget. Responsive overflow belongs only to `.app-table-wrap`.
- Acceptance test: the center of the “Hành động” header and the center of every eye circle MUST share the same vertical axis at every supported breakpoint.
- Also verify that the left and right edges of every action cell share identical x-coordinates.
- Forbidden: action-column widths through `nth-child`, independent `<th>`/`<td>` widths, percentage action widths, auto sizing, or a mix of fixed pixels and separately assigned percentage cell widths.

## Surgical Fix / Preserve Correct Reference

When the user explicitly identifies a screenshot or current UI as correct except for one named defect:

1. Treat all unrelated colors, status badges, typography, spacing, filters, borders, and column proportions in that reference as locked.
2. Change only the minimum DOM/classes/CSS needed for the named defect.
3. Do not use the task as an opportunity to remap status colors or normalize other already-correct components.
4. Review the final diff and remove any unrelated presentation change.
5. The design specification constrains the fix; it does not override the user's explicit correct reference for unaffected components.

## Notification Compose: Exact Required Format

For every Librarian/Admin “Gửi thông báo” task, copy the canonical notification component; do not design a new two-column form.

### Fixed content

- Eyebrow: `TƯƠNG TÁC ĐỘC GIẢ`
- Title: `Gửi thông báo`
- Description: `Thủ thư gửi thông báo đến toàn bộ hoặc một nhóm độc giả.`
- Field labels: left=`Đối tượng nhận`, `Chọn Member`; right=`Tiêu đề`, `Nội dung`
- Do not add separate display headings above either column.

### Fixed DOM

```html
<div class="app-card notification-compose-card">
  <div class="notification-compose-body">
    <form id="notificationForm">
      <div class="notification-form-grid">
        <div class="notification-recipient-column">...</div>
        <div class="notification-message-column">
          ...
          <div class="notification-form-actions">
            <button class="btn btn-classic">Gửi thông báo</button>
          </div>
        </div>
      </div>
    </form>
  </div>
</div>
```

### Fixed measurements

| Element | Required value |
|---|---|
| Outer card | canvas, 1px hairline, 12px radius, no shadow |
| Body padding | 24px desktop, 16px mobile |
| Desktop grid | `minmax(280px,.85fr) minmax(0,1.15fr)`, gap 32px |
| Columns | identical background; no divider and no full-height inner panel |
| Choice group | surface-soft, 1px hairline, 8px radius, 16px padding |
| Inputs | 42px height, 0 14px padding, 8px radius |
| Member list | max-height 260px, `overflow-y:auto`, 16px padding |
| Textarea | min-height 196px, 12px 14px padding, 8px radius |
| Submit | 40px standard text-only button, right aligned; full-width mobile |
| Responsive | below 992px exactly one column |

Implementation requirements:

1. Reuse `notification-compose.css`; never create another notification form stylesheet or duplicate these rules in `<style>`.
2. Keep `.notification-form-grid` with exactly the two documented direct children and their order.
3. Do not add a tinted left column, vertical center rule, separate column cards, display-serif column headings, section icons, send icon, or shadows.
4. Keep existing Thymeleaf fields, CSRF, validation containers, radio values, checkbox names, and JavaScript IDs.
5. Compare at the same viewport after editing. If background treatment, column boundaries, padding, controls, or textarea height differ from the canonical flat form, the task fails even if functionality works.

---

# After Editing

## Mandatory Completion Validation

Before completion, verify all of the following:

- no duplicate HTML page or CSS owner was created;
- the controller uses the edited template and every new stylesheet is linked in the correct owner/load order;
- no `<style>` or inline presentation remains for the edited component, and no selector/component is defined both in HTML and external CSS;
- every Thymeleaf expression, form action/method, CSRF token, hidden field, validation binding, JavaScript hook, and business behavior remains intact;
- canonical component classes and cross-role visual contracts are reused;
- the existing HTML page was reused when applicable;
- the existing CSS owner was reused when applicable;
- no alternative implementation or duplicate dashboard/page was introduced;
- no duplicate CSS owner was introduced;
- the existing page architecture was preserved;
- all colors resolve to documented tokens/semantic classes and no new arbitrary color was introduced;
- no page/role/domain-specific palette, duplicate token alias, raw color literal, contextual color utility, or undocumented derived color remains in the edited component;
- computed foreground, surface, border, icon/SVG, focus, shadow, overlay, and interaction-state colors match `DESIGN-claude.md`;
- modal/toast/tooltip/dropdown geometry, colors, action order, focus behavior, and responsive state match the canonical overlay contract;
- overlays have one backdrop, one frame owner, one scroll owner, and no arbitrary z-index;
- semantic meaning is not communicated by color alone and applicable contrast checks pass;
- responsive behavior works and computed styles match the contract;
- the Git diff is minimal, `git status` contains no file outside the manifest, and no business logic or unrelated file changed.

## Reporting Format for Codex

Report completion with this structure; write `none` explicitly for empty categories:

```text
Updated files:
- ...

Reused existing files:
- ...

Created files:
- none / ...

CSS extracted from HTML:
- none / ...

Existing page reused:
- ...

New page created:
- none / path + repository-backed reason

Validation:
- functionality preserved
- no duplicate page
- no duplicate CSS owner
- no unrelated files modified
```

Verify:

- The page compiles successfully.
- No Thymeleaf errors exist.
- No broken links.
- No broken forms.
- No JavaScript errors.
- Responsive layout works.
- UI matches `DESIGN-claude.md` and its canonical component structure.
- Equivalent Admin/Librarian pages use the same shared stylesheet and component class names.
- No duplicated page-local CSS exists for a component already provided by shared CSS.
- No edited component retains inline presentation styles that override canonical tokens.
- Search/filter spacing is uniform and action buttons follow the fixed icon/order contract.

## Required Consistency Audit

Before reporting completion, compare the target page with every equivalent role page and record pass/fail internally for:

1. Same header DOM pattern and classes
2. Same CTA icon policy and alignment
3. Same search/filter DOM pattern and gap
4. Same table/card classes and measurements
5. Same row-action icon, order, size, tooltip, and semantic color
6. Same badge treatment
7. Same pagination component
8. Same modal corner and color contract
9. Same mobile collapse behavior
10. Exactly one visible frame per data region
11. Exactly one horizontal-overflow owner
12. No horizontal scrollbar at desktop width
13. Related form actions remain in one `.app-form-actions` row
14. Secondary-form triggers use `form` plus a hidden `.app-detached-form`, not a visible form block
15. Same semantic color mapping and interactive-state colors
16. Same modal size, frame, padding, action order, backdrop, and scroll owner
17. Same toast/tooltip/dropdown geometry, placement, and stacking behavior
18. Keyboard focus is trapped/restored correctly and reduced motion is respected
19. Same design token is used for the same visual role; no copied hex or token synonym exists
20. Computed text, surface, border, icon/SVG, focus, shadow, and state colors match the canonical mapping

If any applicable item fails, the task is not complete.

## Mandatory Cross-Member Parity Procedure

Use this procedure for every UI task so two team members produce the same result.

### A. Lock the target before editing

1. Confirm the navigation URL and controller-returned template. Do not edit an unused similarly named file.
2. Identify one locked reference screenshot/page when supplied; never merge styling choices from multiple references.
3. Record role, permissions, sidebar state, canonical variant, root class, shared stylesheet, and CSS load order.
4. Locate equivalent role pages and reuse their shared partial/DOM/classes rather than recreating them.

### B. Copy structure; do not reinterpret

- Copy the canonical element hierarchy, direct-child order, classes, and modifier classes exactly.
- Do not substitute flex for grid, nested cards for one shell, `nth-child` for `<colgroup>`, or a new page-local component for a shared one.
- Do not use Bootstrap sizing/alignment utilities unless the canonical skeleton explicitly contains them.
- Domain classes may style domain-only content; they may not change canonical control size, alignment, spacing, radius, or semantic color.

### C. Verify runtime computed styles

- Inspect base layout, shared CSS, page CSS, inline styles, pseudo-elements, and all `!important` rules.
- Verify the browser's computed font, font size/weight/line-height, color/background, width/height, padding/gap, border/radius, shadow, alignment, and overflow.
- A source rule that loses in the cascade is a failure. Use the documented selector/component owner instead of adding arbitrary specificity.
- Ensure equivalent pages load the same shared stylesheets in the same order.
- Hard-refresh after CSS edits and confirm stylesheet requests succeed; do not approve a stale-cache rendering.

### D. Use identical content and state tests

Test every applicable state: default, hover, focus-visible, active, selected, disabled, valid, invalid, server error, success, loading, empty, no-results, long content, and each permitted action count.

- Use realistic long Vietnamese names, book titles, emails, and labels.
- Canonical wording, capitalization, punctuation, helper text, and icon policy must match exactly.
- Confirm the intended fonts loaded. A fallback font that changes wrapping or control geometry fails parity.

### E. Compare under identical environments

| Audit | Viewport | Zoom | Sidebar |
|---|---:|---:|---|
| Desktop | 1440×900 | 100% | Expanded |
| Compact desktop | 1024×768 | 100% | Expanded |
| Tablet | 768×1024 | 100% | Canonical behavior |
| Mobile | 390×844 | 100% | Collapsed/overlay |

- Use the same role, permissions, seed data, browser engine, zoom, and sidebar state.
- Do not use browser zoom, manual scaling, or sidebar collapse to hide overflow.
- Only the documented overflow owner may scroll at each viewport.

### F. Enforce surgical scope

- Preserve unrelated correct components. Do not redesign adjacent UI while fixing one named defect.
- Review the final diff component-by-component and remove unrelated changes to color, spacing, text, icon, DOM, or responsiveness.
- Do not change a base layout for one page unless every affected consumer is intentionally verified.

### G. Final Pass/Fail gate

Before reporting completion, all applicable checks must pass:

1. Correct URL/controller template
2. Same shared stylesheet and load order
3. Same DOM hierarchy/classes/modifiers
4. Same computed typography, colors, dimensions, spacing, borders, radii, shadows
5. Same wording and icons
6. Same interaction/validation/empty states
7. Same long-content wrapping
8. Same action order/count/column width/alignment
9. Same behavior at all four required viewports
10. Exactly one visual shell and documented overflow owner
11. No unresolved conflicting inline, utility, legacy, or `!important` override
12. No unrelated presentation changes
13. Actual modified/new files exactly match the declared file-scope manifest

Any applicable Fail blocks completion; “looks close” is not a pass.

---

# If Unsure

When uncertain, choose the option that preserves functionality.

Never sacrifice functionality for appearance.

UI improvements must never change business behavior.
