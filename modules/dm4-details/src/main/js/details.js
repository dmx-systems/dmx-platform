const state = {

  // Detail panel visibility
  visible: false,

  // Selected tab: 'info', 'related', 'meta', 'view'.
  // Note: form edit takes place in "info" tab, while 'mode' is set to 'form'.
  tab: 'info',

  // Mode of the "info" tab: 'info' or 'form'.
  mode: 'info'
}

const actions = {

  toggleDetailPanelVisibility ({dispatch}) {
    if (state.visible) {
      dispatch('stripDetailFromRoute')
    } else {
      dispatch('callDetailRoute', state.tab)
    }
  },

  setDetailPanelVisibility (_, visible) {
    // Note: we expect actual boolean values (not truish/falsish) as the watcher
    // is supposed to fire on actual visibility *changes* only (see plugin.js).
    if (typeof visible !== 'boolean') {
      throw Error(`Boolean expexted but got ${typeof visible}`)
    }
    state.visible = visible
  },

  selectDetail (_, detail) {
    // console.log('selectDetail', detail)
    if (!['info', 'edit', 'related', 'meta', 'view'].includes(detail)) {
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
  }
}

export default {
  state,
  actions
}
