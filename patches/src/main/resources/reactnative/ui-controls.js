(function (runtime) {
    'use strict';
    function useOptions(React, prefix, parts) {
        var result = Object.create(null);
        parts.forEach(function (part) { result[part] = runtime.ui.useEnabled(React, prefix + '_hide_' + part); });
        return result;
    }
    runtime.ui.filterControls = function (React, result, hidden, clipType) {
        if (!Object.keys(hidden).some(function (key) { return hidden[key]; })) return result;
        var count = 0, limit = {};
        function visit(node, depth) {
            if (++count > 2048 || depth > 32) throw limit;
            if (Array.isArray(node)) {
                var values = node.map(function (child) { return visit(child, depth + 1); });
                return values.every(function (child, i) { return child === node[i]; }) ? node : values.filter(function (child) { return child !== null; });
            }
            if (!React.isValidElement(node)) return node;
            var id = node.props.testID;
            var part = typeof id === 'string' && /-(clip|cast|share|subscribe|upgrade|gift|bits)$/.exec(id);
            part = node.type === clipType ? 'clip' : part && (part[1] === 'upgrade' ? 'subscribe' : part[1]);
            if (part && hidden[part]) return null;
            var children = node.props.children;
            if (children === undefined) return node;
            var next = visit(children, depth + 1);
            if (next === children) return node;
            if (next === null || Array.isArray(next) && next.length === 0) return null;
            return React.cloneElement(node, {children: next});
        }
        try { return visit(result, 0); }
        catch (error) { if (error !== limit) throw error; runtime.log('UI tree limit'); return result; }
    };
    function controls(module, name, layout) {
        runtime.decorate(module, name, function (original, React, policy, require) {
            var Clip = require(__TWITCH_UI_CLIP_MODULE__).TheatreClipButton;
            return function (props) {
                var hidden = useOptions(React, layout + '_controls', ['clip', 'cast', 'share']);
                return runtime.ui.filterControls(React, original.apply(this, arguments), hidden, Clip);
            };
        });
    }
    controls(__TWITCH_UI_CLASSIC_MODULE__, 'PlayerControlsOverlay', 'classic');
    controls(__TWITCH_UI_VERTICAL_MODULE__, 'SkylineControlsOverlay', 'vertical');

    var layoutContext;
    function layout(React) { return layoutContext || (layoutContext = React.createContext('classic')); }
    runtime.decorate(__TWITCH_UI_CHAT_BAND_MODULE__, 'SkylineChatBand', function (original, React) {
        return function (props) {
            return React.createElement(layout(React).Provider, {value: 'vertical'}, React.createElement(original, props));
        };
    });
    runtime.decorate(__TWITCH_UI_COMPOSER_MODULE__, 'ChatComposerBar', function (original, React) {
        return function (props) {
            var prefix = React.useContext(layout(React));
            var hidden = runtime.ui.useEnabled(React, prefix + '_support_hide_bits');
            return React.createElement(original, hidden ? Object.assign({}, props, {showBitsButton: false}) : props);
        };
    });

    runtime.target(__TWITCH_UI_SUPPORT_ROW_MODULE__, 'SupportActionRow', function (original, React) {
        return function (props) {
            var prefix = props.twitchPatchUiLayout === 'vertical' ? 'vertical' : 'classic';
            var hidden = useOptions(React, prefix + '_support', ['subscribe', 'gift', 'bits']);
            if (!hidden.subscribe && !hidden.gift || !Array.isArray(props.buttons)) return React.createElement(original, props);
            var buttons = props.buttons.filter(function (button) {
                return !hidden[button.kind === 'upgrade' ? 'subscribe' : button.kind];
            });
            return React.createElement(original, Object.assign({}, props, {buttons: buttons}));
        };
    });
    function supportHost(module, name) {
        runtime.target(module, name, function (original, React, policy, require) {
            var Row = require(__TWITCH_UI_SUPPORT_ROW_MODULE__).SupportActionRow;
            return function (props) {
                var result = original.apply(this, arguments);
                var count = 0, limit = {};
                function visit(node, depth) {
                    if (++count > 2048 || depth > 32) throw limit;
                    if (Array.isArray(node)) {
                        var next = node.map(function (child) { return visit(child, depth + 1); });
                        return next.every(function (child, index) { return child === node[index]; }) ? node : next;
                    }
                    if (!React.isValidElement(node)) return node;
                    if (node.type === Row) return React.cloneElement(node, {twitchPatchUiLayout: 'vertical'});
                    if (node.props.children === undefined) return node;
                    var children = visit(node.props.children, depth + 1);
                    return children === node.props.children ? node : React.cloneElement(node, {children: children});
                }
                try { return visit(result, 0); }
                catch (error) { if (error !== limit) throw error; runtime.log('UI support tree limit'); return result; }
            };
        });
    }
    supportHost(__TWITCH_UI_BAND_MODULE__, 'SkylineBandActions');
    supportHost(__TWITCH_UI_SHEET_ACTIONS_MODULE__, 'SkylineSheetActions');
    runtime.target(__TWITCH_UI_OVERLAY_ACTIONS_MODULE__, 'OverlayActionButtons', function (original, React) {
        return function (props) {
            var hidden = useOptions(React, 'classic_support', ['subscribe', 'gift', 'bits']);
            return runtime.ui.filterControls(React, original.apply(this, arguments), hidden);
        };
    });
})(globalThis.__twitchPatchRuntime);
