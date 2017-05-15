import dm5 from 'dm5'

const state = {
  workspaceTopics: []
}

const actions = {
}

// init state
dm5.restClient.getTopicsByType('dm4.workspaces.workspace').then(topics => {
  state.workspaceTopics = topics
})

export default {
  state,
  actions
}
