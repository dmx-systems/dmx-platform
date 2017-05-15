import dm5 from 'dm5'
import store from './store/webclient'

export default {
  loadPlugins () {
    if (DEV) {
      console.info('You are running DM5 in development mode.\n' +
        'The standard plugins are loaded from file system and are hot replaced.')
    }
    initPlugin(require('modules/dm4-workspaces/src/main/resources/web/src/main.js'))
    initPlugin(require('modules/dm4-topicmaps/src/main/resources/web/src/main.js'))
    initPlugin(require('modules/dm4-accesscontrol/src/main/resources/web/src/main.js'))
    //
    loadPluginsFromServer()
  }
}

function initPlugin (plugin) {
  // register store modules
  const storeModule = plugin.default.storeModule
  if (storeModule) {
    console.log('Registering store module', storeModule.name)
    store.registerModule(
      storeModule.name,
      storeModule.module.default
    )
  }
  // register components
  const components = plugin.default.components
  if (components) {
    components.forEach(comp => {
      store.dispatch('registerComponent', {
        extensionPoint: comp.extensionPoint,
        component:      comp.component
      })
    })
  }
}

// --- Load from server ---

function loadPluginsFromServer () {
  dm5.restClient.getPlugins().then(plugins => {
    plugins.forEach(pluginInfo => {
      if (pluginInfo.hasPluginFile) {
        loadPlugin(pluginInfo.pluginUri, function (plugin) {
          initPlugin(plugin)
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
