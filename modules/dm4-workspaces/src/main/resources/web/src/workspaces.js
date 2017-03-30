console.log('Loading Workspaces state.js')

import http from 'axios'

const state = {
  workspaceTopics: []
}

const actions = {

  init () {
    console.log('Workspaces state init() called!')
    http.get('/core/topic/by_type/dm4.workspaces.workspace').then(response => {
      state.workspaceTopics = response.data
    })
  }
}

export default {
  state,
  actions
}
