import store from './store/webclient'
import dmx from 'dmx-api'
import axios from 'axios'
import Vue from 'vue'

const plugins = []        // installed plugins; array of plugin config objects
let _extraElementUI       // a function that loads the extra Element UI components

/**
 * @returns   a promise resolved once all plugins are loaded and initialized.
 */
export default extraElementUI => {
  //
  _extraElementUI = extraElementUI
  //
  // Init order notes:
  //  1. dmx-search provides the registerExtraMenuItems() action.
  //     dmx-search must be inited *before* any plugin which registers extra menu items.
  //  2. dmx-accesscontrol must be inited *before* dmx-workspaces.
  //     dmx-workspaces watches dmx-accesscontrol's "username" store state.
  initPlugin(require('modules/dmx-base/src/main/js/plugin.js').default)
  initPlugin(require('modules/dmx-help-menu/src/main/js/plugin.js').default)
  initPlugin(require('modules/dmx-search/src/main/js/plugin.js').default)
  initPlugin(require('modules/dmx-accesscontrol/src/main/js/plugin.js').default)
  initPlugin(require('modules/dmx-files/src/main/js/plugin.js').default)
  initPlugin(require('modules/dmx-workspaces/src/main/js/plugin.js').default)
  initPlugin(require('modules/dmx-topicmaps/src/main/js/plugin.js').default)
  initPlugin(require('modules/dmx-details/src/main/js/plugin.js').default)
  initPlugin(require('modules/dmx-typeeditor/src/main/js/plugin.js').default)
  initPlugin(require('modules/dmx-datetime/src/main/js/plugin.js').default)
  initPlugin(require('modules/dmx-contacts/src/main/js/plugin.js').default)
  //
  // while development add your plugins here
  // initPlugin(require('modules-external/my-plugin/src/main/js/plugin.js').default)
  //
  let p     // a promise resolved once the assets of all installed plugins are registered
  if (DEV) {
    console.info('[DMX] You are running the DMX webclient in development mode.\nAll frontend code is hot reloaded ' +
      'from file system (instead retrieved through DMX server).\nTo get Hot Module Replacement add your plugin to ' +
      'modules/dmx-webclient/src/main/js/plugin-manager.js')
    p = Promise.resolve()
  } else {
    p = fetchPluginsFromServer().then(plugins => Promise.all(plugins))
  }
  // invoke init hook
  return p.then(() => {
    plugins.forEach(plugin => plugin.init && plugin.init())
  })
}

/**
 * Registers a plugin's assets (store module, webclient components, renderers, ...).
 *
 * @param   pluginConfig    either a plugin config object or a function that returns a plugin config object.
 */
function initPlugin (pluginConfig) {
  const _pluginConfig = typeof pluginConfig === 'function' ? pluginConfig({store, dmx, axios, Vue}) : pluginConfig
  // register plugin
  plugins.push(_pluginConfig)
  // store module
  const storeModule = _pluginConfig.storeModule
  if (storeModule) {
    const module = storeModule.module
    // console.log('[DMX] Registering store module', storeModule.name)
    store.registerModule(
      storeModule.name,
      typeof module === 'function' ? module({dmx, axios, Vue}) : module
    )
  }
  // store watcher
  const storeWatcher = _pluginConfig.storeWatcher
  storeWatcher && storeWatcher.forEach(watcher => {
    store.watch(watcher.getter, watcher.callback)
  })
  // extra Element UI components
  _pluginConfig.extraElementUI && _extraElementUI()
  // webclient components
  const components = _pluginConfig.components
  components && components.forEach(compDef => store.dispatch('registerComponent', compDef))
  // detail renderers
  registerDetailRenderers(_pluginConfig.objectRenderers, 'object')
  registerDetailRenderers(_pluginConfig.valueRenderers,  'value')
  // icon renderers
  registerIconRenderers(_pluginConfig.iconRenderers)
  // extra menu items (create menu)
  const extraMenuItems = _pluginConfig.extraMenuItems
  extraMenuItems && store.dispatch('registerExtraMenuItems', extraMenuItems)
  // help menu items
  const helpMenuItems = _pluginConfig.helpMenuItems
  helpMenuItems && helpMenuItems.forEach(item => store.dispatch('registerHelpMenuItem', item))
  // topicmap type
  const topicmapType = _pluginConfig.topicmapType
  topicmapType && store.dispatch('registerTopicmapType', topicmapType)
  // topicmap commands / workspace commands
  registerToolbarCommands(_pluginConfig.topicmapCommands,  'registerTopicmapCommand')
  registerToolbarCommands(_pluginConfig.workspaceCommands, 'registerWorkspaceCommand')
  // context commands
  const contextCommands = _pluginConfig.contextCommands
  contextCommands && store.dispatch('registerContextCommands', contextCommands)
  // detail panel buttons
  registerDetailPanelButtons(_pluginConfig.detailPanelButtons)
  // login extensions
  const loginExtensions = _pluginConfig.loginExtensions
  loginExtensions && loginExtensions.forEach(ext => store.dispatch('registerLoginExtension', ext))
}

function registerDetailRenderers (renderers, renderer) {
  if (renderers) {
    for (const typeUri in renderers) {
      store.dispatch('registerDetailRenderer', {renderer, typeUri, component: renderers[typeUri]})
    }
  }
}

function registerIconRenderers (renderers) {
  renderers && Object.entries(renderers).forEach(([typeUri, iconFunc]) => {
    store.dispatch('registerIconRenderer', {typeUri, iconFunc})
  })
}

function registerToolbarCommands (commands, action) {
  commands && Object.entries(commands).forEach(([topicmapTypeUri, comps]) => {
    comps.forEach(comp => {
      store.dispatch(action, {topicmapTypeUri, comp})
    })
  })
}

function registerDetailPanelButtons (buttons) {
  buttons && Object.entries(buttons).forEach(([typeUri, buttons]) => {
    store.dispatch('registerDetailPanelButtons', {typeUri, buttons})
  })
}

// --- Fetch from server ---

/**
 * Fetches the list of installed plugins from the server, then fetches the frontend script and style of those
 * plugins which provide one.
 * Note: only frontend script/style of *external* plugins (not included in the DMX distro) is fetched.
 * In contrast the frontend script of the *standard* plugins is "linked" into the Webclient at build time.
 * A standard plugin's .jar file does not contain a `plugin.js` file.
 *
 * @return  a promise for an array containing promises for all installed plugins. These promises
 *          are resolved once the respective plugin is fetched and its assets are registered.
 */
function fetchPluginsFromServer () {
  return dmx.rpc.getPlugins().then(pluginInfos => {
    const plugins = []      // array of promises for plugin exports
    console.group('[DMX] Fetching plugins')
    pluginInfos.forEach(pluginInfo => {
      if (pluginInfo.pluginFile || pluginInfo.styleFile) {
        console.group(pluginInfo.pluginUri)
        if (pluginInfo.pluginFile) {
          console.log('script', pluginInfo.pluginFile)
          plugins.push(fetchPlugin(pluginInfo).then(initPlugin))
        }
        if (pluginInfo.styleFile) {
          console.log('stylesheet', pluginInfo.styleFile)
          loadCSS(styleURL(pluginInfo))
        }
        console.groupEnd()
      }
    })
    console.groupEnd()
    return plugins
  })
}

/**
 * Fetches a plugin from server.
 *
 * @return  a promise for the plugin's export.
 */
function fetchPlugin (pluginInfo) {
  const p = installCallback(pluginInfo.pluginUri)
  loadScript(pluginURL(pluginInfo))
  return p
}

function installCallback (pluginUri) {
  return new Promise(resolve => {
    const _pluginIdent = pluginIdent(pluginUri)
    window[_pluginIdent] = pluginExports => {
      delete window[_pluginIdent]
      resolve(pluginExports.default)
    }
  })
}

function pluginIdent (pluginUri) {
  return '_' + pluginUri.replace(/[.-]/g, '_')
}

function pluginURL (pluginInfo) {
  return '/' + pluginInfo.pluginUri + '/' + pluginInfo.pluginFile
}

function styleURL (pluginInfo) {
  return '/' + pluginInfo.pluginUri + '/' + pluginInfo.styleFile
}

function loadScript (url) {
  const script = document.createElement('script')
  script.src = url
  script.onload = () => document.head.removeChild(script)
  document.head.appendChild(script)
}

function loadCSS (url) {
  const link = document.createElement('link')
  link.rel = 'stylesheet'
  link.href = url
  document.head.appendChild(link)
}
