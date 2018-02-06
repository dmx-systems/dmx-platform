import Vue from 'vue'
import dm5 from 'dm5'

const state = {

  topicmap: undefined,        // The displayed topicmap (derived state) (a dm5.Topicmap object).
                              // Updated by "displayTopicmap" action.

  writable: undefined,        // True if the current user has WRITE permission for the displayed topicmap.
                              // Updated by "displayTopicmap" action.

  topicmapTopics: {},         // Loaded topicmap topics (including childs), grouped by workspace ID:
                              //   {
                              //     workspaceId: [topicmapTopic]    # array of dm5.Topic
                              //   }

  selectedTopicmapId: {},     // Per-workspace selected topicmap:
                              //   {
                              //     workspaceId: topicmapId
                              //   }

  selections: {},             // Per-topicmap selection entries, hashed by topicmap ID:
                              //   {
                              //     topicmapId: {
                              //       type: "topic"|"assoc"
                              //       id: topicId|assocId
                              //     }
                              //   }
                              // Topicmaps with no selection have no selection entry.

  topicmapCache: {}           // Loaded topicmaps, hashed by ID:
                              //   {
                              //     topicmapId: Topicmap         # a dm5.Topicmap
                              //   }
                              // Note: the topicmap cache is not actually reactive state.
                              // TODO: move it to a local variable?
}

const actions = {

  createTopicmap ({rootState, dispatch}, name) {
    console.log('Creating topicmap', name)
    dm5.restClient.createTopicmap(name,
      'dm4.webclient.default_topicmap_renderer',  // TODO
      false                                       // TODO
    ).then(topic => {
      console.log('Topicmap topic', topic)
      state.topicmapTopics[_workspaceId(rootState)].push(topic)
      dispatch('callTopicmapRoute', topic.id)
    })
  },

  /**
   * Displays the topicmap with the given ID.
   * The topicmap is retrieved either from cache or from server (asynchronously).
   *
   * Preconditions:
   * - the route is set.
   * - the topicmap belongs to the selected workspace ("workspaceId" state is up-to-date, see workspaces module).
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
   *            At this time the "topicmap" and "writable" states are up-to-date as well.
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

  selectTopic ({dispatch}, id) {
    dispatch('callTopicRoute', id)
  },

  selectAssoc ({dispatch}, id) {
    dispatch('callAssocRoute', id)
  },

  onBackgroundClick ({dispatch}) {
    dispatch('stripSelectionFromRoute')
  },

  onTopicDoubleClick ({dispatch}, viewTopic) {
    if (viewTopic.typeUri === 'dm4.topicmaps.topicmap') {
      dispatch('selectTopicmap', viewTopic.id)
    }
  },

  /**
   * Renders the topic with the given ID as selected in the topicmap panel.
   *
   * Preconditions:
   * - the route is set.
   * - the topic belongs to the selected topicmap ("topicmap" state is up-to-date).
   *
   * Postcondition:
   * - "selections" state is up-to-date.
   *
   * @param   p   a promise resolved once topic data has arrived and global "object" state is up-to-date.
   */
  setTopicSelection ({dispatch}, {id, p}) {
    // console.log('Setting topic selection of topicmap', _topicmapId(), 'to', id)
    // update state
    state.selections[_topicmapId()] = {type: 'topic', id}
    // sync view
    dispatch('syncSelect', {id, p})
  },

  /**
   * Renders the assoc with the given ID as selected in the topicmap panel.
   *
   * Preconditions:
   * - the route is set.
   * - the assoc belongs to the selected topicmap ("topicmap" state is up-to-date).
   *
   * Postcondition:
   * - "selections" state is up-to-date.
   */
  setAssocSelection ({dispatch}, id) {
    // console.log('Setting assoc selection of topicmap', _topicmapId(), 'to', id)
    // update state
    state.selections[_topicmapId()] = {type: 'assoc', id}
    // sync view
    dispatch('syncSelect', {id, p: Promise.resolve()})
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
    console.log('unsetSelection', _topicmapId())
    // update state
    delete state.selections[_topicmapId()]
    // sync view
    dispatch('syncUnselect')
  },

  revealTopicById ({dispatch}, topicId) {
    dm5.restClient.getTopic(topicId).then(topic => {
      dispatch('revealTopic', {
        topic,
        pos: {x: 100, y: 100},   // TODO
        select: true
      })
    })
  },

  /**
   * Reveals a topic on the topicmap panel.
   *
   * @param   topic   the topic to reveal (a dm5.Topic object).
   * @param   pos     the topic position in model coordinates (an object with "x", "y" properties).
   * @param   select  Optional: if trueish the revealed topic is selected programmatically.
   */
  revealTopic ({dispatch}, {topic, pos, select}) {
    // update state + sync view
    const op = _revealTopic(topic, pos, select, dispatch)
    // update server
    if (state.writable) {
      if (op.type === 'add') {
        dm5.restClient.addTopicToTopicmap(state.topicmap.id, topic.id, op.viewProps)
      } else if (op.type === 'show') {
        dm5.restClient.setTopicVisibility(state.topicmap.id, topic.id, true)
      }
    }
  },

  revealAssoc ({dispatch}, {assoc, select}) {
    // update state + sync view
    const op = _revealAssoc(assoc, select, dispatch)
    // update server
    if (state.writable) {
      if (op.type === 'add') {
        dm5.restClient.addAssocToTopicmap(state.topicmap.id, assoc.id)
      }
    }
  },

  // TODO: add "select" param?
  revealRelatedTopic ({dispatch}, {relTopic, pos}) {
    // update state + sync view
    const topicOp = _revealTopic(relTopic, pos, true, dispatch)      // select=true
    const assocOp = _revealAssoc(relTopic.assoc, false, dispatch)    // select=false
    // update server
    if (state.writable) {
      if (topicOp.type || assocOp.type) {
        dm5.restClient.addRelatedTopicToTopicmap(state.topicmap.id, relTopic.id, relTopic.assoc.id, topicOp.viewProps)
      }
    }
  },

  onTopicDragged (_, {id, pos}) {
    // update state
    state.topicmap.getTopic(id).setPosition(pos)
    // sync view (up-to-date already)
    // update server
    if (state.writable) {
      dm5.restClient.setTopicPosition(state.topicmap.id, id, pos)
    }
  },

  onTopicDroppedOntoTopic ({dispatch}, {topicId, droppedOntoTopicId}) {
    // TODO: display search/create widget; initiate assoc creation there
    const assocModel = {
      typeUri: 'dm4.core.association',
      role1: {
        roleTypeUri: 'dm4.core.default',
        topicId
      },
      role2: {
        roleTypeUri: 'dm4.core.default',
        topicId: droppedOntoTopicId
      }
    }
    console.log('createAssoc', assocModel)
    dm5.restClient.createAssoc(assocModel).then(assoc => {
      console.log(assoc)
      dispatch('revealAssoc', {assoc, select: true})
    }).catch(error => {
      console.error(error)
    })
  },

  /**
   * @param   pos   `model` and `render` positions
   */
  onBackgroundRightClick ({dispatch}, pos) {
    dispatch('openSearchWidget', {pos})
  },

  hideTopic ({dispatch}, id) {
    // update state
    state.topicmap.removeAssocs(id)
    state.topicmap.getTopic(id).setVisibility(false)
    dispatch('unselectIf', id)
    // sync view (up-to-date already)
    // update server
    if (state.writable) {
      dm5.restClient.setTopicVisibility(state.topicmap.id, id, false)
    }
  },

  hideAssoc ({dispatch}, id) {
    // update state
    state.topicmap.removeAssoc(id)
    dispatch('unselectIf', id)
    // sync view (up-to-date already)
    // update server
    if (state.writable) {
      dm5.restClient.removeAssocFromTopicmap(state.topicmap.id, id)
    }
  },

  deleteTopic ({dispatch}, id) {
    // update state
    state.topicmap.removeAssocs(id)
    state.topicmap.removeTopic(id)
    // sync view (up-to-date already)
    // update server
    dm5.restClient.deleteTopic(id).then(object => {
      dispatch('_processDirectives', object.directives)
    })
  },

  deleteAssoc ({dispatch}, id) {
    // update state
    state.topicmap.removeAssoc(id)
    // sync view (up-to-date already)
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
   * - the workspace is selected ("workspaceId" state is up-to-date, see workspaces module).
   * - the topicmap topics for the selected workspace are loaded ("topicmapTopics" state is up-to-date).
   * - the route is *not* yet set.
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

  clearTopicmapCache () {
    state.topicmapCache = {}
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
      case "DELETE_TOPIC":
        deleteTopic(dir.arg, dispatch)
        break
      case "UPDATE_ASSOCIATION":
        updateAssoc(dir.arg, dispatch)
        break
      case "DELETE_ASSOCIATION":
        deleteAssoc(dir.arg, dispatch)
        break
      }
    })
  }
}

export default {
  state,
  actions
}

// Topicmap Cache

// TODO: store promises in topicmap cache
function getTopicmap (id) {
  var p   // a promise for a dm5.Topicmap
  const topicmap = getCachedTopicmap(id)
  if (topicmap) {
    p = Promise.resolve(topicmap)
  } else {
    // console.log('Fetching topicmap', id)
    p = dm5.restClient.getTopicmap(id).then(topicmap => {
      cacheTopicmap(topicmap)
      return topicmap
    }).catch(error => {
      console.error(error)
    })
  }
  return p
}

function getCachedTopicmap (id) {
  return state.topicmapCache[id]
}

function cacheTopicmap (topicmap) {
  state.topicmapCache[topicmap.id] = topicmap
}

// Update state + sync view

/**
 * Preconditions:
 * - "selectedTopicmapId" state is up-to-date
 * - "workspaceId" state is up-to-date (see workspaces module)
 *
 * @returns   a promise resolved once topicmap rendering is complete.
 */
function _displayTopicmap (rootState, dispatch) {
  const id = state.selectedTopicmapId[_workspaceId(rootState)]
  const p = dm5.permCache.isTopicWritable(id).then(writable => {
    state.writable = writable
  })
  return new Promise(resolve => {
    getTopicmap(id).then(topicmap => {
      // update state
      state.topicmap = topicmap
      // sync view
      p.then(() => {
        dispatch('syncTopicmap', topicmap).then(resolve)
      })
    })
  })
}

/**
 * @param   topic   the topic to reveal (a dm5.Topic object).
 * @param   pos     the topic position in model coordinates (an object with "x", "y" properties).
 * @param   select  if trueish the revealed topic is selected programmatically.
 */
function _revealTopic (topic, pos, select, dispatch) {
  // update state
  const op = state.topicmap.revealTopic(topic, pos)
  // sync view
  if (op.type === 'add' || op.type === 'show') {
    dispatch('syncAddTopic', topic.id)
  }
  select && dispatch('selectTopic', topic.id)
  return op
}

function _revealAssoc (assoc, select, dispatch) {
  // update state
  const op = state.topicmap.revealAssoc(assoc)
  // sync view
  if (op.type === 'add') {
    dispatch('syncAddAssoc', assoc.id)
  }
  select && dispatch('selectAssoc', assoc.id)
  return op
}

// Process directives

function updateTopic (topic, dispatch) {
  // console.log('updateTopic', topic)
  // update topicmap
  const _topic = state.topicmap.getTopicIfExists(topic.id)
  if (_topic) {
    _topic.value = topic.value              // update state
    dispatch('syncTopic', topic.id)         // sync view
  }
  // update topicmap topics
  findTopicmapTopic(topic.id, (topics, i) => {
    Vue.set(topics, i, topic)
  })
}

function updateAssoc (assoc, dispatch) {
  const _assoc = state.topicmap.getAssocIfExists(assoc.id)
  if (_assoc) {
    _assoc.value = assoc.value              // update state
    _assoc.typeUri = assoc.typeUri          // update state
    dispatch('syncAssoc', assoc.id)         // sync view
  }
}

function deleteTopic (topic, dispatch) {
  const _topic = state.topicmap.getTopicIfExists(topic.id)
  if (_topic) {
    state.topicmap.removeTopic(topic.id)    // update state
    dispatch('syncRemoveTopic', topic.id)   // sync view
  }
}

function deleteAssoc (assoc, dispatch) {
  const _assoc = state.topicmap.getAssocIfExists(assoc.id)
  if (_assoc) {
    state.topicmap.removeAssoc(assoc.id)    // update state
    dispatch('syncRemoveAssoc', assoc.id)   // sync view
  }
}

// Helper

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
