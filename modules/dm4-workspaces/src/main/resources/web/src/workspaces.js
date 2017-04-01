import http from 'axios'

const state = {
  workspaceTopics: []
}

const actions = {
}

// init state
http.get('/core/topic/by_type/dm4.workspaces.workspace').then(response => {
  state.workspaceTopics = response.data
})

export default {
  state,
  actions
}
