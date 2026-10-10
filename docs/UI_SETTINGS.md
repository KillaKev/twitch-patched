# UI settings

UI settings are available through Patch settings and live player settings. All
customization options are off by default and take effect without restarting Twitch.

Classic Split and Vertical View have separate categories:

- Player controls: Clip, Cast and Share.
- Channel actions: Subscribe, Gift a Sub and Bits.

Each group includes a Hide all switch and individual switches. Hide all reflects
the current state of its members. Hidden controls are removed from their native
constraint chain or action row, so remaining controls retain their order and
alignment. Fullscreen settings control Android system bars and use of the camera
cutout area during landscape playback.

## Adding an option

Keep each option's hooks and runtime policy under the `ui` feature directory. Add
the option to `UiOptions` through `UiFeatureRegistry.register`, then include it in
the appropriate settings group. Preference keys use lowercase letters, digits and
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
