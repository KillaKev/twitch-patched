# UI settings

The UI settings patch provides one page, available through Patch settings and live
player settings. The initial registry is empty. Each customization module adds its
own toggle under this patch, with a default of off.

## Adding an option

Keep the hook and runtime implementation together under the `ui` feature directory.
Register its key, title and summary with `UiSettings.register` during UI settings
initialization, after creating the registry. Keys use lowercase letters, digits and underscores and remain
stable across updates. Removed modules do not expose or apply saved preferences.

Native code reads `UiSettings.enabled(key)`. `UiSettings.observe` returns a
subscription that must be closed when its view or player is destroyed. Apply the
initial state on attachment and restore the original state when the option is off.

V2 components use `runtime.ui.useEnabled(React, key)`. Preferences synchronize on
React Native attachment and whenever an option changes. Subscribe through the hook
to update mounted components without restarting Twitch.

Resolve each module's hooks independently and require a unique match. Add parser,
lifecycle or DEX coverage as appropriate, then verify enabled and disabled behavior
in native playback, V2 Classic Split and Vertical View. Include theme changes,
navigation and reduced patch selections in device checks.
