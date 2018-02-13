import dm5 from 'dm5'
import store from './store/webclient'

export default {
  loadPlugins () {
    if (DEV) {
      console.info('You are running DM5 in development mode.\n' +
        'The standard plugins are loaded from file system and are hot replaced.')
    }
    initPlugin(require('modules/dm4-workspaces/src/main/js/plugin.js').default)
    initPlugin(require('modules/dm4-topicmaps/src/main/js/plugin.js').default)
    initPlugin(require('modules/dm4-accesscontrol/src/main/js/plugin.js').default)
    initPlugin(require('modules/dm4-typeeditor/src/main/js/plugin.js').default)
    // Note: the standard plugin jar files don't contain a plugin file (/web/plugin.js).
    // So, they are not init'ed again. ### TODO: explain better
    loadPluginsFromServer()
  }
}

/**
 * Registers a plugin's resources (store module, components, ...).
 *
 * @param   expo    The plugin.js export. Either an object or a function that returns an object.
 */
function initPlugin (expo) {
  const plugin = typeof expo === 'function' ? expo(store) : expo
  // store module
  const storeModule = plugin.storeModule
  if (storeModule) {
    console.log('Registering store module', storeModule.name)
    store.registerModule(
      storeModule.name,
      storeModule.module.default
    )
  }
  // components
  const components = plugin.components
  if (components) {
    components.forEach(comp => {
      store.dispatch('registerComponent', comp)
    })
  }
  // detail panel
  const renderers = plugin.detailPanel
  if (renderers) {
    for (let typeUri in renderers) {
      store.dispatch('registerObjectRenderer', {
        typeUri,
        component: renderers[typeUri]
      })
    }
  }
  // extra menu items
  const extraMenuItems = plugin.extraMenuItems
  if (extraMenuItems) {
    store.dispatch('registerExtraMenuItems', extraMenuItems)
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
