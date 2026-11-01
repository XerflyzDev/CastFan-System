# CastFan UI Design System

This document is the source of truth for the current CastFan visual language. It replaces earlier blue/mint, pink-dominant, desktop-shell, and role-specific visual variants. All existing and future interfaces must follow this design system unless a later approved design explicitly replaces it.

## Design Direction

CastFan uses a modern, minimalist, iOS-inspired interface in light mode. Black, grey, and white form the primary visual structure, with restrained dusty rose, lavender, and warm amber accents. The approved application-wide treatment uses white content canvases, `#F5F5F5` atmospheric backgrounds, `#EBEBEB` separators, very light depth, rounded pill controls, and translucent floating navigation with a neutral active item. The experience should feel calm, premium, friendly, and easy to scan on a mobile device while remaining polished on tablet and desktop screens.

The previous pink-led visual theme and the older full-width desktop application shell are retired. New pages must use the color tokens, spacing, typography, components, and interaction patterns defined here. The approved direction is based on the current CastFan Product UI v2 reference and the implemented phone-shell pages.

## Core Principles

- Use light mode only.
- Keep layouts clean, airy, and mobile-first.
- Maintain consistent spacing and alignment.
- Use soft neutral gradients as page-level atmosphere, not as visual clutter.
- Present primary content on elevated white surfaces.
- Keep borders subtle and shadows soft and diffused.
- Maintain a clear hierarchy between headings, body text, captions, and actions.
- Prefer familiar thin outline icons and concise English labels.
- Preserve accessible focus, error, success, disabled, and selected states.
- Ensure layouts remain usable from 320 px mobile screens through desktop widths.

## Current Application Shell

- Use a centered phone-style application surface on tablet and desktop, with a maximum width of approximately `390px` and a maximum working height of approximately `844px`.
- At viewports up to `470px`, the application surface becomes full-width and full-height with no outer border, radius, or desktop shadow.
- Use a white content canvas over a `#F5F5F5` page background.
- Use the `CastFan` wordmark at the upper left with the shared `1.22rem`, extra-bold treatment and safe-area-aware top spacing. Do not reduce its size for individual roles or account pages.
- Non-root workflow pages place a circular arrow-back action at the upper right. Its accessible label names the destination (`Back to Home`, `Back to Profile`, `Back to Cast workspace`, or `Back to Owner dashboard`) and it returns to the documented parent page. Other contextual actions may remain beside it.
- Keep the main content independently scrollable and reserve bottom space for persistent navigation.
- Use the same shell, spacing rhythm, heading hierarchy, card treatment, and navigation behavior for Member, Cast, and Owner pages.
- Role differences belong in information, permissions, actions, and navigation destinations—not in a separate visual theme.
- Authentication and public-detail pages may omit persistent navigation, but must retain the same typography, colors, radii, border treatment, and accessibility behavior.
- Authentication pages use the same `390px × 844px` maximum shell and `470px` full-screen breakpoint as the role workspaces, with a centered wordmark, generous breathing room, one elevated form card, thin page heading, full-width dark pill action, and plain-language security note. The `dev` profile may show a clearly labeled, non-functional Google sign-in or sign-up mockup for design review; it must not render in production until a reviewed OAuth flow exists. Public registration must clearly identify itself as Member registration and must not expose privileged role choices.
- Create-account feedback is progressive: display names require at least three non-space characters, email format feedback appears while typing, and password strength is reported as Weak, Fair, or Strong without implying requirements beyond the enforced 8-to-72-character rule.

## Color Palette

| Token | Value | Usage |
| --- | --- | --- |
| Page Background | `#F5F5F5` | Default application background |
| Card Surface | `#FFFFFF` | Cards, panels, forms, and floating navigation |
| Primary Text | `#111318` | Headings and important content |
| Secondary Text | `#898A90` | Supporting copy, captions, and metadata |
| Primary Accent | `#111318` | Primary actions, active navigation, links, and focus states |
| Strong Accent | `#000000` | Hover states and maximum-contrast actions |
| Soft Accent | `#E5E6E8` | Selected controls and subtle neutral surfaces |
| Success Text | `#303238` | Positive feedback text |
| Success Surface | `#E8E9EB` | Positive feedback background |
| Dusty Rose | `#C66C87` | Semantic detail, follow actions, badges, and avatar accents |
| Dusty Rose Dark | `#9A4962` | Accent text and compact actions |
| Dusty Rose Soft | `#F7E7ED` | Badges, empty-state icons, and supporting icon surfaces |
| Soft Lavender | `#EEEAF7` | Limited avatar variation |
| Warm Amber | `#F7EDD9` | Limited avatar variation |
| Error | `#C83F5B` | Validation and destructive feedback |

Page backgrounds blend white, light grey, and soft charcoal-grey tones. Blue and mint are not part of the approved theme. Dusty rose, lavender, and amber are supporting accents only and must not dominate a page. Avoid saturated full-screen color fields and excessive decorative effects.

## Typography

- Primary font: `LINE Seed Sans TH`.
- The official WOFF2 files are self-hosted so the interface does not depend on a third-party font CDN.
- System fallback: `-apple-system`, `BlinkMacSystemFont`, `Segoe UI`, and `sans-serif`.
- Headings use semi-bold or bold weights with compact line height.
- Body text remains clean and readable.
- Captions and metadata use secondary grey with smaller sizing.
- Letter spacing remains `0`.
- Interface content and system messages are written in English first.

LINE Seed is provided by LY Corporation under the SIL Open Font License 1.1. The project uses the official LINE Seed Sans TH webfont package because it supports both Latin and Thai text. See the [official LINE Seed website](https://seed.line.me/index_th.html) for attribution and licensing details.

## Surfaces And Depth

- Cards and panels use pure or slightly translucent white surfaces.
- Prefer a `1px` `#EBEBEB` separator or inset outline over a visible drop shadow.
- Standard card radius is `16px`.
- Compact controls use a `12px` radius.
- Feature cards may use an `18px` radius.
- Pill controls and circular buttons may use fully rounded corners.
- Borders use low-opacity neutral colors.
- Shadows must be soft, diffused, and visually light; large card grids should appear almost flat.
- Do not nest decorative cards inside other decorative cards.

## Components

### Navigation

- Persistent navigation is a translucent floating white pill near the bottom safe area.
- Navigation destinations divide the available width evenly for every role. Keep labels on one line and use ellipsis only when a label cannot fit at the 320 px minimum viewport.
- Active items use black text and icons on a solid white inner pill with a restrained shadow. Use the same selected treatment for anonymous, Member, Cast, and Owner navigation; dusty rose remains available for semantic content accents rather than global navigation state.
- Navigation remains visible without covering page content.
- Navigation labels remain concise and must not wrap or overlap at `320px`.
- Page headers use the consistently sized `CastFan` wordmark, generous safe-area-aware whitespace, and an optional circular contextual action instead of the retired full-width desktop header bar.

### Buttons

- Primary actions use a black-to-charcoal gradient, white text, and a restrained neutral shadow.
- Secondary actions use a white surface, subtle border, and dark text.
- Icon-only commands use circular buttons with tooltips or accessible labels.
- Follow and compact actions may use pill-shaped controls.
- Hover, focus, pressed, and disabled states must remain visually distinct.
- Use task-oriented labels. The Cast dashboard entry action is `Request shift`; the shift form itself may use `Submit a shift` because it performs the actual request submission.

### Forms And Search

- Inputs use soft grey surfaces, subtle borders, and 12px rounded corners.
- Focus states use a dark neutral border and a soft grey focus ring.
- Search fields use a white elevated surface and a leading outline icon.
- Error fields use a soft red background and clear English feedback.
- Labels remain visible; placeholders do not replace labels.

### Content Cards

- Cards prioritize information hierarchy over decoration.
- Event cards may combine a real image with structured event information.
- Cast avatars are circular and may use subtle grey gradients when no image exists.
- Status labels use compact pills with semantic colors.
- Repeated directory and membership items maintain stable dimensions and spacing.

### Shared Page Patterns

- Discovery and membership pages use the shared phone shell, wordmark header, page eyebrow, title, supporting copy, single-column cards, and floating navigation.
- Member Account and Cast Profile settings use the same identity-card proportions, grouped settings rows, section headings, role pill, and neutral borders.
- Cast and Owner workspaces may expose different operational cards, metrics, and actions, but use the same white canvas, restrained accent colors, rounded surfaces, and floating navigation treatment.
- General states such as loading, empty, success, validation, disabled, selected, and error must look consistent across roles.

### Empty And Feedback States

- Empty states use a simple circular icon treatment, a concise heading, and one short explanation.
- Success messages use a soft neutral-grey surface with dark text.
- Neutral information uses the Soft Accent surface.
- Errors use a soft red surface and accessible red text.
- Toasts use a compact dark translucent surface above the floating navigation.

## Responsive Behavior

- Design mobile-first with a minimum supported width of 320px.
- Keep touch targets large enough for comfortable mobile interaction.
- Convert multi-column grids to a single column on smaller screens.
- Prevent button labels, navigation labels, and long names from overflowing.
- Reserve enough bottom padding for the floating navigation and safe-area inset.
- Use `100dvh` with a `100vh` fallback for full-height application shells. On mobile, anchor floating navigation to the visual viewport and add `env(safe-area-inset-bottom)` to the normal bottom spacing so iOS browser controls and the home indicator cannot cover it.
- Desktop layouts may use wider grids but must retain the same visual language.

## Accessibility

- Preserve visible keyboard focus indicators.
- Use semantic HTML and accessible labels for icon-only controls.
- Do not rely on color alone to communicate state.
- Keep text contrast readable against pastel and translucent surfaces.
- Respect `prefers-reduced-motion`.
- Keep validation feedback associated with its form field.

## Implementation

The design tokens and shared component styles are implemented in:

`src/main/resources/static/css/app.css`

The approved mobile Member shell and Home components are implemented in:

`src/main/resources/static/css/member-home.css`

Role-aware persistent navigation is centralized in:

`src/main/resources/templates/fragments/navigation.html`

Shared discovery and membership pages reuse the same shell through:

`src/main/resources/templates/member/casts.html`

`src/main/resources/templates/member/memberships.html`

The production Member Schedule extensions are implemented in:

`src/main/resources/static/css/member-schedule.css`

The production Following and Profile extensions are implemented in:

`src/main/resources/static/css/member-following.css`

`src/main/resources/static/css/member-profile.css`

Role workspace extensions are implemented in:

`src/main/resources/static/css/cast-workspace.css`

`src/main/resources/static/css/owner-workspace.css`

Current templates consume the shared stylesheet from:

`src/main/resources/templates/`

When adding a new page, reuse the existing tokens and component classes before introducing a new pattern. Any new reusable pattern must remain consistent with this document and should be added to the shared stylesheet.

## Current Theme Status

- [x] Member Home concept and JPA-backed production route
- [x] Member Schedule concept and JPA-backed production route
- [x] Member Schedule day/week modes and week navigation
- [x] Member Notification Center backed by followed Cast content and published events
- [x] Persisted Member notification preferences in Profile settings
- [x] Member Following concept and JPA-backed production route
- [x] Member Profile concept and JPA-backed production route
- [x] Member Profile edit form with persisted bio and avatar
- [x] Cast Profile concept and JPA-backed production route
- [x] Event Detail concept and JPA-backed production route
- [x] Member followed-Cast content feed and access-controlled Content Detail
- [x] Authentication and account pages
- [x] Cast discovery and following pages
- [x] Member membership pages
- [x] JPA-backed Cast Dashboard, Schedule, Profile, and Membership Tiers
- [x] JPA-backed Cast Content Studio with audience and tier visibility
- [x] JPA-backed Owner Dashboard and Shift Review workspace
- [x] JPA-backed Owner Event Management workspace
- [x] JPA-backed Owner Store Management and permanent Cast roster
- [x] JPA-backed Owner Promotion and Banner Management
- [x] Cast membership tier management
- [x] Access-denied page
- [x] Responsive mobile and desktop behavior
- [x] Shared application shell and neutral navigation treatment across roles
- [x] Shared identity-card and grouped-settings language for Member Account and Cast Profile

## Approved Member Information Architecture

The persistent Member navigation contains four destinations in this order:

1. `Home` - followed-Cast updates, published events, today's approved shifts, and summary information.
2. `Schedule` - approved Cast shifts by day or week.
3. `Following` - Cast profiles followed by the authenticated Member.
4. `Profile` - account information, memberships, notification preferences, and sign out.

Search remains a field within Home and relevant directory pages. It is not duplicated as a top-level icon. Membership management belongs in Profile and individual Cast profiles rather than the persistent navigation.

## Approved Owner Information Architecture

The persistent Owner navigation contains five destinations in this order:

1. `Dashboard` - store health, staffing totals, pending reviews, and upcoming events.
2. `Shifts` - shift approval queue and daily schedule.
3. `Events` - draft, edit, publish, preview, and archive store events.
4. `Promos` - promotion and banner scheduling for Member-facing surfaces.
5. `Account` - owner identity, security, and sign out.

Event deletion is represented by an `Archived` state so published history remains auditable. Only `Published` events are exposed through Member and public routes.

## Approved Cast Information Architecture

The persistent Cast navigation contains five destinations in this order:

1. `Home` - the shared public feed, events, promotions, and today's approved shifts.
2. `Schedule` - the shared approved Cast schedule by day or week.
3. `Cast Profile` - the Cast's own public page rendered exactly as Members see it.
4. `Profile` - settings for the Cast's public identity, stage name, bio, photo, account email, and sign out. Active Cast profiles are always public.
5. `Cast` - the private Cast workspace for shift requests, tiers, and content management.

Cast accounts use `/cast/profile` as their single Profile destination. The generic `/account` route redirects Cast accounts there so account and public-profile editing are not presented as separate destinations.
