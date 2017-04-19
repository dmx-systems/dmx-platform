import Vue from 'vue'
import store from './store'
import http from 'axios'

export default {
  loadPlugins () {
    console.log('Loading DM5 standard plugins ...')
    if (DEV) {
      console.info('DM5 development mode: these plugins are loaded from file system and are hot replaced')
    }
    initPlugin(require('../../../../../../dm4-workspaces/src/main/resources/web/src/main.js'))
    initPlugin(require('../../../../../../dm4-topicmaps/src/main/resources/web/src/main.js'))
    console.log('Loading 3rd-party DM5 plugins ...')
    loadPluginsFromServer()
  }
}

// --- Load from file system ---

function initPlugin (plugin) {
  plugin.default.init({Vue, store})
}

// --- Load from server ---

function loadPluginsFromServer () {
  http.get('/core/plugin').then(response => {
    response.data.forEach(pluginInfo => {
      if (pluginInfo.hasPluginFile) {
        loadPlugin(pluginInfo.pluginUri, function (plugin) {
          plugin.default.init({Vue, store})
        })
      }
    })
  })
}

function loadPlugin (pluginUri, callback) {
  console.log('Loading plugin from server', pluginUri)
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
