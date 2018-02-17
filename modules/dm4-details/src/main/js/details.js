const state = {

  visible: false,   // Trueish if the detail panel is visible.
  tab: 'info',      // The selected tab: 'info', 'related', ... Always defined.
  mode: 'info'      // 'info' or 'form'
}

const actions = {

  toggleDetailPanelVisibility ({dispatch}) {
    state.visible = !state.visible
    if (state.visible) {
      dispatch('callDetailRoute', state.tab)
    } else {
      dispatch('stripDetailFromRoute')
    }
  },

  setDetailPanelVisibility (visible) {
    state.visible = visible
  },

  selectDetail (_, detail) {
    // console.log('selectDetail', detail)
    state.tab = detail
  }
}

export default {
  state,
  actions
}
