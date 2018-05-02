import Vue from 'vue'
import dm5 from 'dm5'
import Selection from './selection'

const state = {

  topicmapTopics: {},         // Loaded topicmap topics (including childs), grouped by workspace ID:
                              //   {
                              //     workspaceId: [topicmapTopic]    # array of dm5.Topic
                              //   }
                              // TODO: make the array a map, key by topicmap ID?

  selectedTopicmapId: {},     // Per-workspace selected topicmap:
                              //   {
                              //     workspaceId: topicmapId
                              //   }

  selections: {},             // Per-topicmap selection, keyed by topicmap ID:
                              //   {
                              //     topicmapId: Selection
                              //   }

  topicmapTypes: {}           // Registered topicmap types, keyed by topicmap type URI:
                              //   {
                              //     topicmapTypeUri: {
                              //       uri:
                              //       name:
                              //       renderer:
                              //       mounted: callback
                              //     }
                              //   {
}

const actions = {

  createTopicmap ({rootState, dispatch}, {name, topicmapTypeUri, isPrivate}) {
    console.log('Creating topicmap', name)
    dm5.restClient.createTopicmap(name, topicmapTypeUri, isPrivate).then(topic => {
      console.log('Topicmap topic', topic)
      // update state
      state.topicmapTopics[_workspaceId(rootState)].push(topic)
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
    dm5.utils.setCookie('dm4_topicmap_id', id)
    // update state + sync view
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
    const selection = state.selections[id]
    // console.log('selectTopicmap', id, selection)
    if (selection.isSingle()) {
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
  },

  /**
   * Preconditions:
   * - the route is *not* yet set.
   */
  selectTopic ({getters}, id) {
    console.log('selectTopic', id)
    getters.selection.addTopic(id)
  },

  /**
   * Preconditions:
   * - the route is *not* yet set.
   */
  selectAssoc ({getters}, id) {
    console.log('selectAssoc', id)
    getters.selection.addAssoc(id)
  },

  /**
   * Preconditions:
   * - the route is *not* yet set.
   */
  unselectTopic ({getters}, id) {
    console.log('unselectTopic', id)
    getters.selection.removeTopic(id)
  },

  /**
   * Preconditions:
   * - the route is *not* yet set.
   */
  unselectAssoc ({getters}, id) {
    console.log('unselectAssoc', id)
    getters.selection.removeAssoc(id)
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
  setTopicSelection ({getters, dispatch}, {id, p}) {
    // console.log('setTopicSelection', _topicmapId(getters), id)
    // sync view          // Note: view must be synced before state is updated
    dispatch('syncSelect', {id, p})
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
  setAssocSelection ({getters, dispatch}, {id, p}) {
    // console.log('setAssocSelection', _topicmapId(getters), id)
    // sync view          // Note: view must be synced before state is updated
    dispatch('syncSelect', {id, p})
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
    console.log('unsetSelection', id, selection.topicIds, selection.assocIds)
    if (typeof id !== 'number') {
      throw Error(`id is expected to be a number, got ${typeof id} (${id})`)
    }
    dispatch('syncUnselect')          // sync view
    if (selection.isSingle()) {
      // If there is a single selection and history navigation leads to a selection-less route, the "selection" state
      // must be emptied manually. In contrast when removing the selection by topicmap interaction the "selection" state
      // is up-to-date already.
      selection.empty()               // update state
    } else if (selection.isMulti()) {
      // If a single selection is extended to a multi selection the URL's selection part is stripped, causing the router
      // to remove the single selection from state and view. The former single selection must be visually restored in
      // order to match the multi selection state. The low-level '_syncSelect' action manipulates the view only. The
      // normal 'syncSelect' action would display the in-map details.
      dispatch('_syncSelect', id)     // sync view
    }
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

  reloadTopicmap ({getters, rootState, dispatch}) {
    console.log('reloadTopicmap', _topicmapId(getters))
    dispatch('clearTopicmapCache')
    _displayTopicmap(getters, rootState, dispatch).then(() => {
      // sync view
      const selection = getters.selection
      if (selection.isSingle()) {
        dispatch('syncSelect', {
          id: selection.getObjectId(),
          p: Promise.resolve()
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
      p = dm5.restClient.getAssignedTopics(workspaceId, 'dm4.topicmaps.topicmap', true).then(topics => {
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
    const workspace = _topicmapTopic.getChildTopic('dm4.workspaces.workspace')
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
    if (topicmapId === _topicmapId(getters)) {
      // update state
      if (!visibility) {
        dispatch('unselectIf', topicId)
      }
    }
  },

  _removeAssocFromTopicmap ({getters, dispatch}, {topicmapId, assocId}) {
    if (topicmapId === _topicmapId(getters)) {
      // update state
      dispatch('unselectIf', assocId)
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
  return topicmapTopic.isWritable()
    .then(writable => dispatch('showTopicmap', {topicmapTopic, writable}))
    .then(() => _syncSelectMulti(getters.selection, dispatch))
}

function _syncSelectMulti (selection, dispatch) {
  console.log('_syncSelectMulti', selection.topicIds, selection.assocIds)
  if (selection.isMulti()) {
    selection.forEachId(id => {
      dispatch('_syncSelect', id)
    })
  }
}

function _syncUnselectMulti (selection, dispatch) {
  console.log('_syncUnselectMulti', selection.topicIds, selection.assocIds)
  // If there is a multi selection and history navigation leads to a single-selection route, the multi selection must be
  // visually removed. In contrast when changing the selection by topicmap interaction the view is up-to-date already.
  if (selection.isMulti()) {
    selection.forEachId(id => {
      dispatch('_syncUnselect', id)     // TODO: pinned multi selection?
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
    console.log('handleSelection', selection.topicIds, selection.assocIds)
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
