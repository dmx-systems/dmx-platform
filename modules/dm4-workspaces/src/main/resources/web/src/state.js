import http from 'axios'

const state = {
  workspaceTopics: []
}

const actions = {

  init () {
    http.get('/core/topic/by_type/dm4.workspaces.workspace').then(response => {
      state.workspaceTopics = response.data
      console.log('workspaceTopics', state.workspaceTopics)
    })
  }
}

export default {
  state,
  actions
}
