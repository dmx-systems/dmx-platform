import dm5 from 'dm5'
import axios from 'axios'
import Vue from 'vue'
import store from './store/webclient'

export default {
  loadPlugins () {
    // Init order notes:
    //  1. dmx-search provides the registerExtraMenuItems() action.
    //     dmx-search must be inited *before* any plugin which registers extra menu items.
    //  2. dmx-accesscontrol must be inited *before* dmx-workspaces.
    //     dmx-workspaces watches dmx-accesscontrol's "username" store state.
    initPlugin(require('modules/dmx-search/src/main/js/plugin.js').default)
    initPlugin(require('modules/dmx-accesscontrol/src/main/js/plugin.js').default)
    initPlugin(require('modules/dmx-workspaces/src/main/js/plugin.js').default)
    initPlugin(require('modules/dmx-topicmaps/src/main/js/plugin.js').default)
    initPlugin(require('modules/dmx-details/src/main/js/plugin.js').default)
    initPlugin(require('modules/dmx-typeeditor/src/main/js/plugin.js').default)
    initPlugin(require('modules/dmx-datetime/src/main/js/plugin.js').default)
    initPlugin(require('modules/dmx-geomaps/src/main/js/plugin.js').default)
    //
    if (DEV) {
      console.info('[DMX] You are running the webclient in development mode.\nFrontend code is hot reloaded from ' +
        'file system (instead fetched from DMX backend server).\nTo get Hot Module Replacement for your plugin add ' +
        'it to modules/dmx-webclient/src/main/js/plugin_manager.js')
    } else {
      loadPluginsFromServer()
    }
  }
}

/**
 * Registers a plugin's assets (store module, webclient components, renderers, ...).
 *
 * @param   pluginConfig    either a plugin configuration object or a function that returns a plugin configuration
 *                          object.
 */
function initPlugin (pluginConfig) {
  const _pluginConfig = typeof pluginConfig === 'function' ? pluginConfig({store, dm5, axios, Vue}) : pluginConfig
  // store module
  const storeModule = _pluginConfig.storeModule
  if (storeModule) {
    const module = storeModule.module
    // console.log('[DMX] Registering store module', storeModule.name)
    store.registerModule(
      storeModule.name,
      typeof module === 'function' ? module({dm5, axios, Vue}) : module
    )
  }
  // store watcher
  const storeWatcher = _pluginConfig.storeWatcher
  if (storeWatcher) {
    storeWatcher.forEach(watcher => {
      store.watch(watcher.getter, watcher.callback)
    })
  }
  // webclient components
  const components = _pluginConfig.components    // TODO: rename prop to "webclient"
  if (components) {
    components.forEach(compDef => {
      store.dispatch('registerComponent', compDef)
    })
  }
  // detail renderers
  registerDetailRenderers(_pluginConfig.objectRenderers, 'object')
  registerDetailRenderers(_pluginConfig.valueRenderers,  'value')
  //
  // extra menu items
  const extraMenuItems = _pluginConfig.extraMenuItems
  if (extraMenuItems) {
    store.dispatch('registerExtraMenuItems', extraMenuItems)
  }
  // topicmap type
  const topicmapType = _pluginConfig.topicmapType
  if (topicmapType) {
    store.dispatch('registerTopicmapType', topicmapType)
  }
}

function registerDetailRenderers (renderers, renderer) {
  if (renderers) {
    for (let typeUri in renderers) {
      store.dispatch('registerDetailRenderer', {renderer, typeUri, component: renderers[typeUri]})
    }
  }
}

// --- Load from server ---

/**
 * Fetches the list of installed plugins from the server, then fetches the frontend code (`/web/plugin.js`) of those
 * plugins which provide one.
 * Note: only frontend code of *external* plugins (not included in the DMX standard distro) is fetched. In contrast the
 * frontend code of the *standard* plugins is bundled along with the webclient at build time. For the standard plugins
 * no individual `plugin.js` files exist.
 */
function loadPluginsFromServer () {
  dm5.restClient.getPlugins().then(pluginInfos => {
    pluginInfos.forEach(pluginInfo => {
      if (pluginInfo.pluginFile) {
        console.log('[DMX] Fetching frontend code of plugin', pluginInfo.pluginUri)
        loadPlugin(pluginInfo).then(initPlugin)
      }
    })
  })
}

function loadPlugin (pluginInfo) {
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
  return '/' + pluginUri + '/' + pluginInfo.pluginFile
}

function loadScript (url) {
  const script = document.createElement('script')
  script.src = url
  script.onload = () => document.head.removeChild(script)
  document.head.appendChild(script)
}
