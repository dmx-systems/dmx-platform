console.log('Loading Workspaces state.js')

// import Vue from 'vue'
import http from 'axios'

const state = {
  workspaceTopics: []
}

const actions = {

  init () {
    http.get('/core/topic/by_type/dm4.workspaces.workspace').then(response => {
      state.workspaceTopics = response.data
    })
  }
}

export default {
  state,
  actions
}
