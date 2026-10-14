# CastFan UI Design System

This document is the source of truth for the CastFan visual language. All existing and future interfaces must follow this design system unless a later approved design explicitly replaces it.

## Design Direction

CastFan uses a modern, minimalist, iOS-inspired interface in light mode. Black, grey, and white form the primary visual structure, with restrained dusty rose, lavender, and warm amber accents. The experience should feel calm, premium, friendly, and easy to scan on a mobile device while remaining polished on tablet and desktop screens.

The previous pink-led visual theme has been retired. New pages must use the color tokens, spacing, typography, components, and interaction patterns defined here.

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

## Color Palette

| Token | Value | Usage |
| --- | --- | --- |
| Page Background | `#F0F0F2` | Default application background |
| Card Surface | `#FFFFFF` | Cards, panels, forms, and floating navigation |
| Primary Text | `#111318` | Headings and important content |
| Secondary Text | `#898A90` | Supporting copy, captions, and metadata |
| Primary Accent | `#111318` | Primary actions, active navigation, links, and focus states |
| Strong Accent | `#000000` | Hover states and maximum-contrast actions |
| Soft Accent | `#E5E6E8` | Selected controls and subtle neutral surfaces |
| Success Text | `#303238` | Positive feedback text |
| Success Surface | `#E8E9EB` | Positive feedback background |
| Dusty Rose | `#C66C87` | Brand detail, active states, and focused highlights |
| Dusty Rose Dark | `#9A4962` | Accent text and compact actions |
| Dusty Rose Soft | `#F7E7ED` | Active navigation, badges, and icon surfaces |
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
- Standard card radius is `16px`.
- Compact controls use a `12px` radius.
- Feature cards may use an `18px` radius.
- Pill controls and circular buttons may use fully rounded corners.
- Borders use low-opacity neutral colors.
- Shadows must be soft, diffused, and visually light.
- Do not nest decorative cards inside other decorative cards.

## Components

### Navigation

- Member navigation is a floating white pill near the bottom safe area.
- Active items use dusty rose on the icon and label while the navigation surface remains white.
- Navigation remains visible without covering page content.
- Header bars use a translucent light surface with background blur.

### Buttons

- Primary actions use a black-to-charcoal gradient, white text, and a restrained neutral shadow.
- Secondary actions use a white surface, subtle border, and dark text.
- Icon-only commands use circular buttons with tooltips or accessible labels.
- Follow and compact actions may use pill-shaped controls.
- Hover, focus, pressed, and disabled states must remain visually distinct.

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

The production Member Schedule extensions are implemented in:

`src/main/resources/static/css/member-schedule.css`

The production Following and Profile extensions are implemented in:

`src/main/resources/static/css/member-following.css`

`src/main/resources/static/css/member-profile.css`

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

## Approved Member Information Architecture

The persistent Member navigation contains four destinations in this order:

1. `Home` - followed-Cast updates, published events, today's approved shifts, and summary information.
2. `Schedule` - approved Cast shifts by day or week.
3. `Following` - Cast profiles followed by the authenticated Member.
4. `Profile` - account information, memberships, notification preferences, and sign out.

Search remains a field within Home and relevant directory pages. It is not duplicated as a top-level icon. Membership management belongs in Profile and individual Cast profiles rather than the persistent navigation.

## Approved Owner Information Architecture

The persistent Owner navigation contains four destinations:

1. `Dashboard` - store health, staffing totals, pending reviews, and upcoming events.
2. `Shifts` - shift approval queue and daily schedule.
3. `Events` - draft, edit, publish, preview, and archive store events.
4. `Account` - owner identity, security, and sign out.

Event deletion is represented by an `Archived` state so published history remains auditable. Only `Published` events are exposed through Member and public routes.
