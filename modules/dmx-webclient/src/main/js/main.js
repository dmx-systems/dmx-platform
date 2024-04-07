import Vue from 'vue'
import App from './components/App'
import dmx from 'dmx-api'
import store from './store/webclient'
import initRouter from './router'
import loadPlugins from './plugin-manager'
import onHttpError from './error-handler'
import extraElementUI from './element-ui'
import './country-flag-polyfill'

console.log('[DMX] 2024/04/07')

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
store.state.pluginsReady = loadPlugins(extraElementUI)

// 3) Create Vue root instance
new Vue({
  el: '#app',
  store,
  router: initRouter(),
  render: h => h(App)
})

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
