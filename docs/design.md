## Overview

Supify is a modern, tactile B2B platform for supplier discovery and verification. The base atmosphere is **cream-tinted white canvas** (`{colors.canvas}` — #fffaf0) holding dark-navy ink type and **3D-rendered tactile illustrations** (warehouse logistics, mascot characters, peach/ochre/lavender landscapes) as the dominant brand identity. Where most supply chain platforms play it cold with clinical grids, Supify balances professional reliability with warm, approachable 3D illustrations and saturated single-color feature cards.

Type voice runs **Rubik** (weight 500-600) — a custom rounded display face used at very large sizes (72px hero) with negative letter-spacing for headlines. Body type uses **Inter** at standard weights for maximum legibility of data tables, audit logs, and claim ledgers.

Component voltage comes from **saturated single-color feature cards** in a 6-color palette: hot pink, deep teal, lavender, peach, ochre, and cream-card. Each card displays product UI fragments at small scale — match score bars, multi-point verification checklists, and compliance badges.

**Key Characteristics:**
- Cream-tinted white canvas (`{colors.canvas}` — #fffaf0). The warmth differentiates Supify from cool-gray competitor sites.
- Dark navy/black primary CTAs (`{colors.primary}` — #0a0a0a). Buttons rounded `{rounded.md}` (12px) — friendly modern and tactile.
- 6-color saturated feature card palette: `{colors.brand-pink}`, `{colors.brand-teal}`, `{colors.brand-lavender}`, `{colors.brand-peach}`, `{colors.brand-ochre}`, `{colors.surface-card}` (cream).
- 3D tactile illustrations (logistics centers, mascot characters, abstract shapes) as full-bleed hero artifacts — the brand's primary visual anchor.
- Rounded Rubik display typeface at 500 weight with negative letter-spacing on display sizes.
- Generous border radii: `{rounded.md}` (12px) for buttons + inputs, `{rounded.lg}` (16px) for content cards, `{rounded.xl}` (24px) for feature cards.
- Product UI fragments embedded inside colored cards at small scale — verification run logs, matching algorithms, evidence reviews.
- Section rhythm `{spacing.section}` (96px) between major bands.

## Colors

### Brand & Accent
- **Primary** (`{colors.primary}` — #0a0a0a): All primary CTAs, h1/h2 ink type. Near-black with slight warmth.
- **Brand Pink** (`{colors.brand-pink}` — #ff4d8b): Hot-pink feature card surface. Smart discovery and matching modules.
- **Brand Teal** (`{colors.brand-teal}` — #1a3a3a): Deep teal-green feature card. Primary signal for verified and high-trust status.
- **Brand Lavender** (`{colors.brand-lavender}` — #b8a4ed): Soft lavender feature card. Compliance, ESG, and regulatory modules.
- **Brand Peach** (`{colors.brand-peach}` — #ffb084): Warm peach feature card. Watchlists and sanctions checks.
- **Brand Ochre** (`{colors.brand-ochre}` — #e8b94a): Mustard / ochre feature card for in-review and pending statuses.
- **Brand Mint** (`{colors.brand-mint}` — #a4d4c5): Mint accent on illustrations, trust badges, and verified checkmarks.
- **Brand Coral** (`{colors.brand-coral}` — #ff6b5a): Coral accent for highlights and active status indicators.

### Surface
- **Canvas / Background** (`{colors.canvas}` — #fffaf0): The default page floor. Cream-tinted white.
- **Surface Soft** (`{colors.surface-soft}` — #faf5e8): Soft background surface for cards and banners.
- **Surface Card** (`{colors.surface-card}` — #f5f0e0): Cream feature cards, supplier cards, and ledger tables.
- **Surface Strong** (`{colors.surface-strong}` — #ebe6d6): Stronger cream for emphasized bands and filter panels.
- **Surface Dark** (`{colors.surface-dark}` — #0a1a1a): Dark teal-tinted near-black for high-contrast cards.
- **Hairline** (`{colors.hairline}` — #e5e5e5): 1px borders on cards and inputs.

### Text
- **Ink / Primary** (`{colors.ink}` — #0a0a0a): Headlines and primary text.
- **Body Strong** (`{colors.body-strong}` — #1a1a1a): Emphasized body, lead paragraphs.
- **Body** (`{colors.body}` — #3a3a3a): Default running-text.
- **Muted** (`{colors.muted}` — #6a6a6a): Sub-headings, metadata, breadcrumbs.
- **Muted Soft** (`{colors.muted-soft}` — #9a9a9a): Captions, fine-print.
- **On Primary / On Dark** (`{colors.on-primary}` — #ffffff): Text on primary buttons and dark teal feature cards.

### Semantic
- **Success** (`{colors.success}` — #22c55e): Verified and valid attestations.
- **Warning** (`{colors.warning}` — #f59e0b): In-progress and pending evidence reviews.
- **Error** (`{colors.error}` — #ef4444): Revoked claims, applied gates, validation errors.

## Typography

### Font Family
The system runs **Rubik** for headlines and **Inter** for body, navigation, data tables, and UI controls.

### Hierarchy

| Token | Size | Weight | Line Height | Letter Spacing | Use |
|---|---|---|---|---|---|
| `{typography.display-xl}` | 72px | 500 | 1.0 | -2.5px | Homepage h1 ("Source with confidence.") — Rubik |
| `{typography.display-lg}` | 56px | 500 | 1.05 | -2px | Section heads — Rubik |
| `{typography.display-md}` | 40px | 500 | 1.1 | -1px | Sub-section heads, supplier names |
| `{typography.display-sm}` | 32px | 500 | 1.15 | -0.5px | Feature card titles, header brand |
| `{typography.title-lg}` | 24px | 600 | 1.3 | -0.3px | Supplier trade names, large card titles |
| `{typography.title-md}` | 18px | 600 | 1.4 | 0 | Card titles, intro paragraphs |
| `{typography.title-sm}` | 16px | 600 | 1.4 | 0 | Small card titles, list labels |
| `{typography.body-md}` | 16px | 400 | 1.55 | 0 | Default running-text |
| `{typography.body-sm}` | 14px | 400 | 1.55 | 0 | Descriptions, metadata |
| `{typography.caption}` | 13px | 500 | 1.4 | 0 | Badge labels, captions |
| `{typography.caption-uppercase}` | 12px | 600 | 1.4 | 1.5px | Section labels, "VERIFIED" badges |
| `{typography.button}` | 14px | 600 | 1.0 | 0 | Standard button labels |
| `{typography.nav-link}` | 14px | 500 | 1.4 | 0 | Top-nav menu items |

## Layout & Spacing

### Spacing System
- **Base unit:** 4px.
- **Tokens:** `{spacing.xxs}` 4px · `{spacing.xs}` 8px · `{spacing.sm}` 12px · `{spacing.md}` 16px · `{spacing.lg}` 24px · `{spacing.xl}` 32px · `{spacing.xxl}` 48px · `{spacing.section}` 96px.
- **Section padding:** `{spacing.section}` (96px) between major editorial bands.
- **Card internal padding:** `{spacing.xl}` (32px) for feature cards; `{spacing.lg}` (24px) for supplier and mockup cards.

### Grid & Container
- **Max content width:** 1280px centered.
- **Hero layout:** 7/5 split (h1 and copy on the left, 3D illustration on the right).
- **Feature card grids:** 3-up on desktop, 2-up on tablet, 1-up on mobile.

## Elevation & Depth

| Level | Treatment | Use |
|---|---|---|
| Flat | No shadow, no border | Canvas floor, top nav |
| Soft hairline | 1px `{colors.hairline}` border | Inputs, secondary cards |
| Saturated card | Brand pink/teal/lavender/peach/ochre fill with tactile stroke | Feature cards |
| Tactile card | `4px 4px 0px 0px rgba(0,0,0,1)` shadow with 2px stroke | Interactive feature cards |

## Components

### Top Navigation
**`top-nav`** — Cream nav bar pinned to top. 64px tall, `{colors.canvas}` background. Carries the Supify logo, navigation links (Platform, Verification, Solutions, Supplier Workspace), and CTA cluster.

### Buttons
**`button-primary`** — Background `{colors.primary}`, text `{colors.on-primary}`, rounded `{rounded.md}` (12px).
**`button-secondary`** — Cream button with hairline outline.
**`button-on-color`** — White button used over saturated brand-color feature cards.

### Cards & Containers
**`feature-card-pink`** / **`feature-card-teal`** / **`feature-card-lavender`** — Saturated feature cards holding headlines, descriptions, and product UI fragments.
**`supplier-card`** — Rich supplier card with colored banner, initial monogram avatar, verified status badge, capacity/MOQ/lead time metrics, star rating, and profile link.
**`claim-row`** — Ledger row showing asserted claim, verification method, status indicator (Verified, Under Review, Submitted), and evidence count.
**`trust-panel`** — Sidebar displaying trust standing, expiry dates, 4-pillar progress bars (Identity, Legal, Quality, Capability), and gate callouts.

## Invariants & Design Principles
- Every verified status must show the **state + method + recency** triple.
- No boolean trust flags.
- Color alone never conveys trust standing (always accompanied by text and icon).
- Dependencies and layouts reflow cleanly across mobile, tablet, and desktop breakpoints.
