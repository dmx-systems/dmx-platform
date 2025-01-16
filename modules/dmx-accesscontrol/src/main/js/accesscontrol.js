import dmx from 'dmx-api'

export default {

  state: {

    username: undefined,      // the logged in user (String); falsish if no user is logged in

    // Login dialog
    visible: false,           // Login dialog visibility
    extensions: []            // extra Vue components for the Login dialog
  },

  actions: {

    loggedIn ({state}, username) {
      DEV && console.log('Login', username)
      state.username = username
    },

    logout ({state, dispatch}) {
      DEV && console.log('Logout', state.username)
      // Note: once logout request is sent we must succeed synchronously. Plugins may perform further
      // requests in their "loggedOut" handler which may rely on up-to-date login/logout state.
      dmx.rpc.logout().then(() => {
        state.username = undefined
        dispatch('loggedOut')
      })
    },

    openLoginDialog ({state}) {
      state.visible = true
    },

    closeLoginDialog ({state}) {
      state.visible = false
    },

    revealUsername ({state, dispatch}) {
      dmx.rpc.getTopicByValue('dmx.accesscontrol.username', state.username).then(topic => {
        dispatch('revealTopic', {topic})
      })
    },

    registerLoginExtension ({state}, ext) {
      state.extensions.push(ext)
    },

    createUserAccount (_, {username, password}) {
      return dmx.rpc.createUserAccount(username, password)
    }
  }
}
