import dmx from 'dmx-api'
import SHA256 from './lib/sha256'

const ENCODED_PASSWORD_PREFIX = '-SHA256-'

const state = {
  username: undefined,      // the logged in user (string); falsish if no user is logged in
  // Login dialog
  visible: false,           // Login dialog visibility
  extensions: []            // extra Vue components for the Login dialog
}

const actions = {

  loggedIn (_, username) {
    console.log('Login', username)
    state.username = username
  },

  logout ({dispatch}) {
    console.log('Logout', state.username)
    // Note: once logout request is sent we must succeed synchronously. Plugins may perform further
    // requests in their "loggedOut" handler which may rely on up-to-date login/logout state.
    dmx.rpc.logout().then(() => {
      state.username = undefined
      dispatch('loggedOut')
    })
  },

  openLoginDialog () {
    state.visible = true
  },

  closeLoginDialog () {
    state.visible = false
  },

  revealUsername ({dispatch}) {
    dmx.rpc.getTopicByValue('dmx.accesscontrol.username', state.username).then(topic => {
      dispatch('revealTopic', {topic})
    })
  },

  registerLoginExtension (_, ext) {
    state.extensions.push(ext)
  },

  createUserAccount (_, {username, password}) {
    return dmx.rpc.createUserAccount(username, encodePassword(password))
  }
}

// init state

dmx.rpc.getUsername().then(username => {
  state.username = username
})

// helper

function encodePassword (password) {
  return ENCODED_PASSWORD_PREFIX + SHA256(password)
}

//

export default {
  state,
  actions
}
