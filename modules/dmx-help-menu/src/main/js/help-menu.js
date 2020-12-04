const state = {

  items: [],

  aboutBoxVisibility: false
}

const actions = {

  registerHelpMenuItem (_, item) {
    state.items.push(item)
  },

  openAboutBox () {
    state.aboutBoxVisibility = true
  },

  closeAboutBox () {
    state.aboutBoxVisibility = false
  }
}

export default {
  state,
  actions
}
