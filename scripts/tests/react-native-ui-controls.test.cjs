const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const root = path.resolve(__dirname, '../../patches/src/main/resources/reactnative');

function fixture() {
    const adapters = {}, preferences = {}, modules = {};
    const runtime = {ui: {useEnabled(React, key) { return preferences[key] === true; }}, log() {},
        target(id, name, adapter) { adapters[name] = adapter; }, decorate(id, name, adapter) { adapters[name] = adapter; }};
    const React = {
        createElement(type, props, ...children) { return {type, props: children.length ? {...props, children: children.length === 1 ? children[0] : children} : props}; },
        isValidElement(node) { return Boolean(node && node.type && node.props); },
        cloneElement(node, props) { return {type: node.type, props: {...node.props, ...props}}; },
        createContext(value) { return {current: value, Provider: 'Provider'}; },
        useContext(context) { return context.current; }
    };
    const source = fs.readFileSync(path.join(root, 'ui-controls.js'), 'utf8')
        .replace(/__TWITCH_UI_[A-Z_]+_MODULE__/g, '10');
    vm.runInNewContext(source, {globalThis: {__twitchPatchRuntime: runtime}});
    return {React, runtime, preferences, adapters, modules,
        adapt(name, original) { return adapters[name](original, React, runtime, () => modules); }};
}

test('hiding controls removes their wrappers and preserves the remaining order and actions', () => {
    const f = fixture(), e = f.React.createElement;
    const press = () => {};
    const bits = e('Tooltip', {children: e('Button', {testID: 'row-bits'})});
    const subscribe = e('Button', {testID: 'row-subscribe', onPress: press});
    const gift = e('Button', {testID: 'row-gift'});
    const row = e('View', {style: {flexDirection: 'row'}, children: [subscribe, bits, gift]});
    assert.equal(f.runtime.ui.filterControls(f.React, row, {bits: false}), row);
    const result = f.runtime.ui.filterControls(f.React, row, {bits: true});
    assert.deepEqual(Array.from(result.props.children), [subscribe, gift]);
    assert.equal(result.props.children[0].props.onPress, press);
    assert.equal(result.props.style, row.props.style);
    assert.equal(row.props.children.length, 3);
    assert.equal(f.runtime.ui.filterControls(f.React, row, {bits: true, subscribe: true, gift: true}), null);
});

test('native support models filter before layout and keep Follow and notifications intact', () => {
    const f = fixture(), original = () => {};
    const render = f.adapt('SupportActionRow', original);
    const props = {buttons: [{kind: 'follow'}, {kind: 'gift'}, {kind: 'subscribe'}, {kind: 'upgrade'}], onOpenSubscribe() {}};
    assert.equal(render(props).props, props);
    f.preferences.classic_support_hide_subscribe = true;
    assert.deepEqual(Array.from(render(props).props.buttons, button => button.kind), ['follow', 'gift']);
    assert.equal(render(props).props.onOpenSubscribe, props.onOpenSubscribe);
    assert.equal(props.buttons.length, 4);
    const vertical = {...props, twitchPatchUiLayout: 'vertical'};
    assert.equal(render(vertical).props, vertical);
    f.preferences.vertical_support_hide_gift = true;
    assert.deepEqual(Array.from(render(vertical).props.buttons, button => button.kind), ['follow', 'subscribe', 'upgrade']);
    const notificationSettings = {notificationSetting: 'always', notificationsLoaded: true, onOpenSubscribe() {}};
    f.preferences.classic_support_hide_gift = true;
    const empty = render({...notificationSettings, buttons: [{kind: 'subscribe'}, {kind: 'gift'}]});
    assert.equal(empty.type, original);
    assert.equal(empty.props.buttons.length, 0);
    assert.equal(empty.props.notificationSetting, 'always');
    assert.equal(empty.props.onOpenSubscribe, notificationSettings.onOpenSubscribe);
});

test('chat Bits visibility follows its layout and restores the original availability', () => {
    const f = fixture(), original = () => {}, props = {showBitsButton: true, onOpenQuickCheering() {}};
    const render = f.adapt('ChatComposerBar', original);
    f.preferences.vertical_support_hide_bits = true;
    assert.equal(render(props).props, props);
    f.preferences.classic_support_hide_bits = true;
    assert.equal(render(props).props.showBitsButton, false);
    assert.equal(render(props).props.onOpenQuickCheering, props.onOpenQuickCheering);
    f.React.useContext = () => 'vertical';
    assert.equal(render(props).props.showBitsButton, false);
    f.preferences.vertical_support_hide_bits = false;
    assert.equal(render(props).props, props);
    const unavailable = {...props, showBitsButton: false};
    assert.equal(render(unavailable).props, unavailable);
    assert.equal(props.showBitsButton, true);
});

test('classic and vertical player controls observe separate options without hiding Reload or volume', () => {
    const f = fixture(), e = f.React.createElement;
    const Clip = () => {}, Row = () => {};
    f.modules.TheatreClipButton = Clip;
    f.modules.SupportActionRow = Row;
    const tree = e('View', {children: [e(Clip, {}), e('Button', {testID: 'player-controls-cast'}),
        e('Button', {testID: 'player-controls-mute'}), e('Button', {testID: 'player-controls-reload'})]});
    const classic = f.adapt('PlayerControlsOverlay', () => tree);
    const vertical = f.adapt('SkylineControlsOverlay', () => tree);
    assert.equal(classic({}), tree);
    f.preferences.classic_controls_hide_clip = true;
    f.preferences.classic_controls_hide_cast = true;
    assert.deepEqual(Array.from(classic({}).props.children, node => node.props.testID), ['player-controls-mute', 'player-controls-reload']);
    assert.equal(vertical({}), tree);
    f.preferences.classic_controls_hide_clip = false;
    f.preferences.classic_controls_hide_cast = false;
    assert.equal(classic({}), tree);
});

test('vertical support hosts mark their own rows and leave unrelated children unchanged', () => {
    const f = fixture(), e = f.React.createElement;
    const Row = () => {}, unrelated = e('Button', {});
    f.modules.SupportActionRow = Row;
    const tree = e('View', {children: [unrelated, e(Row, {buttons: []})]});
    const render = f.adapt('SkylineBandActions', () => tree);
    const result = render({});
    assert.equal(result.props.children[0], unrelated);
    assert.equal(result.props.children[1].props.twitchPatchUiLayout, 'vertical');
    assert.equal(tree.props.children[1].props.twitchPatchUiLayout, undefined);
});

test('ambiguous deep trees fail without partially removing controls', () => {
    const f = fixture(), e = f.React.createElement;
    let tree = e('Button', {testID: 'row-bits'});
    for (let i = 0; i < 40; i++) tree = e('View', {children: tree});
    assert.equal(f.runtime.ui.filterControls(f.React, tree, {bits: true}), tree);
});

test('UI decorators coexist with the Reload adapter in either registration order', () => {
    for (const first of [true, false]) {
        const context = vm.createContext({console: {info() {}}, RN$registerCallableModule() {}});
        vm.runInContext(fs.readFileSync(path.join(root, 'bootstrap.js'), 'utf8').replace('__TWITCH_REACT_MODULE__', '7'), context);
        const runtime = context.__twitchPatchRuntime;
        const decorate = () => runtime.decorate(9, 'PlayerControlsOverlay', original => props => [...original(props), 'ui']);
        const reload = () => runtime.target(9, 'PlayerControlsOverlay', original => props => [...original(props), 'reload']);
        if (first) { decorate(); reload(); } else { reload(); decorate(); }
        assert.throws(reload, /Duplicate/);
        let factory;
        context.__d = value => { factory = value; };
        context.__d((global, require, a, b, module) => { module.exports.PlayerControlsOverlay = () => ['volume']; }, 9);
        const module = {exports: {}};
        factory(context, () => ({}), null, null, module, module.exports);
        assert.deepEqual(Array.from(module.exports.PlayerControlsOverlay({})), first ? ['volume', 'ui', 'reload'] : ['volume', 'reload', 'ui']);
    }
});
