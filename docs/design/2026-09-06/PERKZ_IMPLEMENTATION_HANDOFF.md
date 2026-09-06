# Perkz — Android Implementation Handoff

## Product goal

Perkz helps users import recurring credit-card benefits from a Google Sheet, identify perks requiring attention, record partial or full usage, mark perks as not applicable, and synchronize changes back through an Apps Script webhook.

## Visual source of truth

Use the two approved “Wallet Organizer” mockups from the design conversation:

- Dark theme: the finalized dark Perkz screen.
- Light theme: the matching finalized light Perkz screen.

Save them in the Android repository as:

```text
docs/design/perkz-dark.png
docs/design/perkz-light.png
```

Both references represent the same layout and behavior. Theme changes must not change spacing, information hierarchy, component sizes, or available actions.

## Recommended Android stack

- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- ViewModel + StateFlow
- Room for imported perks, period usage, and persisted UI state
- DataStore for URLs, theme, filters, and collapse state
- Retrofit/OkHttp (or the project's existing networking layer)
- WorkManager only if background refresh is added later

Preserve the existing project architecture and dependency conventions when they differ.

## Navigation

Use a Material 3 bottom navigation bar with two destinations:

1. Perks — default destination
2. Settings

## Perks screen

### Top region

- `Perkz` title
- Last synchronization state
- Refresh button
- Horizontally scrollable card selector
- Attention/status selector and filter button
- Horizontally scrollable status rail

Selected filters persist across process death and app restart.

### Status sections

Order sections as:

1. Expiring soon
2. Needs use
3. Upcoming
4. Partially used
5. Already used
6. Expired
7. Not applicable

Each section has independent expanded state, accessible status treatment, optional count/value summary, and interval groups. Interval groups have their own persistent expanded state. Show `Collapse all` or `Expand all` only when the section contains interval groups with items.

### Perk presentation

The selected design uses an action-first wallet surface:

- Status/deadline badge
- Perk title and card name
- Current period and reset interval
- Details/notes
- Maximum value or uses
- Used value
- Remaining value as the strongest numeric emphasis
- Progress indicator
- `Add amount` primary action
- `Mark full` secondary action
- Overflow actions for clear usage and not applicable

Action labels must remain horizontal. Prefer a single-column arrangement on narrow screens rather than compressing controls.

## Usage behavior

- A new `$25` perk displays `$0 / $25 used` and `$25 remaining`.
- Selecting usage opens an amount dialog defaulted to the remaining amount.
- Entering `10` produces `$10 / $25 used` and `$15 remaining`.
- `Add amount` adds to the current-period value rather than replacing it.
- Clamp or reject entries that would exceed the maximum; choose one consistent policy and explain it in the UI.
- `Mark full` writes the exact maximum value.
- Clearing usage clears both Used and Date Used for the current period.
- Support currency and integer-count units without showing inappropriate currency symbols.
- Confirm destructive clearing when existing usage is non-zero.

## Status precedence

Evaluate status in this order so categories remain mutually exclusive:

1. Not applicable when the sheet/local value is literal `N/A`.
2. Already used when usage is greater than or equal to maximum.
3. Expired when expiration has passed and usage is below maximum.
4. Partially used when usage is greater than zero and below maximum.
5. Upcoming when the benefit belongs to a future period.
6. Expiring soon when its active expiration is within the configured threshold.
7. Needs use when it belongs to the current period and usage is zero.

Define the expiring-soon threshold in one domain-level policy rather than duplicating date logic in UI code.

## Not applicable

- Write literal `N/A` to the sheet Used column.
- Exclude the perk from every active status section.
- Hide normal usage controls.
- Show `Restore perk`.
- Restoring clears `N/A` and recalculates the perk's status.

## Google Sheet boundary

Create repository interfaces before implementing network details:

```kotlin
interface PerkRepository {
    fun observePerks(): Flow<List<Perk>>
    suspend fun refresh(): Result<Unit>
    suspend fun addUsage(perkId: String, periodKey: String, amount: BigDecimal): Result<Unit>
    suspend fun markFullyUsed(perkId: String, periodKey: String): Result<Unit>
    suspend fun clearUsage(perkId: String, periodKey: String): Result<Unit>
    suspend fun markNotApplicable(perkId: String, periodKey: String): Result<Unit>
    suspend fun restore(perkId: String, periodKey: String): Result<Unit>
}
```

CSV import fields include perk title, card, interval, reset period, value/uses, deadline, details, Used, and Date Used.

Parsing rules:

- Numeric Used = exact partial or full usage.
- `yes`, `true`, or populated Date Used with no numeric value = full usage.
- `N/A` = not applicable.
- Blank Used = no usage.

Webhook writes must send the exact Used representation plus Date Used:

- Partial: `10`
- Full: `25`
- Not applicable: `N/A`
- Cleared: blank Used and blank Date Used

Treat local changes as pending until confirmed. On refresh, reconcile remote values with local pending writes deterministically and expose sync errors without discarding user input.

## Settings screen

- Google Sheet CSV URL
- Apps Script webhook URL
- Save settings
- Refresh data
- Theme: System, Light, Dark
- Inline validation and actionable errors
- Explanatory copy for each URL
- Polished empty state when no CSV URL exists

Validate HTTPS, expected CSV accessibility, and webhook URL shape without logging sensitive query parameters.

## Theme tokens

### Dark

| Role | Suggested color |
| --- | --- |
| Background | `#08131F` |
| Surface | `#111F2D` |
| Elevated surface | `#172635` |
| Primary text | `#F4F7FB` |
| Secondary text | `#B9C5D4` |
| Primary mint | `#78F0C1` |
| Expiring coral | `#FF6B6B` |

### Light

| Role | Suggested color |
| --- | --- |
| Background | `#F7F9F8` |
| Surface | `#FFFFFF` |
| Elevated surface | `#F1F5F6` |
| Primary text | `#102033` |
| Secondary text | `#566579` |
| Primary mint | `#39C997` |
| Expiring coral | `#D94B50` |

Use semantic Material color roles in code; do not reference raw colors throughout components. Confirm contrast for text, icons, badges, progress indicators, disabled states, and focus states.

## Accessibility and responsive requirements

- Minimum 48 dp touch targets
- Meaningful content descriptions for icon-only controls
- Do not rely on color alone for status
- Support font scaling without clipping essential values or vertically wrapping action labels
- Use horizontal scrolling for filter rails
- Provide state descriptions for collapsible controls and progress
- Test at narrow phone widths and in both themes

## Suggested delivery sequence

1. Inspect the repository and document its current architecture.
2. Add domain models and status calculation tests.
3. Add fake repository data and build both screens.
4. Implement theme tokens and theme persistence.
5. Add Room/DataStore persistence.
6. Implement CSV import and parsing tests.
7. Implement webhook writes and reconciliation.
8. Add accessibility checks, UI tests, and narrow-screen verification.

## Prompt for Codex app

Attach both mockups, open the Android project, and send:

```text
Implement Perkz using PERKZ_IMPLEMENTATION_HANDOFF.md and the attached
docs/design/perkz-dark.png and docs/design/perkz-light.png references.

First inspect the repository, its AGENTS.md files, build configuration,
architecture, and existing conventions. Then propose a concise implementation
plan before editing. Use Kotlin, Jetpack Compose, and Material 3 unless this
project already establishes equivalent choices.

Build the shared light/dark UI from one semantic component system; do not
duplicate screens by theme. Preserve the exact Perkz business and status rules.
Start with tested domain logic and fake repository data, then add persistence,
CSV import, webhook synchronization, and reconciliation in staged changes.

Verify each stage with the project's formatter, unit tests, Compose/UI tests,
and Android build. Compare the final narrow-phone rendering against both
references and report remaining differences explicitly.
```

## Definition of done

- Both theme variants match the approved hierarchy and remain behaviorally identical.
- All seven statuses are mutually exclusive and covered by tests.
- Partial, full, cleared, and N/A writes use exact sheet values.
- Usage is keyed by perk and usage period.
- Settings, filters, theme, and collapse states survive restart.
- Refresh reconciliation never silently overwrites pending local input.
- Empty, loading, validation-error, sync-error, and populated states are designed.
- Narrow-screen layouts remain readable with accessible touch targets.
