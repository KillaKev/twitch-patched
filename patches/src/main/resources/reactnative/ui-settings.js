(function (global) {
    'use strict';
    var runtime = global.__twitchPatchRuntime;
    var values = Object.freeze({});
    var listeners = new Set();
    runtime.ui = {
        enabled: function (key) { return values[key] === true; },
        subscribe: function (listener) {
            listeners.add(listener);
            return function () { listeners.delete(listener); };
        },
        useEnabled: function (React, key) {
            return React.useSyncExternalStore(runtime.ui.subscribe,
                function () { return runtime.ui.enabled(key); }, function () { return false; });
        }
    };
    global.RN$registerCallableModule('TwitchPatchUi', function () {
        return {set: function (serialized) {
            var incoming;
            try { incoming = JSON.parse(serialized); }
            catch (error) { runtime.log('UI preferences invalid'); return; }
            if (!incoming || Array.isArray(incoming) || typeof incoming !== 'object') return;
            var next = Object.create(null);
            Object.keys(incoming).forEach(function (key) {
                if (/^[a-z][a-z0-9_]{0,63}$/.test(key) && typeof incoming[key] === 'boolean') next[key] = incoming[key];
            });
            if (JSON.stringify(values) === JSON.stringify(next)) return;
            values = Object.freeze(next);
            listeners.forEach(function (listener) { listener(); });
        }};
    });
    runtime.target(__TWITCH_UI_SHEET_MODULE__, 'BottomSheet', function (original, React, runtime, require) {
        var Row = require(__TWITCH_UI_ROW_MODULE__).SettingsListItem;
        var Linking = require(__TWITCH_UI_NATIVE_MODULE__).Linking;
        function Sheet(props) {
            var body = props.children;
            if (!React.isValidElement(body) || body.props.testID !== 'playback-settings-sheet')
                return React.createElement(original, props);
            var row = React.createElement(Row, {
                key: 'twitch-patches-ui-settings', title: 'UI settings', onPress: function () {
                    if (typeof props.onClose === 'function') props.onClose();
                    Linking.openURL('twitch-patches://settings/ui').catch(function () { runtime.log('UI navigation failed'); });
                }, accessibilityRole: 'button', accessibilityLabel: 'UI settings', testID: 'twitch-patches-ui-settings'
            });
            var children = React.Children.toArray(body.props.children);
            children.unshift(row);
            return React.createElement(original, Object.assign({}, props, {children: React.cloneElement(body, {}, children)}));
        }
        Object.assign(Sheet, original);
        return Sheet;
    });
})(globalThis);
