const state = {
  aboutBoxVisibility: false
}

const actions = {

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
