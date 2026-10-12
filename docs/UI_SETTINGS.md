# UI settings

UI settings are available through Patch settings, live player settings and V2
chat settings. All customization options are off by default and take effect
without restarting Twitch.

Options are organized by the control being changed:

- Player controls: Clip, Cast and Share.
- Channel actions: Subscribe, gifting, Bits and Leaderboard.
- Display: Android status and navigation bars during landscape fullscreen playback.

Controls with multiple locations have one settings page with a Hide everywhere
switch and individual
location switches. Classic Split settings also cover fullscreen playback.
Subscribe and gifting distinguish the Vertical View header from channel actions
below the video. Leaderboard is available on versions that include the button;
casting settings apply wherever Twitch makes casting available.

Hide status bar also removes Twitch's reserved camera-cutout padding during
landscape fullscreen playback. Keyboard insets remain active. Leaving fullscreen
or disabling the setting restores Twitch's normal inset handling.

Fullscreen inset handling targets the fullscreen container and checks its owning
activity's current orientation. Insets can arrive before the first layout callback after rotation.
Changing a system-bar option reapplies Twitch's padding listener even when Android's
inset values stay unchanged.
Run `scripts/test-ui-fullscreen.ps1` with a connected Android device to check
container scope, rotation and toggle behavior without installing an APK.

Group switches derive their state from the location preferences and show how many
are hidden. Existing preferences are retained. Hidden controls are removed from
their native constraint chain or action row, so remaining controls retain their
order and alignment.

Search is available on every Patch settings page and covers the included patches.
Results show their category and layout, and switches can be changed directly.
Back restores the previous page, search and scroll position. Clearing a search
returns to the previous browsing position.

## Adding an option

Keep each option's hooks and runtime policy under the `ui` feature directory. Add
the option to `UiOptions` through `UiFeatureRegistry.register`, then include it in
the appropriate settings group with a section and native drawable icon. Search
indexes titles, descriptions and the category path. Preference keys use lowercase letters, digits and
underscores and remain stable across updates. Group switches derive their state
from individual preferences; they do not store a second set of values.

Native views read `UiSettings.enabled(key)`. `UiSettings.observe` returns a
subscription that must close when the view detaches. Retain Twitch's requested
visibility so turning a setting off restores the current application state.
Native Compose controls collect the shared preference flow inside a balanced
composition group. Resolve Compose types and methods from the original APK.
Find matching call sites before requesting mutable classes. Collect action
callers in one scan, retaining only matches for the selected renderers. Unrelated
classes must remain unchanged so patching fits Android Manager's memory limits.

React Native components use `runtime.ui.useEnabled(React, key)`. Preferences
synchronize when the runtime attaches and when an option changes. Filter action
models before rendering whenever possible. Tree transformations must preserve
callbacks, keys and untouched elements, with explicit traversal limits.

Resolve each option's hooks independently and require a unique match. Verify both
toggle directions, mixed group states, nested Back navigation and disabled-state
behavior. Check native playback, V2 Classic Split and Vertical View, including
theme changes, orientation changes and reduced patch selections.
