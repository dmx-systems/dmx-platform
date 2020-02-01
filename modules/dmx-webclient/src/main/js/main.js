import Vue from 'vue'
import App from './components/App'
import dm5 from 'dm5'
import store from './store/webclient'
import router from './router'
import loadPlugins from './plugin-manager'
import onHttpError from './error-handler'
import 'font-awesome/css/font-awesome.css'
import './element-ui'
import './websocket'

console.log('[DMX] 2020/02/01')

// 1) Init dm5 library
// The dm5 library must be inited *before* the dm5-webclient component is instantiated.
// The dm5-webclient component relies on the "typeCache" store module as registered by dm5.init(). ### TODO: still true?
const dm5ready = dm5.init({
  store,
  onHttpError
})

// 2) Create Vue root instance
// Instantiates router-view and dm5-webclient components.
const root = new Vue({
  el: '#app',
  store,
  router,
  render: h => h(App)
})

// 3) Load plugins
// Note: loading plugins and mounting the Weblient toplevel components (step 6) does not require synchronization at the
// moment. This is because all toplevel components (basically dm5-topicmap-panel and dm5-detail-panel) are provided by
// *standard* plugins. Standard plugins are "linked" into the Webclient at build time. At runtime no asynchronicity is
// involved.
// As soon as we want allow an *external* plugin to provide Webclient toplevel components synchronization is required.
// Note: in production mode external plugins are loaded asynchronously. Mounting can only start once loading completes.
// In contrast external plugins in *development mode* as well as standard plugins (both modes) are "linked" into the
// Webclient at build time. At runtime no asynchronicity is involved.
loadPlugins()

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

// 5) Register default context commands
// Must take place *after* the standard plugins are initialized.
// The "registerContextCommands" action is provided by the Topicmaps plugin.
store.dispatch('registerContextCommands', require('./context-commands').default)

// 6) Mount Webclient toplevel components as provided by plugins (mount point: 'webclient')
// Note: the mount point DOM exists only on next tick.
Vue.nextTick(() => {
  const webclient = root.$children[0].$children[0]    // child level 1 is <router-view>, level 2 is <dm5-webclient>
  store.dispatch('mountComponents', webclient)
})

// 7) Initial navigation
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
// - right-clicking a topic/assoc (to invoke Cytoscape context menu command)
// Note: in contrast to other platforms on Windows the target of the "contextmenu" event is not the canvas but
// - the search/create dialog (or its wrapper)
// - the delete warning message box (or its wrapper)
// Note: in contrast to the dialog the message box is not a child of <dm5-webclient> component, so we attach
// the listener directly to <body>.
document.body.addEventListener('contextmenu', e => {
  // console.log('body', e.target.tagName, e.target.classList, e.target.parentNode.classList)
  // search dialog
  const inSearchDialog = document.querySelector('.dm5-search-widget').parentNode.contains(e.target)
  // message box
  // Note: the message box wrapper is added to DOM only on 1st message box invocation
  const messageBoxWrapper = document.querySelector('.el-message-box__wrapper')
  const inMessageBox = messageBoxWrapper && messageBoxWrapper.contains(e.target)
  //
  // console.log(inSearchDialog, inMessageBox)
  if (inSearchDialog || inMessageBox) {
    e.preventDefault()
  }
})
