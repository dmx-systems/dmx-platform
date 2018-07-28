import Vue from 'vue'
import dm5 from 'dm5'

const state = {

  workspaceId: undefined,         // ID of selected workspace (number)

  workspaceTopics: undefined,     // all workspace topics readable by current user (array of dm5.Topic)

  ready: fetchWorkspaceTopics()   // a promise resolved once the workspace topics are loaded
}

const actions = {

  createWorkspace ({dispatch}, {name, sharingModeUri}) {
    console.log('createWorkspace', name, sharingModeUri)
    dm5.restClient.createWorkspace(name, undefined, sharingModeUri).then(topic => {     // uri=undefined
      console.log('Workspace', topic)
      state.workspaceTopics.push(topic)
      dispatch('selectWorkspace', topic.id)
    })
  },

  /**
   * Preconditions:
   * - the route is *not* yet set.
   */
  selectWorkspace ({dispatch}, id) {
    // console.log('selectWorkspace', id)
    dispatch('_selectWorkspace', id).then(() => {
      // the workspace's topicmap topics are now available
      dispatch('selectTopicmapForWorkspace')
    })
  },

  /**
   * Sets the workspace state ("workspaceId" and cookie), and fetches the workspace's topicmap topics
   * if not done already.
   *
   * Low-level action (dispatched by router) that sets the workspace state *without* selecting a topicmap.
   *
   * Postconditions:
   * - "workspaceId" state is up-to-date
   * - "dm4_workspace_id" cookie is up-to-date.
   *
   * @return  a promise resolved once the workspace's topicmap topics are available.
   *          At this time the "topicmapTopics" state is up-to-date (see topicmaps module).
   */
  _selectWorkspace ({dispatch}, id) {
    // console.log('_selectWorkspace', id)
    state.workspaceId = id
    dm5.utils.setCookie('dm4_workspace_id', id)
    return dispatch('fetchTopicmapTopics')     // data for topicmap selector
  },

  /**
   * Low-level action as dispatched for initial navigation (see router.js)
   * and after logout (see loggedOut() below).
   *
   * Preconditions:
   * - the route is *not* yet set.
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

/**
 * Processes an UPDATE_TOPIC directive.
 * Updates the workspace menu when a workspace is renamed.
 */
function updateTopic (topic) {
  const i = state.workspaceTopics.findIndex(_topic => _topic.id === topic.id)
  if (i !== -1) {
    Vue.set(state.workspaceTopics, i, topic)
  }
}
