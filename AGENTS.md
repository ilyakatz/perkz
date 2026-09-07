# Development rules

## Tests

- When adding new logic, agents may add or update tests that cover that new logic.
- When changing existing logic, agents must not modify existing tests without consulting the user first.

## Compose components

- Keep each reusable Compose component in its own file named after the component.
- Keep previews with the component they preview, and provide light and dark previews when the component supports both themes.
- Preview visible content separately from platform wrappers such as `Dialog`, `ModalBottomSheet`, or navigation hosts so Android Studio renders the preview.
- When a UI component is changed, compare its preview or emulator screenshot with the applicable design reference before declaring the work complete.
- Screens should compose reusable components rather than defining substantial reusable UI inline.
