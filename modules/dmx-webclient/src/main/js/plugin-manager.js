import dm5 from 'dm5'
import axios from 'axios'
import Vue from 'vue'
import store from './store/webclient'

export default {
  loadPlugins () {
    // Note: dmx-search provides the registerExtraMenuItems() action.
    // dmx-search must be inited *before* any plugin which registers extra menu items.
    initPlugin(require('modules/dmx-search/src/main/js/plugin.js').default)
    // Note: dmx-accesscontrol must be inited *before* dmx-workspaces.
    // dmx-workspaces watches dmx-accesscontrol's "username" store state.
    initPlugin(require('modules/dmx-accesscontrol/src/main/js/plugin.js').default)
    initPlugin(require('modules/dmx-workspaces/src/main/js/plugin.js').default)
    initPlugin(require('modules/dmx-topicmaps/src/main/js/plugin.js').default)
    initPlugin(require('modules/dmx-details/src/main/js/plugin.js').default)
    initPlugin(require('modules/dmx-typeeditor/src/main/js/plugin.js').default)
    initPlugin(require('modules/dmx-geomaps/src/main/js/plugin.js').default)
    // Note: the standard plugin jar files don't contain a plugin file (/web/plugin.js).
    // So, they are not init'ed again. ### TODO: explain better
    loadPluginsFromServer()
  }
}

/**
 * Registers a plugin's assets (store module, webclient components, ...).
 *
 * @param   expo    The plugin.js default export.
 *                  Either an object or a function that returns an object.
 */
function initPlugin (expo) {
  const plugin = typeof expo === 'function' ? expo({store, dm5, axios, Vue}) : expo
  // store module
  const storeModule = plugin.storeModule
  if (storeModule) {
    const module = storeModule.module
    console.log('[DMX] Registering store module', storeModule.name)
    store.registerModule(
      storeModule.name,
      typeof module === 'function' ? module({dm5, axios, Vue}) : module
    )
  }
  // store watcher
  const storeWatcher = plugin.storeWatcher
  if (storeWatcher) {
    storeWatcher.forEach(watcher => {
      store.watch(watcher.getter, watcher.callback)
    })
  }
  // webclient components
  const components = plugin.components    // TODO: rename prop to "webclient"
  if (components) {
    components.forEach(compDef => {
      store.dispatch('registerComponent', compDef)
    })
  }
  // detail renderers
  registerDetailRenderers(plugin.objectRenderers, 'object')
  registerDetailRenderers(plugin.valueRenderers,  'value')
  //
  // extra menu items
  const extraMenuItems = plugin.extraMenuItems
  if (extraMenuItems) {
    store.dispatch('registerExtraMenuItems', extraMenuItems)
  }
  // topicmap type
  const topicmapType = plugin.topicmapType
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

function loadPluginsFromServer () {
  dm5.restClient.getPlugins().then(plugins => {
    plugins.forEach(pluginInfo => {
      if (pluginInfo.hasPluginFile) {
        loadPlugin(pluginInfo.pluginUri, function (plugin) {
          initPlugin(plugin.default)
        })
      }
    })
  })
}

function loadPlugin (pluginUri, callback) {
  installCallback(pluginUri, callback)
  loadScript(pluginURL(pluginUri))
}

function installCallback (pluginUri, callback) {
  var _pluginIdent = pluginIdent(pluginUri)
  window[_pluginIdent] = function (exports) {
    delete window[_pluginIdent]
    callback(exports)
  }
}

function pluginIdent (pluginUri) {
  return '_' + pluginUri.replace(/[.-]/g, '_')
}

function pluginURL (pluginUri) {
  return '/' + pluginUri + '/plugin' + '.js'
}

function loadScript (url) {
  var script = document.createElement('script')
  script.src = url
  script.onload = function () {
    document.head.removeChild(script)
  }
  document.head.appendChild(script)
}
