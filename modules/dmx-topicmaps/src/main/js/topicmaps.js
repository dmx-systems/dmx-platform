import dmx from 'dmx-api'
import app from 'modules/dmx-webclient/src/main/js/app'
import Selection from './selection'

const state = {

  topicmap: undefined,        // The rendered topicmap (dmx.Topicmap).
                              // Set by _displayTopicmap()
                              // TODO: undefined for non-standard topicmaps, e.g. Geomap

  topicmapWritable: false,    // If the current topicmap is writable (Boolean)

  topicmapTopics: {},         // Per-workspace loaded topicmap topics (including children):
                              //   {
                              //     workspaceId: [topicmapTopic]    # array of dmx.Topic
                              //   }

  selectedTopicmapId: {},     // Per-workspace selected topicmap:
                              //   {
                              //     workspaceId: topicmapId
                              //   }

  selections: {},             // Per-topicmap selection:
                              //   {
                              //     topicmapId: Selection
                              //   }
                              // A Selection instance exists for every loaded topicmap topic.
                              // To get the Selection instance of the selected topicmap use the "selection" getter.

  topicmapTypes: {},          // Registered topicmap types:
                              //   {
                              //     topicmapTypeUri: {
                              //       uri:
                              //       name:
                              //       renderer:
                              //       hidden:
                              //     }
                              //   }

  topicmapCommands: {}        // Registered topicmap commands:
                              //   {
                              //      topicmapTypeUri: [comp]
                              //   }
}

const actions = {

  createTopicmap ({rootState, dispatch}, {
                                           name = 'untitled',
                                           topicmapTypeUri = 'dmx.topicmaps.topicmap',
                                           viewProps = {}
                                         }) {                                                 /* eslint indent: "off" */
    // console.log('Creating topicmap', name, topicmapTypeUri, viewProps)
    dmx.rpc.createTopicmap(name, topicmapTypeUri, viewProps).then(topic => {
      // console.log('Topicmap topic', topic)
      // update state
      topicmapTopics(rootState).push(topic)
      initSelection(topic.id, dispatch)
      //
      dispatch('callTopicmapRoute', topic.id)
    })
  },

  /**
   * Sets the topicmap state ("selectedTopicmapId" and cookie), and displays the given topicmap.
   * The topicmap is retrieved either from cache or from server.
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
   * Note: this state is *not* yet up-to-date:
   * - "topicmap" (updated only once topicmap retrieval is complete)
   *
   * @returns   a promise resolved once topicmap rendering is complete.
   *            At this time the "topicmap" state is up-to-date.
   */
  displayTopicmap ({getters, rootState, dispatch}, id) {
    // console.log('displayTopicmap', id)
    // update state
    state.selectedTopicmapId[_workspaceId(rootState)] = id
    dmx.utils.setCookie('dmx_topicmap_id', id)
    // update state + update view
    return _displayTopicmap(getters, dispatch).then(() => {
      _syncSelectMulti(getters.selection, dispatch)
    })
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
   *
   * Note: this action handles *interactive* selection only.
   * For *programmatic* selection dispatch `callTopicRoute` instead.
   */
  selectTopic ({getters}, id) {
    // console.log('selectTopic', id)
    getters.selection.addTopic(id)
    // console.log(getters.selection.topicIds, getters.selection.assocIds)
  },

  /**
   * Preconditions:
   * - the route is *not* yet set.
   *
   * Note: this action handles *interactive* selection only.
   * For *programmatic* selection dispatch `callAssocRoute` instead.
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
    // console.log('setTopicSelection', _topicmapId(getters), id, getters.selection.topicIds)
    // update view          // Note: view must be updated before state is updated
    dispatch('renderAsSelected', {id, p, showDetails: getters.showInmapDetails})
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
    // update view          // Note: view must be updated before state is updated
    dispatch('renderAsSelected', {id, p, showDetails: getters.showInmapDetails})
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
   * @param   id            id of the topic/assoc to unselect
   * @param   noViewUpdate  if true the actual renderer's view will not be updated, only its model. This might be needed
   *                        when unset-selection is triggered programmatically in the course of an hide/delete
   *                        operation. In this case the renderer's view might not be able to update anymore as certain
   *                        on-screen parts (e.g. the Cytoscape renderer's Topic DOM) are disposed of already. The view
   *                        update can be safely omitted as the object is about to disappear anyways.
   */
  unsetSelection ({getters, dispatch}, {id, noViewUpdate}) {
    const selection = getters.selection
    // console.log('unsetSelection', id, noViewUpdate, selection.topicIds, selection.assocIds)
    if (typeof id !== 'number') {
      throw Error(`id is expected to be a number, got ${typeof id} (${id})`)
    }
    dispatch('renderAsUnselected', noViewUpdate)  // update view
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
      selection.empty()                           // update state
    } else if (selection.isMulti()) {
      // If a single selection is extended to a multi selection the URL's selection part is stripped, causing the router
      // to remove the single selection from state and view. The former single selection must be visually restored in
      // order to match the multi selection state. The low-level '_renderAsSelected' action manipulates the view only.
      // The normal 'renderAsSelected' action would display the in-map details.
      dispatch('_renderAsSelected', id)           // update view
    }
  },

  /**
   * @param   topicId   the ID (number), or an object with `id`, `pos`, `noSelect` properties.
   */
  revealTopicById ({dispatch}, topicId) {
    let id, pos, noSelect
    if (typeof topicId === 'object') {
      ({id, pos, noSelect} = topicId)
    } else {
      id = topicId
    }
    return dmx.rpc.getTopic(id).then(topic => {
      dispatch('revealTopic', {topic, pos, noSelect})
    })
  },

  /**
   * Reveals a topic on the topicmap panel.
   *
   * @param   topic     the topic to reveal (dmx.Topic).
   * @param   pos       Optional: the topic position in model coordinates (object with "x", "y" props).
   *                    If not given it's up to the topicmap renderer to position the topic.
   * @param   noSelect  Optional: if trueish the programmatic topic selection is suppressed.
   */
  revealTopic ({dispatch}, {topic, pos, noSelect}) {
    // Note: in case selection is requested (noSelect=falsish) auto-panning is performed through route change
    // (see "renderAsSelected" action in topicmap-model, dmx-cytoscape-renderer module)
    dispatch('renderTopic', {topic, pos, autoPan: noSelect})            // dispatch into topicmap renderer
    !noSelect && dispatch('callTopicRoute', topic.id)                   // dispatch into app
  },

  /**
   * Reveals an assoc on the topicmap panel.
   *
   * Prerequisite: both players are revealed already.
   */
  revealAssoc ({dispatch}, {assoc, noSelect}) {
    dispatch('renderAssoc', assoc)                                      // dispatch into topicmap renderer
    !noSelect && dispatch('callAssocRoute', assoc.id)                   // dispatch into app
  },

  revealRelatedTopic ({getters, dispatch}, {relTopic, pos, noSelect}) {
    // Note: in case selection is requested (noSelect=falsish) auto-panning is performed through route change
    // (see "renderAsSelected" action in topicmap-model, dmx-cytoscape-renderer module)
    dispatch('renderRelatedTopic', {relTopic, pos, autoPan: noSelect})  // dispatch into topicmap renderer
    !noSelect && dispatch('callTopicRoute', relTopic.id)                // dispatch into app
  },

  createAssoc ({dispatch}, {playerId1, playerId2}) {
    // TODO: display search/create widget; initiate assoc creation there
    const assocModel = {
      typeUri: 'dmx.core.association',
      player1: {roleTypeUri: 'dmx.core.default', ...playerId1},
      player2: {roleTypeUri: 'dmx.core.default', ...playerId2}
    }
    dmx.rpc.createAssoc(assocModel).then(assoc => {
      // console.log('Created', assoc)
      dispatch('revealAssoc', {assoc})
      dispatch('_processDirectives', assoc.directives)
    })
  },

  // TODO: adapt 2 actions

  hideMulti ({getters, rootState, dispatch}, idLists) {
    // update state + view (for immediate visual feedback)
    idLists.topicIds.forEach(id => hideTopic(id, getters, rootState, dispatch))
    idLists.assocIds.forEach(id => hideAssoc(id, getters, rootState, dispatch))
    // update server
    if (state.topicmapWritable) {
      dmx.rpc.hideMulti(state.topicmap.id, idLists)
    }
  },

  deleteMulti ({getters, rootState, dispatch}, idLists) {
    confirmDeletion(idLists).then(() => {
      // console.log('deleteMulti', idLists.topicIds, idLists.assocIds)
      // update client state + sync view (for immediate visual feedback)
      idLists.topicIds.forEach(id => deleteTopic(id, getters, rootState, dispatch))
      idLists.assocIds.forEach(id => deleteAssoc(id, dispatch))
      // update server state
      dmx.rpc.deleteMulti(idLists).then(response => {
        dispatch('_processDirectives', response.directives)
      })
    }).catch(() => {})    // suppress unhandled rejection on cancel
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

  reloadTopicmap ({getters, dispatch}) {
    // 1) update state
    dispatch('clearTopicmapCache')
    // Note: when the topicmap cache is cleared (see dmx-topicmap-panel module) here we remove all topicmap selection
    // states as we can't know if the selected topic/assoc is still contained in the topicmap when loaded with changed
    // authorization.
    emptyAllSelectionsExcept(_topicmapId(getters))
    // 2) update view
    _displayTopicmap(getters, dispatch).then(() => adaptSelection(getters, dispatch))
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
      p = dmx.rpc.getAssignedTopics(workspaceId, 'dmx.topicmaps.topicmap', true).then(topics => {
        // console.log('### Topicmap topics ready!', topics.length)        // includeChildren=true
        if (!topics.length) {
          throw Error(`workspace ${workspaceId} has no topicmap`)
        }
        state.topicmapTopics[workspaceId] = topics
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

  registerTopicmapCommand (_, command) {
    const c = state.topicmapCommands
    const uri = command.topicmapTypeUri
    const commands = c[uri] || (c[uri] = [])
    commands.push(command.comp)
  },

  //

  loggedIn ({dispatch}) {
    dispatch('reloadTopicmap')
  },

  // Note: loggedOut is handled by workspaces module

  // WebSocket messages

  _newTopicmap ({dispatch}, {topicmapTopic}) {
    const _topicmapTopic = new dmx.Topic(topicmapTopic)
    const workspace = _topicmapTopic.children['dmx.workspaces.workspace#dmx.workspaces.workspace_assignment']
    // Note: the default topicmap created for new workspaces have no workspace assignment yet
    if (!workspace) {
      console.warn('No workspace found in topicmap', _topicmapTopic)
      return
    }
    const topics = state.topicmapTopics[workspace.id]
    if (topics) {
      // console.log('Adding topicmap topic', _topicmapTopic, 'to workspace', workspace.id)
      topics.push(_topicmapTopic)
      initSelection(_topicmapTopic.id, dispatch)
    } else {
      // console.log('Ignoring topicmap topic', _topicmapTopic, 'for workspace', workspace.id)
    }
  },

  _setTopicVisibility ({getters, rootState, dispatch}, {topicmapId, topicId, visibility}) {
    // console.log('_setTopicVisibility (Topicmaps Module)', topicmapId, topicId, visibility)
    if (topicmapId === _topicmapId(getters)) {
      if (!visibility) {
        unselectIfCascade(topicId, getters, rootState, dispatch)      // update state
      }
    }
  },

  _setAssocVisibility ({getters, rootState, dispatch}, {topicmapId, assocId, visibility}) {
    // console.log('_setAssocVisibility (Topicmaps Module)', topicmapId, assocId, visibility)
    if (topicmapId === _topicmapId(getters)) {
      if (!visibility) {
        unselectIfCascade(assocId, getters, rootState, dispatch)      // update state
      }
    }
  },

  _processDirectives ({getters, rootState, dispatch}, directives) {
    // console.log(`Topicmaps: processing ${directives.length} directives`)
    directives.forEach(dir => {
      let topic
      switch (dir.type) {
      case 'UPDATE_TOPIC':
        topic = new dmx.Topic(dir.arg)
        if (topic.typeUri === 'dmx.topicmaps.topicmap') {
          updateTopicmap(topic)
        }
        break
      case 'DELETE_TOPIC':
        unselectIf(dir.arg.id, getters, rootState, dispatch)
        topic = new dmx.Topic(dir.arg)
        if (topic.typeUri === 'dmx.topicmaps.topicmap') {
          deleteTopicmap(topic, getters, rootState, dispatch)
        }
        //
        removeFromAllSelections(dir.arg.id)
        break
      case 'DELETE_ASSOC':
        unselectIf(dir.arg.id, getters, rootState, dispatch)
        removeFromAllSelections(dir.arg.id)
        break
      }
    })
  }
}

const getters = {

  /**
   * ID of the selected topicmap.
   *
   * Calculation is based on "workspaceId" state (see workspaces module) and "selectedTopicmapId" state.
   *
   * Undefined if no workspace and/or no topicmap is selected.
   * Note: at the moment the webclient components are instantiated no workspace and no topicmap is selected.
   */
  topicmapId: (state, getters, rootState) => {
    const workspaceId = __workspaceId(rootState)
    const topicmapId = workspaceId && state.selectedTopicmapId[workspaceId]
    return topicmapId
  },

  /**
   * Topicmap topic of the selected topicmap; undefined if no topicmap is selected.
   */
  topicmapTopic (state, getters, rootState) {
    const topicmapId = getters.topicmapId
    if (!topicmapId) {
      return
    }
    const workspaceId = _workspaceId(rootState)
    const topicmapTopics = state.topicmapTopics[workspaceId]
    if (!topicmapTopics) {
      throw Error(`topicmap topics of workspace ${workspaceId} not yet loaded`)
    }
    const topicmapTopic = topicmapTopics.find(topic => topic.id === topicmapId)
    if (!topicmapTopic) {
      throw Error(`topicmap topic ${topicmapId} not found (workspace ${workspaceId})`)
    }
    return topicmapTopic
  },

  topicmapTypeUri (state, getters) {
    return getters.topicmapTopic?.children['dmx.topicmaps.topicmap_type_uri'].value
  },

  /**
   * Selection instance of the selected topicmap.
   */
  selection: (state, getters) => {
    const topicmapId = getters.topicmapId     // FIXME: undefined?
    return state.selections[topicmapId]
  },

  visibleTopicIds (state) {
    // Note: at startup or at renderer switch state.topicmap is undefined
    return state.topicmap?.topics.filter(topic => topic.isVisible()).map(topic => topic.id)
  },

  visibleAssocIds (state) {
    // Note: at startup or at renderer switch state.topicmap is undefined
    return state.topicmap?.assocs.filter(assoc => assoc.isVisible()).map(assoc => assoc.id)
  }
}

export default {
  state,
  actions,
  getters
}

// Process directives

/**
 * Processes an UPDATE_TOPIC directive.
 * Updates the topicmap menu when a topicmap is renamed.
 */
function updateTopicmap (topic) {
  // console.log('updateTopicmap', topic)
  // update state
  findTopicmapTopic(topic.id, (topics, i) => topics[i] = topic)
}

/**
 * Processes a DELETE_TOPIC directive.
 */
function deleteTopicmap (topic, getters, rootState, dispatch) {
  // update state
  findTopicmapTopic(topic.id, (topics, i) => topics.splice(i, 1))
  delete state.selections[topic.id]
  // redirect
  // console.log('deleteTopicmap', topic.id, _topicmapId(getters))
  if (topic.id === _topicmapId(getters)) {
    _selectTopicmap(firstTopicmapTopic(rootState).id, dispatch)
  }
}

// Actions Helper

function _selectTopicmap (id, dispatch) {
  const selection = state.selections[id]
  // console.log('_selectTopicmap', id)
  // Note: for cross-workspace jumps the workspace's map topics might not yet be loaded and no selection object
  // available. In that case we call the topicmap route. It will load the map topics and init the selection objects.
  if (selection?.isSingle()) {
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
 *            At this time the "topicmap" state is up-to-date as well.
 */
function _displayTopicmap (getters, dispatch) {
  const topicmapTopic = getters.topicmapTopic
  const selection = getters.selection
  return topicmapTopic.isWritable()
    .then(writable => {
      state.topicmapWritable = writable
      return dispatch('showTopicmap', {topicmapTopic, writable, selection})    // dispatch into topicmap-panel
    })
    .then(topicmap => {
      state.topicmap = topicmap
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
      dispatch('_renderAsUnselected', id)
    })
  }
}

function confirmDeletion (idLists) {
  const _size = size(idLists)
  if (!_size) {
    throw Error('confirmDeletion() called with empty idLists')
  }
  let message, buttonText
  if (_size > 1) {
    message = "You're about to delete multiple items!"
    buttonText = `Delete ${_size} items`
  } else {
    message = `You're about to delete a ${viewObject(idLists).typeName}!`
    buttonText = 'Delete'
  }
  return app.config.globalProperties.$confirm(message, 'Warning', {
    type: 'warning',
    confirmButtonText: buttonText,
    confirmButtonClass: 'el-button--danger',
    showClose: false
  })
}

// copy in cytoscape-view.js (module dmx-cytoscape-renderer)
// TODO: unify selection models (see selection.js in dmx-topicmaps module)
function size (idLists) {
  return idLists.topicIds.length + idLists.assocIds.length
}

function viewObject (idLists) {
  const id = idLists.topicIds.length ? idLists.topicIds[0] : idLists.assocIds[0]
  return state.topicmap.getObject(id)
}

// ---

// TODO: adapt 4 functions

function hideTopic (id, getters, rootState, dispatch) {
  // update state
  unselectIfCascade(id, getters, rootState, dispatch)
  hideAssocsWithPlayer(id, dispatch)
  state.topicmap.getTopic(id).setVisibility(false)
  // update view
  dispatch('removeObject', id)
}

function hideAssoc (id, getters, rootState, dispatch) {
  // update state
  unselectIfCascade(id, getters, rootState, dispatch)
  if (!state.topicmap.hasAssoc(id)) {   // Note: idempotence is needed for hide-multi
    return
  }
  removeAssocsWithPlayer(id, dispatch)  // Note: topicmap contexts of *explicitly* hidden assocs are removed
  state.topicmap.removeAssoc(id)        // Note: topicmap contexts of *explicitly* hidden assocs are removed
  // update view
  dispatch('removeObject', id)
}

function deleteTopic (id, getters, rootState, dispatch) {
  handleTopicmapDeletion(id, getters, rootState, dispatch)
  //
  // update state
  // Note: unselecting (and possible route change) is done while processing DELETE directives (see webclient.js)
  removeAssocsWithPlayer(id, dispatch)
  state.topicmap.removeTopic(id)
  // update view
  dispatch('removeObject', id)
}

function deleteAssoc (id, dispatch) {
  if (!state.topicmap.hasAssoc(id)) {   // Note: idempotence is needed for delete-multi
    return
  }
  // update state
  // Note: unselecting (and possible route change) is done while processing DELETE directives (see webclient.js)
  removeAssocsWithPlayer(id, dispatch)
  state.topicmap.removeAssoc(id)
  // update view
  dispatch('removeObject', id)
}

/**
 * Updates "topicmapTopics" and "selectedTopicmapId" state in case the deleted topic is a topicmap.
 * Supports deletion of both, standard maps and special maps (e.g. a Geomap or Tableview).
 *
 * Preconditions:
 * - the current topicmap is a standard map AND
 * - the given "id" refers to a topic in that topicmap
 * OR
 * - the given "id" refers to a topicmap (standard or special).
 */
function handleTopicmapDeletion(id, getters, rootState, dispatch) {
  if (getters.topicmapTypeUri === 'dmx.topicmaps.topicmap') {
    // abort if deleted topic is not a topicmap
    const topic = state.topicmap.getTopic(id)
    if (topic.typeUri !== 'dmx.topicmaps.topicmap') {
      return
    }
    // sanity check
    if (state.topicmap.id !== id) {
      throw Error(`topicmap ${id} can't be deleted as it is not selected`)
    }
  }
  // select another topicmap, or create a new one if this was the last one
  const topicmapTopic = topicmapTopics(rootState).filter(topic => topic.id !== id)[0]
  if (topicmapTopic) {
    // Note: selecting a new topicmap is not strictly required. It would be selected while directives processing
    // anyways. But we do it here (before the delete request is actually sent) for quick user feedback.
    _selectTopicmap(topicmapTopic.id, dispatch)
  } else {
    // FIXME: synchronization. Create topicmap *before* processing response of delete request
    dispatch('createTopicmap', {})
  }
}

// ---

function hideAssocsWithPlayer (id, dispatch) {
  state.topicmap.getAssocsWithPlayer(id).forEach(assoc => {
    assoc.setVisibility(false)                  // update state
    dispatch('removeObject', assoc.id)          // update view
    hideAssocsWithPlayer(assoc.id, dispatch)    // recursion
  })
}

function removeAssocsWithPlayer (id, dispatch) {
  state.topicmap.getAssocsWithPlayer(id).forEach(assoc => {
    state.topicmap.removeAssoc(assoc.id)        // update state
    dispatch('removeObject', assoc.id)          // update view
    removeAssocsWithPlayer(assoc.id, dispatch)  // recursion
  })
}

function unselectIfCascade (id, getters, rootState, dispatch) {
  // console.log('unselectIfCascade', id)
  unselectIf(id, getters, rootState, dispatch)
  state.topicmap.getAssocsWithPlayer(id).forEach(assoc => {
    unselectIfCascade(assoc.id, getters, rootState, dispatch)       // recursion
  })
}

// State helper

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

function removeFromAllSelections (id) {
  Object.values(state.selections).forEach(selection => selection.remove(id))
}

function emptyAllSelectionsExcept (topicmapId) {
  Object.keys(state.selections).forEach(_topicmapId => {
    // Note: Object.keys() returns string keys. So we use non-strict equality here.
    if (_topicmapId != topicmapId) {    /* eslint eqeqeq: "off" */
      state.selections[_topicmapId].empty()
    }
  })
}

/**
 * Adapts selection (model, view, and route) of current topicmap to changed authorization.
 *
 * Prerequisite: the current topicmap is still readable after authorization change.
 *
 * Note: when authorization changes current selection might shrink, never expand. A single selection might
 * become empty (or stay single), a multi selection might become single or empty (or stay multi).
 */
function adaptSelection (getters, dispatch) {
  const selection = getters.selection
  const wasSingle = selection.isSingle()
  shrinkSelection(selection)
  if (selection.isEmpty()) {
    if (wasSingle) {
      dispatch('stripSelectionFromRoute')
    }
  } else if (selection.isSingle()) {
    const id = selection.getObjectId()
    if (wasSingle) {
      dispatch('renderAsSelected', {id, showDetails: getters.showInmapDetails})
    } else {
      dispatch(selection.getType() === 'topic' ? 'callTopicRoute' : 'callAssocRoute', id)
    }
  } else {
    _syncSelectMulti(selection, dispatch)
  }
}

function shrinkSelection (selection) {
  selection.topicIds = selection.topicIds.filter(id => state.topicmap.hasVisibleObject(id))
  selection.assocIds = selection.assocIds.filter(id => state.topicmap.hasVisibleObject(id))
}

function unselectIf (id, getters, rootState, dispatch) {
  // console.log('unselectIf', id)
  if (isSelected(id, rootState)) {
    dispatch('stripSelectionFromRoute', true)     // noViewUpdate=true
  }
  getters.selection.remove(id)
}

/**
 * @return  true if the given object ID represents the current single-selection, if there is one, falsish otherwise
 */
function isSelected (id, rootState) {
  return rootState.object?.id === id
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
    throw Error('selected workspace unknown')
  }
  return workspaceId
}

function __workspaceId (rootState) {
  return rootState.workspaces.workspaceId
}
