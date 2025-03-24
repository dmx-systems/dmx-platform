export default ({dmx}) => ({

  state: {

    visible: false,     // Detail panel visibility
    pinned: false,      // Pin toggle state

    tab: 'info',        // Selected tab: 'info', 'related', 'meta', 'config'.
                        // Note: form edit takes place in "info" tab, while 'mode' is set to 'form'.

    mode: 'info',       // Mode of the "info" tab: 'info' or 'form'.

    configDefs: {}      // As received from BE's ConfigService
  },

  actions: {

    // TODO: combine with selectDetail action?
    setDetailPanelVisibility ({state}, visible) {
      // console.log('setDetailPanelVisibility', visible)
      // Note: we expect actual boolean values (not truish/falsish).
      // The watcher is supposed to fire only on actual visibility changes (see plugin.js).
      if (typeof visible !== 'boolean') {
        throw Error(`boolean expexted, got ${typeof visible}`)
      }
      state.visible = visible
    },

    setDetailPanelPinned ({state, rootState}, pinned) {
      state.pinned = pinned
      // When unpinning an empty detail panel close it.
      // Note: no route change is involved.
      if (!pinned && !rootState.object) {
        state.visible = false
      }
    },

    selectDetail ({state}, detail) {
      if (!['info', 'edit', 'related', 'meta', 'config'].includes(detail)) {
        throw Error(`"${detail}" is not an expected detail name`)
      }
      if (detail === 'info') {
        state.tab = 'info'
        state.mode = 'info'
      } else if (detail === 'edit') {
        state.tab = 'info'
        state.mode = 'form'
      } else {
        state.tab = detail
      }
    },

    initConfigDefs ({state}) {
      dmx.rpc.getConfigDefs().then(configDefs => {
        state.configDefs = configDefs
      })
    },

    updateConfigTopic ({rootState, dispatch}, configTopic) {
      dmx.rpc.updateConfigTopic(rootState.object.id, configTopic).then(directives => {
        dispatch('_processDirectives', directives)
      })
    },

    loggedIn ({dispatch}) {
      dispatch('initConfigDefs')
    },

    loggedOut ({dispatch}) {
      dispatch('initConfigDefs')
    }
  }
})
