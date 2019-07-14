const state = {
  dialogVisibility: false      // Help dialog visibility
}

const actions = {

  openHelpDialog () {
    state.dialogVisibility = true
  },

  closeHelpDialog () {
    state.dialogVisibility = false
  }
}

export default {
  state,
  actions
}
