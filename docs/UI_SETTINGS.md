# UI settings

UI settings are available through Patch settings, live player settings and V2
chat settings. All
customization options are off by default and take effect without restarting Twitch.

Options are grouped by function, with separate Classic Split and Vertical View
settings for each layout:

- Player controls: Clip, Cast and Share.
- Channel actions: Subscribe, Gift a Sub and Bits.
- Vertical View header: Subscribe, Gift and Leaderboard. Leaderboard controls are available
  on versions that include this button.
- Fullscreen: status bar, navigation bar and camera cutout behavior.

Player and channel groups include a switch for all buttons and individual
switches. Group switches show whether all, some or none of their controls are
hidden. Hidden controls are removed from their native
constraint chain or action row, so remaining controls retain their order and
alignment. Fullscreen settings control Android system bars and use of the camera
cutout area during landscape playback.

Header settings are independent of the channel actions below the video. Existing
Subscribe and Gift preferences are retained when upgrading from the combined action group.

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
Find matching call sites before requesting mutable classes. Unrelated classes
must remain unchanged so patching fits Android Manager's memory limits.

React Native components use `runtime.ui.useEnabled(React, key)`. Preferences
synchronize when the runtime attaches and when an option changes. Filter action
models before rendering whenever possible. Tree transformations must preserve
callbacks, keys and untouched elements, with explicit traversal limits.

Resolve each option's hooks independently and require a unique match. Verify both
toggle directions, mixed group states, nested Back navigation and disabled-state
behavior. Check native playback, V2 Classic Split and Vertical View, including
theme changes, orientation changes and reduced patch selections.
