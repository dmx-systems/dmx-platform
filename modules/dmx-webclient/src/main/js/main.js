import dmx from 'dmx-api'
import store from './store/webclient'
import router from './router'
import loadPlugins from './plugin-manager'
import onHttpError from './error-handler'
import extraElementPlus from './element-plus'
import app from './app'
import './country-flag-polyfill'

console.log('[DMX] 2025/07/17')

const messageHandler = message => {
  store.dispatch('_' + message.type, message.args)    // FIXME: use message bus instead of actions
}

// 1) Init dmx library
const typeCacheReady = dmx.init({
  topicTypes: 'all',
  store,
  messageHandler,
  onHttpError,
  iconRenderers: store.state.iconRenderers
})

// 2) Load plugins
store.state.pluginsReady = loadPlugins(extraElementPlus)

// 3) Register app assets and mount root component
// Global component registrations
// Allow plugins to reuse Webclient components (instead of rebundle the component along with the plugin)
app.component('dmx-object-renderer', require('dmx-object-renderer').default)
app.component('dmx-assoc',           require('dmx-object-renderer/src/components/dmx-assoc').default)
app.component('dmx-boolean-field',   require('dmx-object-renderer/src/components/dmx-boolean-field').default)
app.component('dmx-child-topic',     require('dmx-object-renderer/src/components/dmx-child-topic').default)
app.component('dmx-child-topics',    require('dmx-object-renderer/src/components/dmx-child-topics').default)
app.component('dmx-html-field',      require('dmx-object-renderer/src/components/dmx-html-field').default)
app.component('dmx-number-field',    require('dmx-object-renderer/src/components/dmx-number-field').default)
app.component('dmx-player',          require('dmx-object-renderer/src/components/dmx-player').default)
app.component('dmx-select-field',    require('dmx-object-renderer/src/components/dmx-select-field').default)
app.component('dmx-text-field',      require('dmx-object-renderer/src/components/dmx-text-field').default)
app.component('dmx-value-renderer',  require('dmx-object-renderer/src/components/dmx-value-renderer').default)
app.component('dmx-topic-list', require('dmx-topic-list').default)    // Required e.g. by dmx-geomaps
app.use(store)
app.use(router)
app.mount('body')

// 4) Register own renderers
store.dispatch('registerDetailRenderer', {
  renderer: 'value',
  typeUri: 'dmx.webclient.icon',
  component: require('./components/dmx-icon-picker').default
})
store.dispatch('registerDetailRenderer', {
  renderer: 'value',
  typeUri: 'dmx.webclient.color',
  component: require('./components/dmx-color-picker').default
})
store.dispatch('registerDetailRenderer', {
  renderer: 'value',
  typeUri: 'dmx.webclient.arrow_shape',
  component: require('./components/dmx-arrow-select').default
})

// 5) Initial navigation
Promise.all([
  // dmx-webclient component relies on a populated type cache.
  typeCacheReady,
  // Initial navigation might involve "select the 1st workspace", so the workspace topics must be already loaded.
  store.state.workspaces.ready
]).then(() => {
  store.dispatch('initialNavigation')
})

// 6) Windows workaround to suppress the browser's native context menu on
//   - right-clicking the canvas (to invoke search/create dialog)
//   - right-clicking a topic/assoc (to invoke Cytoscape context menu)
//   - a dialog appears as the reaction of a Cytoscape context menu command
// Note: in contrast to other platforms on Windows the target of the "contextmenu" event is not the canvas but
//   - a dialog (or its wrapper) e.g. the search/create dialog
//   - a message box (or its wrapper) e.g. the deletion warning
// Note: in contrast to a dialog the message box is not a child of <dmx-webclient> component, so we attach
// the listener directly to <body>.
document.body.addEventListener('contextmenu', e => {
  // console.log('body', e.target.tagName, e.target.classList, e.target.parentNode.classList)
  const inDialog     = e.target.closest('.el-dialog__wrapper')
  const inMessageBox = e.target.closest('.el-message-box__wrapper')
  // console.log(inDialog, inMessageBox)
  if (inDialog || inMessageBox) {
    e.preventDefault()
  }
})
