import dm5 from 'dm5'

const state = {
  workspaceId: undefined,       // ID of selected workspace (number)
  workspaceTopics: undefined    // all readable workspace topics (array of dm5.Topic)
}

const actions = {
  selectWorkspace (_, id) {
    console.log('Selecting workspace', id)
    // update state
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
