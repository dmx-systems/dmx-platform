export default {

  state: {
    items: [],
    aboutBoxVisibility: false
  },

  actions: {

    registerHelpMenuItem ({state}, item) {
      state.items.push(item)
    },

    openAboutBox ({state}) {
      state.aboutBoxVisibility = true
    },

    closeAboutBox ({state}) {
      state.aboutBoxVisibility = false
    }
  }
}
