import Vue from 'vue'
import dm5 from 'dm5'
import Selection from './selection'

const state = {

  topicmap: undefined,        // the rendered topicmap (dm5.Topicmap)

  topicmapTopics: {},         // Per-workspace loaded topicmap topics (including childs):
                              //   {
                              //     workspaceId: [topicmapTopic]    # array of dm5.Topic
                              //   }
                              // TODO: make the array a map, key by topicmap ID?

  selectedTopicmapId: {},     // Per-workspace selected topicmap:
                              //   {
                              //     workspaceId: topicmapId
                              //   }

  selections: {},             // Per-topicmap selection:
                              //   {
                              //     topicmapId: Selection
                              //   }

  topicmapTypes: {}           // Registered topicmap types:
                              //   {
                              //     topicmapTypeUri: {
                              //       uri:
                              //       name:
                              //       renderer:
                              //     }
                              //   }
}

const actions = {

  createTopicmap ({rootState, dispatch}, {name, topicmapTypeUri, isPrivate}) {
    name            = name            || 'untitled'
    topicmapTypeUri = topicmapTypeUri || 'dmx.topicmaps.topicmap'
    isPrivate       = isPrivate       || false
    //
    console.log('Creating topicmap', name)
    dm5.restClient.createTopicmap(name, topicmapTypeUri, isPrivate).then(topic => {
      console.log('Topicmap topic', topic)
      // update state
      topicmapTopics(rootState).push(topic)
      initSelection(topic.id, dispatch)
      //
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
  displayTopicmap ({getters, rootState, dispatch}, id) {
    // console.log('displayTopicmap', id)
    // update state
    Vue.set(state.selectedTopicmapId, _workspaceId(rootState), id)    // Vue.set() recalculates "topicmapId" getter
    dm5.utils.setCookie('dmx_topicmap_id', id)
    // update state + update view
    return _displayTopicmap(getters, rootState, dispatch)
  },

  /**
   * Calls the "topicmap" route with the given ID.
   * If a topic/assoc selection is known for that topicmap, the "topic" or "assoc" route is called instead.
   *
   * Preconditions:
   * - the route is *not* yet set.
   *
   * Note: the topicmap is *not* required to belong to the selected workspace.
   * This allows for cross-workspace browser history navigation.
   */
  selectTopicmap ({dispatch}, id) {
    _selectTopicmap(id, dispatch)
  },

  /**
   * Preconditions:
   * - the route is *not* yet set.
   */
  selectTopic ({getters}, id) {
    // console.log('selectTopic', id)
    getters.selection.addTopic(id)
    // console.log(getters.selection.topicIds, getters.selection.assocIds)
  },

  /**
   * Preconditions:
   * - the route is *not* yet set.
   */
  selectAssoc ({getters}, id) {
    // console.log('selectAssoc', id)
    getters.selection.addAssoc(id)
    // console.log(getters.selection.topicIds, getters.selection.assocIds)
  },

  /**
   * Preconditions:
   * - the route is *not* yet set.
   */
  unselectTopic ({getters}, id) {
    // console.log('unselectTopic', id)
    getters.selection.removeTopic(id)
    // console.log(getters.selection.topicIds, getters.selection.assocIds)
  },

  /**
   * Preconditions:
   * - the route is *not* yet set.
   */
  unselectAssoc ({getters}, id) {
    // console.log('unselectAssoc', id)
    getters.selection.removeAssoc(id)
    // console.log(getters.selection.topicIds, getters.selection.assocIds)
  },

  unselectIf ({getters}, id) {
    // console.log('unselectIf', id)
    getters.selection.remove(id)
  },

  // Note: by design multi-selections behave different than single selections:
  // - multi selections are not represented in the browser URL.
  // - the object details of a multi selection are *not* displayed in-map (unless pinned).

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
  setTopicSelection ({getters, rootState, dispatch}, {id, p}) {
    // console.log('setTopicSelection', _topicmapId(getters), id, getters.selection.topicIds)
    // update view          // Note: view must be updated before state is updated
    dispatch('renderAsSelected', {id, p, showDetails: showDetails(rootState)})
    _syncUnselectMulti(getters.selection, dispatch)
    // update state
    getters.selection.setTopic(id)
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
  setAssocSelection ({getters, rootState, dispatch}, {id, p}) {
    // console.log('setAssocSelection', _topicmapId(getters), id)
    // update view          // Note: view must be updated before state is updated
    dispatch('renderAsSelected', {id, p, showDetails: showDetails(rootState)})
    _syncUnselectMulti(getters.selection, dispatch)
    // update state
    getters.selection.setAssoc(id)
  },

  /**
   * Removes the selection in the topicmap panel.
   *
   * Preconditions:
   * - the route is set.
   * - the "topicmap" state is up-to-date.
   * - the topicmap rendering is complete.
   *
   * Postcondition:
   * - "selections" state is up-to-date.
   *
   * @param   id    id of the topic/assoc to unselect
   */
  unsetSelection ({getters, dispatch}, id) {
    const selection = getters.selection
    // console.log('unsetSelection', id, selection.topicIds, selection.assocIds)
    if (typeof id !== 'number') {
      throw Error(`id is expected to be a number, got ${typeof id} (${id})`)
    }
    dispatch('renderAsUnselected')          // update view
    if (!selection) {
      // This can happen while workspace deletion. The workspace topic is removed from the topicmap, causing
      // unsetSelection(). The topicmap might be deleted aleady in the course of deleting the workspace content.
      // The requested topicmap update (unsetSelection()) is now obsolete. TODO: rethink this in-depth.
      // console.warn(`Can't unselect topic/assoc ${id}; topicmap already gone`)
      return
    }
    if (selection.isSingle()) {
      // If there is a single selection and history navigation leads to a selection-less route, the "selection" state
      // must be emptied manually. In contrast when removing the selection by topicmap interaction the "selection" state
      // is up-to-date already.
      selection.empty()                     // update state
    } else if (selection.isMulti()) {
      // If a single selection is extended to a multi selection the URL's selection part is stripped, causing the router
      // to remove the single selection from state and view. The former single selection must be visually restored in
      // order to match the multi selection state. The low-level '_renderAsSelected' action manipulates the view only.
      // The normal 'renderAsSelected' action would display the in-map details.
      dispatch('_renderAsSelected', id)     // update view
    }
  },

  revealTopicById ({dispatch}, topicId) {
    dm5.restClient.getTopic(topicId).then(topic => {
      dispatch('revealTopic', {topic})
    })
  },

  /**
   * Reveals a topic on the topicmap panel.
   *
   * @param   topic     the topic to reveal (dm5.Topic).
   * @param   pos       Optional: the topic position in model coordinates (object with "x", "y" props).
   *                    If not given it's up to the topicmap renderer to position the topic.
   * @param   noSelect  Optional: if trueish the programmatic topic selection is suppressed.
   */
  revealTopic ({dispatch}, {topic, pos, noSelect}) {
    dispatch('renderTopic', {topic, pos})                     // dispatch into topicmap renderer
    !noSelect && dispatch('callTopicRoute', topic.id)         // dispatch into app
  },

  revealAssoc ({dispatch}, {assoc, noSelect}) {
    dispatch('renderAssoc', assoc)                            // dispatch into topicmap renderer
    !noSelect && dispatch('callAssocRoute', assoc.id)         // dispatch into app
  },

  revealRelatedTopic ({dispatch}, {relTopic, noSelect}) {
    dispatch('renderRelatedTopic', relTopic)                  // dispatch into topicmap renderer
    !noSelect && dispatch('callTopicRoute', relTopic.id)      // dispatch into app
  },

  createAssoc ({dispatch}, {playerId1, playerId2}) {
    // TODO: display search/create widget; initiate assoc creation there
    const assocModel = {
      typeUri: 'dmx.core.association',
      role1: {roleTypeUri: 'dmx.core.default', ...playerId1},
      role2: {roleTypeUri: 'dmx.core.default', ...playerId2}
    }
    console.log('createAssoc', assocModel)
    dm5.restClient.createAssoc(assocModel).then(assoc => {
      console.log('Created', assoc)
      dispatch('revealAssoc', {assoc})
      dispatch('_processDirectives', assoc.directives)
    })
  },

  _hideTopic ({dispatch}, id) {
    // update state
    unselectIfCascade(id, dispatch)
  },

  _hideAssoc ({dispatch}, id) {
    // update state
    unselectIfCascade(id, dispatch)
  },

  _deleteTopic ({rootState, dispatch}, id) {
    const topic = state.topicmap.getTopic(id)
    if (topic.typeUri !== 'dmx.topicmaps.topicmap') {
      return
    }
    if (state.topicmap.id !== id) {
      throw Error(`topicmap ${id} can't be deleted as it is not selected`)
    }
    console.log('_deleteTopic', id)
    const topicmapTopic = topicmapTopics(rootState).filter(topic => topic.id !== id)[0]
    if (topicmapTopic) {
      // Note: selecting a new topicmap is not strictly required. It would be selected while directives processing
      // anyways. But we do it here (before the delete request is actually sent) for quick user feedback.
      _selectTopicmap(topicmapTopic.id, dispatch)
    } else {
      // FIXME: synchronization. Create topicmap *before* processing response of delete request
      dispatch('createTopicmap', {})
    }
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
    _selectTopicmap(topicmapId, dispatch)
  },

  reloadTopicmap ({getters, rootState, dispatch}) {
    console.log('reloadTopicmap', _topicmapId(getters))
    dispatch('clearTopicmapCache')
    _displayTopicmap(getters, rootState, dispatch).then(() => {
      // update view
      const selection = getters.selection
      if (selection.isSingle()) {
        dispatch('renderAsSelected', {
          id: selection.getObjectId(),
          showDetails: showDetails(rootState)
        })
      } else {
        // Note: a multi selection is visually restored by _displayTopicmap() already
      }
    })
  },

  /**
   * Fetches the topicmap topics for the selected workspace.
   * Updates the "topicmapTopics" and "selections" states.
   * If the topicmap topics for the selected workspace are fetched already nothing is performed.
   * (In this case the returned promise is already resolved.)
   *
   * Precondition:
   * - the workspace is selected ("workspaceId" state is up-to-date, see workspaces module).
   *
   * @return  a promise resolved once the "topicmapTopics" state is up-to-date.
   */
  fetchTopicmapTopics ({rootState, dispatch}) {
    const workspaceId = _workspaceId(rootState)
    let p
    if (state.topicmapTopics[workspaceId]) {
      p = Promise.resolve()
    } else {
      // console.log('fetchTopicmapTopics', workspaceId)
      p = dm5.restClient.getAssignedTopics(workspaceId, 'dmx.topicmaps.topicmap', true).then(topics => {
        // console.log('### Topicmap topics ready!', topics.length)                  // includeChilds=true
        if (!topics.length) {
          throw Error(`workspace ${workspaceId} has no topicmap`)
        }
        Vue.set(state.topicmapTopics, workspaceId, topics)
        topics.forEach(topic => {
          initSelection(topic.id, dispatch)
        })
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

  _newTopicmap ({dispatch}, {topicmapTopic}) {
    const _topicmapTopic = new dm5.Topic(topicmapTopic)
    const workspace = _topicmapTopic.childs['dmx.workspaces.workspace#dmx.workspaces.workspace_assignment']
    // Note: the default topicmap created for new workspaces have no workspace assignment yet
    if (!workspace) {
      console.warn('No workspace found in topicmap', _topicmapTopic)
      return
    }
    const topics = state.topicmapTopics[workspace.id]
    if (topics) {
      console.log('Adding topicmap topic', _topicmapTopic, 'to workspace', workspace.id)
      topics.push(_topicmapTopic)
      initSelection(_topicmapTopic.id, dispatch)
    } else {
      console.log('Ignoring topicmap topic', _topicmapTopic, 'for workspace', workspace.id)
    }
  },

  _setTopicVisibility ({getters, dispatch}, {topicmapId, topicId, visibility}) {
    // console.log('_setTopicVisibility (Topicmaps Module)', topicmapId, topicId, visibility)
    if (topicmapId === _topicmapId(getters)) {
      if (!visibility) {
        unselectIfCascade(topicId, dispatch)      // update state
      }
    }
  },

  _setAssocVisibility ({getters, dispatch}, {topicmapId, assocId, visibility}) {
    // console.log('_setAssocVisibility (Topicmaps Module)', topicmapId, assocId, visibility)
    if (topicmapId === _topicmapId(getters)) {
      if (!visibility) {
        unselectIfCascade(assocId, dispatch)      // update state
      }
    }
  },

  _processDirectives ({getters, rootState, dispatch}, directives) {
    // console.log(`Topicmaps: processing ${directives.length} directives`)
    directives.forEach(dir => {
      let topic
      switch (dir.type) {
      case "UPDATE_TOPIC":
        topic = new dm5.Topic(dir.arg)
        if (topic.typeUri === 'dmx.topicmaps.topicmap') {
          updateTopicmap(topic)
        }
        break
      case "DELETE_TOPIC":
        topic = new dm5.Topic(dir.arg)
        if (topic.typeUri === 'dmx.topicmaps.topicmap') {
          deleteTopicmap(topic, getters, rootState, dispatch)
        }
        break
      }
    })
  }
}

const getters = {

  /**
   * ID of the selected topicmap. Its calculation is based on "workspaceId" state (see workspaces module) and
   * "selectedTopicmapId" state.
   * Undefined if no workspace and/or no topicmap is selected. Note: at the moment the webclient components are
   * instantiated no workspace and no topicmap is selected
   */
  topicmapId: (state, getters, rootState) => {
    const workspaceId = __workspaceId(rootState)
    const topicmapId = workspaceId && state.selectedTopicmapId[workspaceId]
    // console.log('# topicmapId getter', workspaceId, topicmapId)
    return topicmapId
  },

  selection: (state, getters) => {
    const topicmapId = getters.topicmapId     // FIXME: undefined?
    // console.log('# selection getter', topicmapId, state.selections[topicmapId])
    return state.selections[topicmapId]
  }
}

export default {
  state,
  actions,
  getters
}

// Actions

function _selectTopicmap (id, dispatch) {
  const selection = state.selections[id]
  console.log('_selectTopicmap', id)
  // Note: for cross-workspace jumps the workspace's map topics might not yet be loaded and no selection object
  // available. In that case we call the topicmap route. It will load the map topics and init the selection objects.
  if (selection && selection.isSingle()) {
    const type = selection.getType()
    dispatch('callRoute', {
      name: type,
      params: {
        topicmapId: id,
        [`${type}Id`]: selection.getObjectId()
      }
    })
  } else {
    dispatch('callTopicmapRoute', id)
  }
}

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
function _displayTopicmap (getters, rootState, dispatch) {
  const topicmapTopic = getTopicmapTopic(rootState)
  const selection = getters.selection
  return topicmapTopic.isWritable()
    .then(writable => dispatch('showTopicmap', {topicmapTopic, writable, selection}))
    .then(topicmap => {
      state.topicmap = topicmap
      _syncSelectMulti(selection, dispatch)
    })
}

function _syncSelectMulti (selection, dispatch) {
  // console.log('_syncSelectMulti', selection.topicIds, selection.assocIds)
  if (selection.isMulti()) {
    selection.forEachId(id => {
      dispatch('_renderAsSelected', id)
    })
  }
}

function _syncUnselectMulti (selection, dispatch) {
  // console.log('_syncUnselectMulti', selection.topicIds, selection.assocIds)
  // If there is a multi selection and history navigation leads to a single-selection route, the multi selection must be
  // visually removed. In contrast when changing the selection by topicmap interaction the view is up-to-date already.
  if (selection.isMulti()) {
    selection.forEachId(id => {
      dispatch('_renderAsUnselected', id)     // TODO: pinned multi selection?
    })
  }
}

function initSelection (id, dispatch) {
  if (state.selections[id]) {
    throw Error(`'selections' state for topicmap ${id} already initialized`)
  }
  state.selections[id] = new Selection(selectionHandler(dispatch))
}

function selectionHandler (dispatch) {
  return selection => {
    // console.log('handleSelection', selection.topicIds, selection.assocIds)
    if (selection.isSingle()) {
      dispatch(
        selection.getType() === 'topic' ? 'callTopicRoute' : 'callAssocRoute',
        selection.getObjectId()
      )
    } else {
      dispatch('stripSelectionFromRoute')
    }
  }
}

// ---

function unselectIfCascade(id, dispatch) {
  // console.log('unselectIfCascade', id)
  dispatch('unselectIf', id)
  state.topicmap.getAssocsWithPlayer(id).forEach(assoc => {
    unselectIfCascade(assoc.id, dispatch)      // recursion
  })
}

// Process directives

/**
 * Processes an UPDATE_TOPIC directive.
 * Updates the topicmap menu when a topicmap is renamed.
 */
function updateTopicmap (topic) {
  // console.log('updateTopicmap', topic)
  // update state
  findTopicmapTopic(topic.id, (topics, i) => Vue.set(topics, i, topic))
}

/**
 * Processes a DELETE_TOPIC directive.
 */
function deleteTopicmap (topic, getters, rootState, dispatch) {
  // update state
  findTopicmapTopic(topic.id, (topics, i) => topics.splice(i, 1))
  delete state.selections[topic.id]
  // redirect
  console.log('deleteTopicmap', topic.id, _topicmapId(getters))
  if (topic.id === _topicmapId(getters)) {
    _selectTopicmap(firstTopicmapTopic(rootState).id, dispatch)
  }
}

// State helper

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
    throw Error(`topicmap topics of workspace ${workspaceId} not yet loaded`)
  }
  const topicmapTopic = topicmapTopics.find(topic => topic.id === topicmapId)
  if (!topicmapTopic) {
    throw Error(`topicmap topic ${topicmapId} not found (workspace ${workspaceId})`)
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
function _topicmapId (getters) {
  if (!getters.topicmapId) {
    throw Error('no selected topicmap known')
  }
  return getters.topicmapId
}

function firstTopicmapTopic (rootState) {
  const topic = topicmapTopics(rootState)[0]
  if (!topic) {
    throw Error(`workspace ${__workspaceId(rootState)} has no topicmap`)
  }
  return topic
}

function topicmapTopics (rootState) {
  return state.topicmapTopics[_workspaceId(rootState)]
}

function _workspaceId (rootState) {
  const workspaceId = __workspaceId(rootState)
  if (!workspaceId) {
    throw Error(`no selected workspace known`)
  }
  return workspaceId
}

function __workspaceId (rootState) {
  return rootState.workspaces.workspaceId
}

function showDetails (rootState) {
  return !rootState.details.visible
}
