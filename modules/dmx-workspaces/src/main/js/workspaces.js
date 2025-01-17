import dmx from 'dmx-api'

export default {

  state: {

    workspaceId: undefined,           // ID of selected workspace (number)

    isWritable: undefined,            // true if selected workspace is writable

    workspaceTopics: undefined,       // All workspace topics readable by current user (array of dmx.Topic).
                                      // Initialzed by 'fetchWorkspaceTopics' action

    workspaceCommands: {},            // Registered workspace commands:
                                      //   {
                                      //      topicmapTypeUri: [comp]
                                      //   }

    ready: undefined                  // A promise resolved once the workspace topics are loaded, inited by plugin.js
  },

  actions: {

    fetchWorkspaceTopics ({state}) {
      return dmx.rpc.getTopicsByType('dmx.workspaces.workspace').then(topics => {
        state.workspaceTopics = topics
      })
    },

    createWorkspace ({state, dispatch}, {name, sharingModeUri}) {
      dmx.rpc.createWorkspace(name, undefined, sharingModeUri).then(topic => {     // uri=undefined
        state.workspaceTopics.push(topic)
        selectWorkspace(topic.id, state, dispatch)
      })
    },

    /**
     * Preconditions:
     * - the route is *not* yet set.
     */
    selectWorkspace ({state, dispatch}, id) {
      selectWorkspace(id, state, dispatch)
    },

    updateWorkspaceCookie ({state}) {
      // console.log('# updateWorkspaceCookie', state.workspaceId)
      dmx.utils.setCookie('dmx_workspace_id', state.workspaceId)
    },

    /**
     * Sets the workspace state ("workspaceId" and cookie), and fetches the workspace's topicmap topics
     * if not done already.
     *
     * Low-level action (dispatched by router) that sets the workspace state *without* selecting a topicmap.
     *
     * Postconditions:
     * - "workspaceId" state is up-to-date
     * - "dmx_workspace_id" cookie is up-to-date.
     *
     * @return  a promise resolved once the workspace's topicmap topics are available.
     *          At this time the "topicmapTopics" state is up-to-date (see topicmaps module).
     */
    _selectWorkspace ({state, dispatch}, id) {
      return _selectWorkspace(id, state, dispatch)
    },

    _initWorkspaceIsWritable ({state}) {
      // workspaceId might be uninitialized. Accesscontrol "username" state is inited *before* workspaceId state. TODO?
      state.workspaceId && dmx.permCache.isWritable(state.workspaceId).then(
        writable => {
          state.isWritable = writable
        }
      )
    },

    registerWorkspaceCommand ({state}, command) {
      const c = state.workspaceCommands
      const uri = command.topicmapTypeUri
      const commands = c[uri] || (c[uri] = [])
      commands.push(command.comp)
    },

    //

    loggedIn ({dispatch}) {
      dispatch('fetchWorkspaceTopics')
    },

    loggedOut ({state, dispatch}) {
      dispatch('fetchWorkspaceTopics')
        .then(() => dispatch('clearTopicmap'))
        .then(() => dispatch('initTypeCache'))
        .then(() => {
          if (isWorkspaceReadable(state)) {
            // Note: 'clearTopicmapCache' is dispatched inside 'reloadTopicmap'
            dispatch('reloadTopicmap')
          } else {
            dispatch('clearTopicmapCache')
            selectFirstWorkspace(state, dispatch)
          }
        })
    },

    // WebSocket messages

    _newWorkspace ({state}, {workspace}) {
      state.workspaceTopics.push(workspace)
    },

    _processDirectives ({state, dispatch}, directives) {
      directives.forEach(dir => {
        let topic
        switch (dir.type) {
        case 'UPDATE_TOPIC':
          topic = new dmx.Topic(dir.arg)
          if (topic.typeUri === 'dmx.workspaces.workspace') {
            updateWorkspace(topic, state)
          }
          break
        case 'DELETE_TOPIC':
          topic = new dmx.Topic(dir.arg)
          if (topic.typeUri === 'dmx.workspaces.workspace') {
            deleteWorkspace(topic, state, dispatch)
          }
          break
        }
      })
    }
  }
}

// Actions helper

/**
 * Called after workspace deletion and after logout (see loggedOut() below).
 *
 * Preconditions:
 * - the route is *not* yet set.
 */
function selectFirstWorkspace (state, dispatch) {
  selectWorkspace(state.workspaceTopics[0].id, state, dispatch)
}

function selectWorkspace (id, state, dispatch) {
  _selectWorkspace(id, state, dispatch).then(() => {
    // the workspace's topicmap topics are now available
    dispatch('selectTopicmapForWorkspace')
  })
}

function _selectWorkspace (id, state, dispatch) {
  state.workspaceId = id
  dispatch('updateWorkspaceCookie')
  return dispatch('fetchTopicmapTopics')     // data for topicmap selector
}

// State helper

function findWorkspaceTopic (id, callback, state) {
  const i = state.workspaceTopics.findIndex(topic => topic.id === id)
  if (i !== -1) {
    callback(state.workspaceTopics, i)
  }
}

function isWorkspaceReadable (state) {
  return state.workspaceTopics.find(workspace => workspace.id === state.workspaceId)
}

// Process directives

/**
 * Processes an UPDATE_TOPIC directive.
 * Updates the workspace menu when a workspace is renamed.
 */
function updateWorkspace (topic, state) {
  // update state
  findWorkspaceTopic(topic.id, (topics, i) => topics[i] = topic, state)
}

/**
 * Processes a DELETE_TOPIC directive.
 */
function deleteWorkspace (topic, state, dispatch) {
  // update state
  findWorkspaceTopic(topic.id, (topics, i) => topics.splice(i, 1), state)
  // redirect
  if (topic.id === state.workspaceId) {
    selectFirstWorkspace(state, dispatch)
  }
}
