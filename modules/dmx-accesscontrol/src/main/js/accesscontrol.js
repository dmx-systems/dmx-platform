import dm5 from 'dm5'
import SHA256 from './lib/sha256'

const ENCODED_PASSWORD_PREFIX = '-SHA256-'

const state = {

  username: undefined,    // the logged in user (string); falsish if no user is logged in

  visible: false          // Login dialog visibility
}

const actions = {

  login ({dispatch}, credentials) {
    return dm5.restClient.login(credentials).then(() => {
      const username = credentials.username
      console.log('Login', username)
      setUsername(username)
      dm5.permCache.clear()
      dispatch('loggedIn')
      return true
    }).catch(error => {
      console.log('Login failed', error)
      return false
    })
  },

  logout ({dispatch}) {
    console.log('Logout', state.username)
    // Note: once logout request is sent we must succeed synchronously. Plugins may perform further
    // requests in their "loggedOut" handler which may rely on up-to-date login/logout state.
    dm5.restClient.logout().then(() => {
      setUsername()
      dm5.permCache.clear()
      dispatch('loggedOut')
    })
  },

  openLoginDialog () {
    state.visible = true
  },

  closeLoginDialog () {
    state.visible = false
  },

  createUserAccount (_, {username, password}) {
    return dm5.restClient.createUserAccount(username, encodePassword(password))
  }
}

// init state

dm5.restClient.getUsername().then(username => {
  state.username = username
})

// helper

function setUsername (username) {
  state.username = username
}

function encodePassword (password) {
    return ENCODED_PASSWORD_PREFIX + SHA256(password)
}

//

export default {
  state,
  actions
}
