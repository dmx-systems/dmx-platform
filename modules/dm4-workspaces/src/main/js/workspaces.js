import Vue from 'vue'
import dm5 from 'dm5'

const state = {

  workspaceId: undefined,         // ID of selected workspace (number)

  workspaceTopics: undefined,     // all workspace topics readable by current user (array of dm5.Topic)

  ready: fetchWorkspaceTopics()   // a promise resolved once the workspace topics are loaded
}

const actions = {

  createWorkspace ({dispatch}, name) {
    console.log('Creating workspace', name)
    dm5.restClient.createWorkspace(name, undefined, 'dm4.workspaces.public').then(topic => {
      console.log('Workspace', topic)
      state.workspaceTopics.push(topic)
      dispatch('selectWorkspace', topic.id)
    })
  },

  selectWorkspace ({dispatch}, id) {
    // console.log('selectWorkspace', id)
    // Note: the topicmap can be selected once the workspace's topicmap topics are available
    dispatch('_selectWorkspace', id).then(() => {
      dispatch('selectTopicmapForWorkspace')
    })
  },

  /**
   * Low-level action as dispatched by router.
   *
   * Displays the given workspace in the workspace selector.
   * Displays the given workspace's topicmaps in the topicmap selector.
   * Fetches the topicmap topics for the workspace if not yet done.
   *
   * Postconditions:
   * - "workspaceId" state is up-to-date
   * - "dm4_workspace_id" cookie is up-to-date.
   *
   * @return  a promise resolved once the workspace's topicmap topics are available.
   */
  _selectWorkspace ({dispatch}, id) {
    // console.log('_selectWorkspace', id)
    state.workspaceId = id
    dm5.utils.setCookie('dm4_workspace_id', id)
    return dispatch('fetchTopicmapTopics')     // data for topicmap selector
  },

  /**
   * Low-level action as dispatched by router.
   */
  selectFirstWorkspace ({dispatch}) {
    dispatch('selectWorkspace', state.workspaceTopics[0].id)
  },

  //

  loggedIn () {
    fetchWorkspaceTopics()
  },

  loggedOut ({dispatch}) {
    fetchWorkspaceTopics().then(() => {
      if (isWorkspaceReadable()) {
        dispatch('reloadTopicmap')
      } else {
        console.log('Workspace not readable anymore')
        dispatch('clearTopicmapCache')
        dispatch('selectFirstWorkspace')
      }
    })
  },

  // WebSocket messages

  _newWorkspace (_, {workspace}) {
    state.workspaceTopics.push(workspace)
  },

  _processDirectives (_, directives) {
    // console.log(`Workspaces: processing ${directives.length} directives`)
    directives.forEach(dir => {
      switch (dir.type) {
      case "UPDATE_TOPIC":
        updateTopic(dir.arg)    // FIXME: construct dm5.Topic?
        break
      }
    })
  }
}

export default {
  state,
  actions
}

// State helper

function fetchWorkspaceTopics () {
  return dm5.restClient.getTopicsByType('dm4.workspaces.workspace').then(topics => {
    // console.log('### Workspaces ready!')
    state.workspaceTopics = topics
  })
}

function isWorkspaceReadable () {
  return state.workspaceTopics.find(workspace => workspace.id === state.workspaceId)
}

// Process directives

function updateTopic (topic) {
  const i = state.workspaceTopics.findIndex(_topic => _topic.id === topic.id)
  if (i !== -1) {
    Vue.set(state.workspaceTopics, i, topic)
  }
}
