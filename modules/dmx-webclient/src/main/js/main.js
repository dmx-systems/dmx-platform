import Vue from 'vue'
import App from './components/App'
import dm5 from 'dmx-api'
import store from './store/webclient'
import router from './router'
import loadPlugins from './plugin-manager'
import onHttpError from './error-handler'
import extraElementUI from './element-ui'
import './websocket'

console.log('[DMX] 2020/12/04')

// 1) Init dm5 library
// The dm5 library must be inited *before* the dm5-webclient component is instantiated.
// The dm5-webclient component relies on the "typeCache" store module as registered by dm5.init(). ### TODO: still true?
const dm5ready = dm5.init({
  store,
  onHttpError,
  iconRenderers: store.state.iconRenderers
})

// 2) Create Vue root instance
// Instantiates router-view and dm5-webclient components.
const root = new Vue({
  el: '#app',
  store,
  router,
  render: h => h(App)
})

// 3) Load plugins + mount toplevel components
// Note: in order to allow external plugins to provide Webclient toplevel components -- in particular el-dialog
// boxes -- component mounting must perform not before all plugins are loaded.
// Another approach would be not to collect the toplevel components and then mounting all at once but to mount a
// plugin's components immediately while plugin initialization. However this would result in unresolved circular
// dependencies, e.g. the Webclient plugin depends on Search plugin's `registerExtraMenuItems` action while
// the Search plugin on the other hand depends on Workspaces `isWritable` state.
loadPlugins(extraElementUI).then(() => {
  // Mount Webclient toplevel components as provided by plugins (mount point: 'webclient')
  const webclient = root.$children[0].$children[0]    // child level 1 is <router-view>, level 2 is <dm5-webclient>
  store.dispatch('mountComponents', webclient)
})

// 4) Register own renderers
store.dispatch('registerDetailRenderer', {
  renderer: 'value',
  typeUri: 'dmx.webclient.icon',
  component: require('./components/dm5-icon-picker').default
})
store.dispatch('registerDetailRenderer', {
  renderer: 'value',
  typeUri: 'dmx.webclient.color',
  component: require('./components/dm5-color-picker').default
})

// 5) Initial navigation
// Initial navigation must take place *after* the webclient plugins are loaded.
// The "workspaces" store module is registered by the Workspaces plugin.
Promise.all([
  // Both, the Topicmap Panel and the Detail Panel, rely on a populated type cache.
  // The type cache must be ready *before* "initialNavigation" is dispatched.
  dm5ready,
  // Initial navigation might involve "select the 1st workspace", so the workspace
  // topics must be already loaded.
  store.state.workspaces.ready
]).then(() => {
  store.dispatch('initialNavigation')
})

// Windows workaround to suppress the browser's native context menu on
// - right-clicking the canvas (to invoke search/create dialog)
// - right-clicking a topic/assoc (to invoke Cytoscape context menu)
// - a dialog appears as the reaction of a Cytoscape context menu command
// Note: in contrast to other platforms on Windows the target of the "contextmenu" event is not the canvas but
// - a dialog (or its wrapper) e.g. the search/create dialog
// - a message box (or its wrapper) e.g. the deletion warning
// Note: in contrast to a dialog the message box is not a child of <dm5-webclient> component, so we attach
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
