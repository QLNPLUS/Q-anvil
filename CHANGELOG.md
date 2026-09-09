# Changelog

## 1.4.0

### Fixes

- Fixed enchanted books repeatedly increasing an enchantment beyond its vanilla maximum when combining equal levels.

## 1.3.0

### KubeJS

- Added explicit prompt text control with `event.setText('Text')`.
- Prompt text replaces the `Q Anvil` title at the top of the GUI and is synchronized from the server to the client.
- Removed the legacy `event.text` compatibility path.
- Callback return values are ignored; returning a string no longer sets prompt text.
- `event.setCanceled(true)` can still be used together with `event.setText(...)` to show a warning while rejecting an operation.

### Documentation

- Updated the KubeJS examples and API notes to use the explicit prompt text method.

### Fixes

- Q Anvil now preserves enchantment-book levels above the vanilla maximum when applying them to an item.
- QShop currency charges now use whole units and round fractional costs up consistently.
- Currency display names are synchronized from the server so multiplayer clients no longer show only the currency id.
- Payment validation and vanilla anvil input consumption now use the same settled currency amount.
