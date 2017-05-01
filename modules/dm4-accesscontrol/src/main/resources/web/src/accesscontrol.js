import dm5 from 'dm5'

const state = {
  user: '',           // the logged in user (string); falsish if no user is logged in
  loginDialog: false  // true if the Login dialog is open
}

const actions = {

  login (_, credentials) {
    return dm5.restClient.login(credentials)
  },

  logout () {
    dm5.restClient.logout()
  },

  openLoginDialog() {
    state.loginDialog = true
  },

  closeLoginDialog() {
    state.loginDialog = false
  },

  setUser (_, user) {
    state.user = user
  }
}

// init state
dm5.restClient.getUsername().then(username => {
  state.user = username
})

export default {
  state,
  actions
}
