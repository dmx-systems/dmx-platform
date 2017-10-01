import dm5 from 'dm5'

var ready

const state = {
  workspaceId: undefined,       // ID of selected workspace (number)
  workspaceTopics: undefined    // all readable workspace topics (array of dm5.Topic)
}

const actions = {

  selectWorkspace ({dispatch}, id) {
    console.log('selectWorkspace', id)
    dispatch('setWorkspaceId', id)
    dispatch('selectTopicmapForWorkspace')
  },

  setWorkspaceId (_, id) {
    console.log('setWorkspaceId', id)
    // update state
    state.workspaceId = id
    dm5.utils.setCookie('dm4_workspace_id', id)
  },

  workspacesReady () {
    return ready
  }
}

// init state
ready = dm5.restClient.getTopicsByType('dm4.workspaces.workspace').then(topics => {
  console.log('### Workspaces ready!')
  state.workspaceTopics = topics
})

export default {
  state,
  actions
}
