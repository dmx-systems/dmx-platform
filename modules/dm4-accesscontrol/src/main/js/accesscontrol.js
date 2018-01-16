import dm5 from 'dm5'

const state = {

  username: undefined,    // the logged in user (string); falsish if no user is logged in

  visible: false,         // Login dialog visibility

  permissionCache: {}     // Key is a topic/association ID.
                          // Value is a promise for a permissions object:
                          //   {
                          //     "dm4.accesscontrol.operation.write": true|false
                          //   }
                          //
                          // Note 1: at client-side there is no explicit READ permission.
                          // The Webclient never gets hold of an object the user is not allowed to read.
                          // The server would not send it in the first place.
                          //
                          // Note 2: the permission cache is not actually reactive state.
                          // TODO: move it to a local variable?
}

const actions = {

  login ({dispatch}, credentials) {
    return dm5.restClient.login(credentials).then(response => {
      const username = credentials.username
      console.log('Login', username)
      setUsername(username)
      clearPermissionCache()
      dispatch('loggedIn')
      return true
    }).catch(error => {
      console.log('Login failed', error)
      return false
    })
  },

  logout () {
    console.log('Logout', state.username)
    dm5.restClient.logout()
    setUsername()
  },

  openLoginDialog () {
    state.visible = true
  },

  closeLoginDialog () {
    state.visible = false
  },

  /**
   * @return  a promise for a permissions object
   */
  getTopicPermissions (_, id) {
    return getPermissions(id, dm5.restClient.getTopicPermissions)
  },

  /**
   * @return  a promise for a permissions object
   */
  getAssocPermissions (_, id) {
    return getPermissions(id, dm5.restClient.getAssocPermissions)
  }
}

// init state

dm5.restClient.getUsername().then(username => {
  state.username = username
})

// state helper

function setUsername (username) {
  state.username = username
}

function getPermissions (id, retrievalFunc) {
  return state.permissionCache[id] || (state.permissionCache[id] = retrievalFunc(id).catch(error => {
    console.error(error)
  }))
}

function clearPermissionCache () {
  state.permissionCache = {}
}

//

export default {
  state,
  actions
}
