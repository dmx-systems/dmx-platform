import Vue from 'vue'
import dm5 from 'dm5'

const state = {

  topicmap: undefined,        // The displayed topicmap (derived state) (dm5.Topicmap).
                              // Updated by "displayTopicmap" action.
                              // ### TODO: drop it? "topicmap" state is hold in renderer.

  writable: undefined,        // True if the current user has WRITE permission for the displayed topicmap.
                              // Updated by "displayTopicmap" action.
                              // ### TODO: drop it? "writable" state is hold in renderer.

  topicmapTopics: {},         // Loaded topicmap topics (including childs), grouped by workspace ID:
                              //   {
                              //     workspaceId: [topicmapTopic]    # array of dm5.Topic
                              //   }
                              // TODO: make the array a map, key by topicmap ID?

  selectedTopicmapId: {},     // Per-workspace selected topicmap:
                              //   {
                              //     workspaceId: topicmapId
                              //   }

  selections: {},             // Per-topicmap selection entries, keyed by topicmap ID:
                              //   {
                              //     topicmapId: {
                              //       type: "topic"|"assoc"
                              //       id: topicId|assocId
                              //     }
                              //   }
                              // Topicmaps with no selection have no selection entry.

  topicmapTypes: {}           // Registered topicmap types, keyed by topicmap type URI:
                              //   {
                              //     topicmapTypeUri: {
                              //       uri:
                              //       name:
                              //       storeModule:
                              //       comp:
                              //     }
                              //   {
}

const actions = {

  createTopicmap ({rootState, dispatch}, {name, topicmapTypeUri, isPrivate}) {
    console.log('Creating topicmap', name)
    dm5.restClient.createTopicmap(name, topicmapTypeUri, isPrivate).then(topic => {
      console.log('Topicmap topic', topic)
      state.topicmapTopics[_workspaceId(rootState)].push(topic)
      dispatch('callTopicmapRoute', topic.id)
    })
  },

  /**
   * Sets the topicmap state ("selectedTopicmapId" and cookie), and displays the given topicmap.
   * The topicmap is retrieved either from cache or from server (asynchronously).
   *
   * Preconditions:
   * - the route is set.
   * - the topicmap belongs to the selected workspace ("workspaceId" state is up-to-date, see workspaces module).
   * - the workspace's topicmap topics are available.
   *
   * Postconditions:
   * - "selectedTopicmapId" state is up-to-date
   * - topicmap cookie is up-to-date.
   *
   * Note: these states are *not* yet up-to-date:
   * - "topicmap" (updated only once topicmap retrieval is complete)
   * - "writable" (updated only once permission retrieval is complete)
   *
   * @returns   a promise resolved once topicmap rendering is complete.
   *            At this time the "topicmap" and "writable" states are up-to-date.
   */
  displayTopicmap ({rootState, dispatch}, id) {
    // console.log('displayTopicmap', id)
    // update state
    state.selectedTopicmapId[_workspaceId(rootState)] = id
    dm5.utils.setCookie('dm4_topicmap_id', id)
    // update state + sync view
    return _displayTopicmap(rootState, dispatch)
  },

  /**
   * Calls the "topicmap" route with the given ID.
   * If a topic/assoc selection is known for that topicmap, the "topic" or "assoc" route is called instead.
   *
   * Preconditions:
   * - the route is *not* yet set.
   *
   * Note: the topicmap is *not* required to belong to the selected workspace.
   */
  selectTopicmap ({dispatch}, id) {
    const selection = state.selections[id]
    // console.log('selectTopicmap', id, selection)
    if (selection) {
      const type = selection.type
      dispatch('callRoute', {
        name: type,
        params: {
          topicmapId: id,
          [`${type}Id`]: selection.id
        }
      })
    } else {
      dispatch('callTopicmapRoute', id)
    }
  },

  /**
   * Preconditions:
   * - the route is *not* yet set.
   */
  selectTopic ({dispatch}, id) {
    dispatch('callTopicRoute', id)
  },

  /**
   * Preconditions:
   * - the route is *not* yet set.
   */
  selectAssoc ({dispatch}, id) {
    dispatch('callAssocRoute', id)
  },

  /**
   * Preconditions:
   * - the route is *not* yet set.
   */
  unselect ({dispatch}) {
    dispatch('stripSelectionFromRoute')
  },

  /**
   * Renders the given topic as selected in the topicmap panel.
   *
   * Preconditions:
   * - the route is set.
   * - the topic belongs to the selected topicmap ("topicmap" state is up-to-date).
   * - the topicmap rendering is complete.
   *
   * Postcondition:
   * - "selections" state is up-to-date.
   *
   * @param   p   a promise resolved once topic data has arrived (global "object" state is up-to-date).
   */
  setTopicSelection ({dispatch}, {id, p}) {
    // console.log('Setting topic selection of topicmap', _topicmapId(), 'to', id)
    // update state
    state.selections[_topicmapId()] = {type: 'topic', id}
    // sync view
    dispatch('syncSelect', {id, p})
  },

  /**
   * Renders the given assoc as selected in the topicmap panel.
   *
   * Preconditions:
   * - the route is set.
   * - the assoc belongs to the selected topicmap ("topicmap" state is up-to-date).
   * - the topicmap rendering is complete.
   *
   * Postcondition:
   * - "selections" state is up-to-date.
   *
   * @param   p   a promise resolved once assoc data has arrived (global "object" state is up-to-date).
   */
  setAssocSelection ({dispatch}, {id, p}) {
    // console.log('Setting assoc selection of topicmap', _topicmapId(), 'to', id)
    // update state
    state.selections[_topicmapId()] = {type: 'assoc', id}
    // sync view
    dispatch('syncSelect', {id, p})
  },

  /**
   * Removes the selection in the topicmap panel.
   *
   * Preconditions:
   * - the route is set.
   * - the "topicmap" state is up-to-date.
   *
   * Postcondition:
   * - "selections" state is up-to-date.
   */
  unsetSelection ({dispatch}) {
    // console.log('unsetSelection', _topicmapId())
    // update state
    delete state.selections[_topicmapId()]
    // sync view
    dispatch('syncUnselect')
  },

  revealTopicById ({dispatch}, topicId) {
    dm5.restClient.getTopic(topicId).then(topic => {
      dispatch('revealTopic', {
        topic,
        select: true
      })
    })
  },

  createAssoc ({dispatch}, {topicId1, topicId2}) {
    // TODO: display search/create widget; initiate assoc creation there
    const assocModel = {
      typeUri: 'dm4.core.association',
      role1: {topicId: topicId1, roleTypeUri: 'dm4.core.default'},
      role2: {topicId: topicId2, roleTypeUri: 'dm4.core.default'}
    }
    console.log('createAssoc', assocModel)
    dm5.restClient.createAssoc(assocModel).then(assoc => {
      console.log(assoc)
      dispatch('revealAssoc', {assoc, select: true})
    }).catch(error => {
      console.error(error)
    })
  },

  hideTopic ({dispatch}, id) {
    // update state
    dispatch('unselectIf', id)
  },

  hideAssoc ({dispatch}, id) {
    // update state
    dispatch('unselectIf', id)
  },

  deleteTopic ({dispatch}, id) {
    // update server
    dm5.restClient.deleteTopic(id).then(object => {
      dispatch('_processDirectives', object.directives)
    })
  },

  deleteAssoc ({dispatch}, id) {
    // update server
    dm5.restClient.deleteAssoc(id).then(object => {
      dispatch('_processDirectives', object.directives)
    })
  },

  //

  /**
   * Selects the topicmap last used in the selected workspace.
   * Calls the "topicmap" (or "topic"/"assoc") route.
   *
   * Preconditions:
   * - the route is *not* yet set.
   * - the workspace is selected ("workspaceId" state is up-to-date, see workspaces module).
   * - the topicmap topics for the selected workspace are loaded ("topicmapTopics" state is up-to-date).
   */
  selectTopicmapForWorkspace ({rootState, dispatch}) {
    const workspaceId = _workspaceId(rootState)
    // console.log('selectTopicmapForWorkspace', workspaceId)
    let topicmapId = state.selectedTopicmapId[workspaceId]
    if (!topicmapId) {
      topicmapId = state.topicmapTopics[workspaceId][0].id
    }
    dispatch('selectTopicmap', topicmapId)
  },

  reloadTopicmap ({rootState, dispatch}) {
    console.log('Reloading topicmap', _topicmapId())
    dispatch('clearTopicmapCache')
    _displayTopicmap(rootState, dispatch).then(() => {
      // sync view (selection)
      const selection = state.selections[_topicmapId()]
      if (selection) {
        dispatch('syncSelect', {id: selection.id, p: Promise.resolve()})
      }
    })
  },

  /**
   * Fetches the topicmap topics for the selected workspace.
   * Updates the "topicmapTopics" state.
   * If the topicmap topics for the selected workspace are fetched already nothing is performed.
   * (In this case the returned promise is already resolved.)
   *
   * Precondition:
   * - the workspace is selected ("workspaceId" state is up-to-date, see workspaces module).
   *
   * @return  a promise resolved once the "topicmapTopics" state is up-to-date.
   */
  fetchTopicmapTopics ({rootState}) {
    const workspaceId = _workspaceId(rootState)
    let p
    if (state.topicmapTopics[workspaceId]) {
      p = Promise.resolve()
    } else {
      // console.log('fetchTopicmapTopics', workspaceId)
      p = dm5.restClient.getAssignedTopics(workspaceId, 'dm4.topicmaps.topicmap', true).then(topics => {
        // console.log('### Topicmap topics ready!', topics.length)                  // includeChilds=true
        if (!topics.length) {
          throw Error(`Workspace ${workspaceId} has no topicmap`)
        }
        Vue.set(state.topicmapTopics, workspaceId, topics)
      })
    }
    return p
  },

  registerTopicmapType (_, topicmapType) {
    state.topicmapTypes[topicmapType.uri] = topicmapType
  },

  //

  loggedIn ({dispatch}) {
    dispatch('reloadTopicmap')
  },

  // Note: loggedOut is handled by workspaces module

  // WebSocket messages

  _newTopicmap (_, args) {
    const topicmapTopic = new dm5.Topic(args.topicmapTopic)
    const workspace = topicmapTopic.getChildTopic('dm4.workspaces.workspace')
    // Note: the default topicmap created for new workspaces have no workspace assignment yet
    if (!workspace) {
      console.warn('No workspace found in topicmap', topicmapTopic)
      return
    }
    const topics = state.topicmapTopics[workspace.id]
    if (topics) {
      console.log('Adding topicmap topic', topicmapTopic, 'to workspace', workspace.id)
      topics.push(topicmapTopic)
    } else {
      console.log('Ignoring topicmap topic', topicmapTopic, 'for workspace', workspace.id)
    }
  },

  _addTopicToTopicmap ({dispatch}, {topicmapId, viewTopic}) {
    if (topicmapId === state.topicmap.id) {
      state.topicmap.addTopic(new dm5.ViewTopic(viewTopic))               // update state
      dispatch('syncAddTopic', viewTopic.id)                              // sync view
    }
  },

  _addAssocToTopicmap ({dispatch}, {topicmapId, assoc}) {
    if (topicmapId === state.topicmap.id) {
      state.topicmap.addAssoc(new dm5.Assoc(assoc))                       // update state
      dispatch('syncAddAssoc', assoc.id)                                  // sync view
    }
  },

  _setTopicPosition ({dispatch}, {topicmapId, topicId, pos}) {
    if (topicmapId === state.topicmap.id) {
      state.topicmap.getTopic(topicId).setPosition(pos)                   // update state
      dispatch('syncTopicPosition', topicId)                              // sync view
    }
  },

  _setTopicVisibility ({dispatch}, {topicmapId, topicId, visibility}) {
    if (topicmapId === state.topicmap.id) {
      // update state
      if (!visibility) {
        state.topicmap.removeAssocs(topicId)
        dispatch('unselectIf', topicId)
      }
      state.topicmap.getTopic(topicId).setVisibility(visibility)
      // sync view
      dispatch('syncTopicVisibility', topicId)
    }
  },

  _removeAssocFromTopicmap ({dispatch}, {topicmapId, assocId}) {
    if (topicmapId === state.topicmap.id) {
      // update state
      state.topicmap.removeAssoc(assocId)
      dispatch('unselectIf', assocId)
      // sync view
      dispatch('syncRemoveAssoc', assocId)
    }
  },

  _processDirectives ({dispatch}, directives) {
    // console.log(`Topicmaps: processing ${directives.length} directives`)
    directives.forEach(dir => {
      switch (dir.type) {
      case "UPDATE_TOPIC":
        updateTopic(dir.arg, dispatch)    // FIXME: construct dm5.Topic?
        break
      }
    })
  }
}

export default {
  state,
  actions
}

// Update state + sync view

/**
 * Displays the selected topicmap, according to current state.
 *
 * Preconditions:
 * - "selectedTopicmapId" state is up-to-date
 * - "workspaceId" state is up-to-date (see workspaces module)
 * - the workspace's topicmap topics are available ("topicmapTopics" state is up-to-date)
 *
 * @returns   a promise resolved once topicmap rendering is complete.
 *            At this time the "topicmap" and "writable" states are up-to-date as well.
 */
function _displayTopicmap (rootState, dispatch) {
  const topicmapTopic = getTopicmapTopic(rootState)
  return topicmapTopic.isWritable()
    .then(writable => state.writable = writable)
    .then(writable => dispatch('showTopicmap', {topicmapTopic, writable}))
    .then(topicmap => {
      console.log('_displayTopicmap', topicmap)
      state.topicmap = topicmap
    })
}

// Process directives

function updateTopic (topic, dispatch) {
  // console.log('updateTopic', topic)
  findTopicmapTopic(topic.id, (topics, i) => {
    Vue.set(topics, i, topic)
  })
}

// Helper

/**
 * Returns the selected topicmap topic, according to current state.
 *
 * Preconditions:
 * - "selectedTopicmapId" state is up-to-date
 * - "workspaceId" state is up-to-date (see workspaces module)
 * - the workspace's topicmap topics are available ("topicmapTopics" state is up-to-date)
 */
function getTopicmapTopic (rootState) {
  const workspaceId = _workspaceId(rootState)
  const topicmapId = state.selectedTopicmapId[workspaceId]
  if (typeof topicmapId !== 'number') {
    throw Error(`topicmapId is expected to be of type 'number', but is ${typeof topicmapId}`)
  }
  const topicmapTopics = state.topicmapTopics[workspaceId]
  if (!topicmapTopics) {
    throw Error(`Topicmap topics of workspace ${workspaceId} not yet loaded`)
  }
  const topicmapTopic = topicmapTopics.find(topic => topic.id === topicmapId)
  if (!topicmapTopic) {
    throw Error(`Topicmap topic ${topicmapId} not found (workspace ${workspaceId})`)
  }
  return topicmapTopic
}

function findTopicmapTopic (id, callback) {
  for (const topics of Object.values(state.topicmapTopics)) {
    const i = topics.findIndex(topic => topic.id === id)
    if (i !== -1) {
      callback(topics, i)
      break
    }
  }
}

// ---

/**
 * Preconditions:
 * - the "topicmap" state is up-to-date.
 */
function _topicmapId () {
  const topicmap = state.topicmap
  if (!topicmap) {
    throw Error('No selected topicmap known')
  }
  return topicmap.id
}

function _workspaceId (rootState) {
  const workspaceId = rootState.workspaces.workspaceId
  if (!workspaceId) {
    throw Error(`No selected workspace known`)
  }
  return workspaceId
}
