const state = {

  tab: undefined,     // The selected tab: 'info', 'related', ...
                      // If undefined the detail panel is not visible. ### TODO
  mode: undefined     // 'info' or 'form'
}

const actions = {

  selectDetail (_, detail) {
    // console.log('selectDetail', detail)
    state.tab = detail
  }
}

export default {
  state,
  actions
}
