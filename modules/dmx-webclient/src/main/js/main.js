import Vue from 'vue'
import App from './components/App'
import dmx from 'dmx-api'
import store from './store/webclient'
import initRouter from './router'
import loadPlugins from './plugin-manager'
import onHttpError from './error-handler'
import extraElementUI from './element-ui'

console.log('[DMX] 2023/10/30')

const messageHandler = message => {
  store.dispatch('_' + message.type, message.args)    // FIXME: use message bus instead of actions
}

// 1) Init dmx library
// The dmx library must be inited *before* the dmx-webclient component is instantiated.
// The dmx-webclient component relies on the "typeCache" store module as registered by dmx.init(). ### TODO: still true?
const typeCacheReady = dmx.init({
  topicTypes: 'all',
  store,
  messageHandler,
  onHttpError,
  iconRenderers: store.state.iconRenderers
})

// 2) Create Vue root instance
// This includes instantiation of the router-view component and, after initial navigation, instantiation of the
// dmx-webclient component and its child components (as provided by plugins), e.g. Topicmap Panel and the Detail Panel.
const root = new Vue({
  el: '#app',
  store,
  router: initRouter(),
  render: h => h(App)
})

// 3) Load plugins
const pluginsReady = loadPlugins(extraElementUI)    // FIXME: sync Vue root instantiation (2), or Initial navigation (5)

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
