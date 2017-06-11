import dm5 from 'dm5'

const state = {
  workspaceId: undefined,       // ID of selected workspace (number)
  workspaceTopics: undefined    // all readable workspace topics (Topic array)
}

const actions = {
  selectWorkspace (_, id) {
    console.log('select workspace', id)
    state.workspaceId = id
    dm5.utils.setCookie('dm4_workspace_id', id)
  }
}

// init state
dm5.restClient.getTopicsByType('dm4.workspaces.workspace').then(topics => {
  state.workspaceTopics = topics
})

export default {
  state,
  actions
}
