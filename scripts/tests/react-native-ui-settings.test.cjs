const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const root = path.resolve(__dirname, '../../patches/src/main/resources/reactnative');

function install() {
    const callable = {};
    const context = vm.createContext({console: {info() {}},
        RN$registerCallableModule(name, factory) { callable[name] = factory(); }});
    const React = {
        createElement(type, props) { return {type, props}; },
        isValidElement(element) { return !!element && !!element.type && !!element.props; },
        cloneElement(element, props, children) { return {type: element.type, props: {...element.props, ...props, children}}; },
        Children: {toArray(children) { return Array.isArray(children) ? [...children] : [children]; }},
        useSyncExternalStore(subscribe, snapshot) { return snapshot(); }
    };
    const opened = [];
    const Row = () => {};
    const Sheet = () => {};
    Sheet.Item = () => {};
    const modules = {7: React, 12: {BottomSheet: Sheet}, 13: {SettingsListItem: Row},
        14: {Linking: {openURL(url) { opened.push(url); return Promise.resolve(); }}}};
    vm.runInContext(fs.readFileSync(path.join(root, 'bootstrap.js'), 'utf8').replace('__TWITCH_REACT_MODULE__', '7'), context);
    vm.runInContext(fs.readFileSync(path.join(root, 'ui-settings.js'), 'utf8')
        .replace('__TWITCH_UI_SHEET_MODULE__', '12').replace('__TWITCH_UI_ROW_MODULE__', '13')
        .replace('__TWITCH_UI_NATIVE_MODULE__', '14'), context);
    let factory;
    context.__d = value => { factory = value; };
    context.__d((global, require, a, b, module, exports) => { exports.BottomSheet = Sheet; }, 12, []);
    factory(context, id => modules[id], null, null, {exports: modules[12]}, modules[12], []);
    return {render: modules[12].BottomSheet, Sheet, Row, opened, ui: context.__twitchPatchRuntime.ui, set: callable.TwitchPatchUi.set, React};
}

test('only the playback sheet receives a native UI settings row', async () => {
    const state = install();
    const child = {type: 'View', props: {testID: 'playback-settings-sheet', children: ['existing-controls']}};
    let closed = 0;
    const close = () => { closed++; };
    const props = {children: child, visible: true, onClose: close, title: 'Localized title', footer: 'footer'};
    const result = state.render(props);
    assert.equal(result.type, state.Sheet);
    assert.equal(result.props.onClose, close);
    assert.equal(result.props.footer, 'footer');
    const children = result.props.children.props.children;
    assert.equal(children[0].type, state.Row);
    assert.equal(children[0].props.title, 'UI settings');
    assert.equal(children[1], 'existing-controls');
    children[0].props.onPress();
    assert.equal(closed, 1);
    assert.deepEqual(state.opened, ['twitch-patches://settings/ui']);
    assert.equal(child.props.children.length, 1, 'original element remains unchanged');
    assert.equal(state.render(props).props.children.props.children.length, 2, 'rerenders do not duplicate rows');
    assert.equal(state.render({children: {type: 'View', props: {testID: 'other-sheet'}}}).props.children.props.testID, 'other-sheet');
    assert.equal(state.render({children: null}).props.children, null);
    assert.equal(state.render.Item, state.Sheet.Item, 'static sheet components are retained');
});

test('UI preferences update live and omitted keys remain disabled', () => {
    const state = install();
    let updates = 0;
    const unsubscribe = state.ui.subscribe(() => { updates++; });
    assert.equal(state.ui.enabled('hide_clip'), false);
    state.set('{"hide_clip":true,"hide_share":false,"invalid-key":true,"string_value":"true"}');
    assert.equal(state.ui.useEnabled(state.React, 'hide_clip'), true);
    assert.equal(state.ui.enabled('hide_share'), false);
    assert.equal(state.ui.enabled('invalid-key'), false);
    assert.equal(state.ui.enabled('string_value'), false);
    state.set('{"hide_clip":true,"hide_share":false}');
    assert.equal(updates, 1, 'identical state does not rerender');
    state.set('{}');
    assert.equal(state.ui.enabled('hide_clip'), false);
    assert.equal(updates, 2);
    unsubscribe();
    state.set('{"hide_clip":true}');
    assert.equal(updates, 2);
    state.set('broken');
    state.set('null');
    state.set('[]');
    assert.equal(state.ui.enabled('hide_clip'), true, 'malformed payload does not reset valid state');
});
