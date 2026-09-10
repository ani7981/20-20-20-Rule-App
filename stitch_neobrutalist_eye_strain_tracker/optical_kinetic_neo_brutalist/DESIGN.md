---
name: Optical Kinetic Neo-Brutalist
colors:
  surface: '#f9f9f9'
  surface-dim: '#dadada'
  surface-bright: '#f9f9f9'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f3f3f3'
  surface-container: '#eeeeee'
  surface-container-high: '#e8e8e8'
  surface-container-highest: '#e2e2e2'
  on-surface: '#1b1b1b'
  on-surface-variant: '#4b4731'
  inverse-surface: '#303030'
  inverse-on-surface: '#f1f1f1'
  outline: '#7c775f'
  outline-variant: '#cdc7aa'
  surface-tint: '#6a5f00'
  primary: '#6a5f00'
  on-primary: '#ffffff'
  primary-container: '#ffe600'
  on-primary-container: '#726600'
  inverse-primary: '#dec800'
  secondary: '#006d3f'
  on-secondary: '#ffffff'
  secondary-container: '#53fca4'
  on-secondary-container: '#007242'
  tertiary: '#b31f56'
  on-tertiary: '#ffffff'
  tertiary-container: '#ffdce1'
  on-tertiary-container: '#bd285d'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#fde400'
  primary-fixed-dim: '#dec800'
  on-primary-fixed: '#201c00'
  on-primary-fixed-variant: '#504700'
  secondary-fixed: '#57ffa7'
  secondary-fixed-dim: '#2ce28d'
  on-secondary-fixed: '#002110'
  on-secondary-fixed-variant: '#00522e'
  tertiary-fixed: '#ffd9df'
  tertiary-fixed-dim: '#ffb1c1'
  on-tertiary-fixed: '#3f0018'
  on-tertiary-fixed-variant: '#8f003f'
  background: '#f9f9f9'
  on-background: '#1b1b1b'
  surface-variant: '#e2e2e2'
typography:
  display-hero:
    fontFamily: Space Grotesk
    fontSize: 56px
    fontWeight: '700'
    lineHeight: 58px
    letterSpacing: -0.04em
  display-hero-mobile:
    fontFamily: Space Grotesk
    fontSize: 42px
    fontWeight: '700'
    lineHeight: 44px
    letterSpacing: -0.03em
  timer-display:
    fontFamily: Space Mono
    fontSize: 64px
    fontWeight: '700'
    lineHeight: 64px
    letterSpacing: -0.05em
  timer-display-mobile:
    fontFamily: Space Mono
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 48px
    letterSpacing: -0.04em
  headline-lg:
    fontFamily: Space Grotesk
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 36px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Space Grotesk
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 28px
    letterSpacing: -0.01em
  headline-sm:
    fontFamily: Space Grotesk
    fontSize: 20px
    fontWeight: '700'
    lineHeight: 24px
    letterSpacing: 0em
  body-lg:
    fontFamily: Space Grotesk
    fontSize: 16px
    fontWeight: '500'
    lineHeight: 22px
    letterSpacing: 0em
  body-md:
    fontFamily: Space Grotesk
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
    letterSpacing: 0em
  body-sm:
    fontFamily: Space Grotesk
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.01em
  label-code:
    fontFamily: Space Mono
    fontSize: 14px
    fontWeight: '700'
    lineHeight: 18px
    letterSpacing: 0.04em
  label-badge:
    fontFamily: Space Grotesk
    fontSize: 11px
    fontWeight: '700'
    lineHeight: 12px
    letterSpacing: 0.06em
  label-telemetry:
    fontFamily: Space Mono
    fontSize: 10px
    fontWeight: '400'
    lineHeight: 12px
    letterSpacing: 0.08em
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  space-xxs: 4px
  space-xs: 8px
  space-sm: 12px
  space-md: 16px
  space-lg: 20px
  space-xl: 24px
  space-2xl: 32px
  space-3xl: 48px
  border-sm: 3px
  border-md: 4px
  offset-shadow-sm: 3px
  offset-shadow-md: 4px
  offset-shadow-lg: 6px
  screen-margin-mobile: 16px
  gutter-mobile: 12px
---

## Brand & Style

This design system establishes a high-energy, hyper-tactile, Neo-Brutalist identity engineered specifically to combat digital fatigue and screen numbness. By abandoning passive, low-contrast clinical wellness tropes, the interface uses aggressive physical metaphors, intentional visual weight, and playful utilitarian ergonomics to snap users out of automatic scrolling and focus them on the 20-20-20 health protocol (every 20 minutes, look at an object 20 feet away for 20 seconds).

The aesthetic marries vintage print poster geometry with retro-digital telemetry:
- **Tone**: High-agency, rebellious, urgent yet playful, hyper-functional.
- **Visual Stance**: Unapologetic structural borders, offset pure-black drop shadows without blur, raw mechanical transitions, and vibrant pop-accent surfaces.
- **User Feel**: Every interactive element responds like an analog physical arcade switch—crisp snap states, depressed mechanical offsets, and immediate visual feedback.

## Colors

The color system relies on high-contrast black containment structures framing electric, saturated focal fills over a warm, non-fatiguing paper backdrop.

### Application Rules
- **Base Canvas (`#FFFDF9`)**: Serves as the global backdrop to prevent the visual strain caused by pure `#FFFFFF` over long viewing sessions while maintaining a bright, print-like paper foundation.
- **Primary Ink (`#000000`)**: Used for all borders, drop shadows, icons, and primary typography. No grey borders or muted dividers exist.
- **Primary Accent (`#FFE600`)**: Reserved for primary calls-to-action, the main 20-minute countdown visualizer, and critical attention triggers.
- **Functional Secondary Accents**:
  - **Mint Green (`#2DE28D`)**: Indicates active break execution, completion milestones, streak metrics, and optical recovery states.
  - **Hot Cyber Pink (`#FF5C8D`)**: Denotes critical strain warnings, screen-time thresholds exceeded, and destructive prompts.
  - **Electric Cyan (`#38DBFF`)**: Identifies distance targets (the "20 Feet" visual cues) and technical telemetry stats.
  - **Sunset Orange (`#FF6B4A`)**: Used for alert tags, session pauses, and warning states.
  - **Soft Lavender (`#E8D5FF`)**: Serves as a neutral tinted container background for secondary card groupings to balance high-saturation accents.
- **Card Surface (`#FFFFFF`)**: Neutral card bodies hosting dense typography, always framed by solid `#000000` boundaries.

## Typography

The typographic hierarchy pairs two typefaces: **Space Grotesk** for muscular display titles and primary body prose, and **Space Mono** for telemetry, timer readouts, sensory status codes, and data tags.

### Hierarchy & Usage Rules
- **Uppercase Mandate**: All button labels, status badges, metric labels, and section headings must be styled in uppercase with tight horizontal tracking.
- **Monospace Isolation**: Numeric timers (`00:20:00`, `00:00:20`), real-time eye-tracking frame-rates, camera sync status, and streak counts strictly use `Space Mono` to evoke real-time instrumentation.
- **Letter Spacing**: Display scales utilize negative letter spacing to create dense typographic mass that mirrors physical wood-block print. Smaller labels enforce positive tracking to maintain legibility inside thick borders.

## Layout & Spacing

The layout is built around an explicit, non-fluid bounding container model prioritizing tactile, thumb-driven single-screen ergonomic loops on Android mobile devices.

### Mobile Grid & Layout Principles
- **Screen Margin**: 16px lateral safe margin on mobile displays.
- **Columns**: 4-column layout on standard phones; 8-column layout on foldable expanded states and tablets.
- **Tactile Component Offsets**: Spacing tokens explicitly accommodate hard drop-shadow footprints. Every component with a 4px shadow must account for the shadow boundary in parent padding to prevent clipping against neighboring elements.
- **Component Stacking**: Vertical module flow requires a mandatory 16px or 20px gap. Density must remain high without overlapping physical drop shadow footprints.
- **Strict Rhythm**: Margins, internal card paddings, and button dimensions align to an 8px base rhythm, with 4px micro-increments for borders, shadows, and inline badge offsets.

## Elevation & Depth

This system rejects physics-simulated elevation, skeuomorphic ambient lighting, tonal layer tinting, and blurred drop shadows. Depth is communicated strictly through directional offset ink stamping.

### Elevation Levels
1. **Level 0 (Recessed / Flat Canvas)**
   - No shadow.
   - 3px or 4px solid `#000000` border.
   - Example: Inactive progress track, input fields, container backgrounds.
2. **Level 1 (Static Objects & Interactive Chips)**
   - Box shadow: `3px 3px 0px #000000`.
   - Border: `3px solid #000000`.
   - Example: Selectable chips, small toggle switches, auxiliary metric tiles.
3. **Level 2 (Default Interactive Surface & Standard Cards)**
   - Box shadow: `4px 4px 0px #000000`.
   - Border: `4px solid #000000`.
   - Example: Dashboard cards, main action buttons, active status panels.
4. **Level 3 (Hero Modules & Floating System Drawers)**
   - Box shadow: `6px 6px 0px #000000`.
   - Border: `4px solid #000000`.
   - Example: Active break countdown modal, emergency eye-rest overlay.

### The Tactile Press Mechanic (Active States)
All interactive elements must translate physically downward and to the right upon interaction:
- `default`: `transform: translate(0px, 0px); box-shadow: 4px 4px 0px #000000;`
- `active / pressed`: `transform: translate(4px, 4px); box-shadow: 0px 0px 0px #000000;`
This replicates an authentic mechanical switch depression across all touch triggers.

## Shapes

Shapes leverage a deliberate tension between boxy structural containers and pill-shaped interactive chips. Roundedness is kept tight (`roundedness: 1` — base `4px` corner radius) to preserve hard industrial corners without appearing unfinished.

### Geometry Guidelines
- **Containers & Cards**: Base `4px` to `8px` corner radius with `4px` solid black borders. Never exceed `12px` on structural layouts.
- **Badges & Pill Stickers**: Fully rounded (`9999px`) pill containers equipped with `3px` solid borders, often offset with slight rotations (`-2deg` to `+3deg`) to replicate adhesive physical stickers applied to heavy equipment.
- **Visual Punches & Cutouts**: Corner chamfers and notched card corners are encouraged for telemetry panels and warning callouts.

## Components

### Buttons
- **Primary CTA ("START BREAK" / "REST EYES")**: 
  - Background: Electric Lemon Yellow (`#FFE600`).
  - Border: `4px solid #000000`.
  - Shadow: `4px 4px 0px #000000`.
  - Typography: `Space Grotesk`, 18px, Bold, Uppercase.
  - Active: Translate `4px 4px`, shadow collapse to `0px 0px 0px`.
- **Secondary Action**: Background Crisp White (`#FFFFFF`) or Vivid Mint (`#2DE28D`) with identical border and shadow values.
- **Destructive / Cancel**: Background Hot Cyber Pink (`#FF5C8D`), typography `#000000`.

### Cards & Telemetry Containers
- **Metric Cards**: Background `#FFFFFF` or Soft Lavender (`#E8D5FF`), framed in `4px solid #000000`, with `4px 4px 0px #000000` shadow. Internal padding: `16px`. Header area partitioned by a internal `3px solid #000000` horizontal rule.
- **Breakout Warning Card**: Background Sunset Orange (`#FF6B4A`), pure black typography, bold cross-hatch pattern accents along the top edge.

### Chips & Badges
- **Status Badges**: Capsule pill shape (`9999px`), `3px solid #000000` border, `2px 2px 0px #000000` shadow.
- **Filter / Toggle Chips**: Unselected states are `#FFFFFF` with `3px` borders. Selected states snap to `#38DBFF` or `#FFE600` with the button-press translation applied.

### Form Inputs & Checkboxes
- **Input Fields**: Crisp off-white (`#FFFDF9`) or `#FFFFFF`, `3px solid #000000` border, inset shadow effect replaced with clean high-contrast selection borders: on focus, field gains a `3px 3px 0px #000000` hard shadow and background switches to Electric Cyan tint.
- **Checkboxes & Radios**: 
  - Large `24x24px` squares with `3px solid #000000`.
  - Checked state: Background `#FFE600` or `#2DE28D` with a thick, hand-drawn vector checkmark (`#000000`).

### Segmented Brutalist Progress Bar
- Instead of smooth continuous loading bars, progress is rendered as an array of discrete chunky rectangular cells:
  - Total duration (20 minutes or 20 seconds) split into solid blocks.
  - Completed blocks fill with `#2DE28D` (break progress) or `#FFE600` (strain interval) separated by `3px solid #000000` lines.
  - Incomplete blocks remain `#FFFFFF` with black borders.

### Sticker Elements
- Floated visual accents (e.g., "BLINK!", "20 FT TARGET", "RETINA SHIELD") positioned atop card headers with a `-3deg` to `+4deg` rotation, sharp drop shadows, and high-saturation color fills.