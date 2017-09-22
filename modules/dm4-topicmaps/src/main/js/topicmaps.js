import Vue from 'vue'
import dm5 from 'dm5'

const state = {

  topicmapId: undefined,      // ID of the displayed topicmap (master state).

  topicmap: undefined,        // The displayed topicmap (derived state) (a dm5.Topicmap object)

  topicmapTopics: {},         // Loaded topicmap topics (including childs), grouped by workspace ID:
                              //   {
                              //     workspaceId: {
                              //       topics: [topicmapTopic]    # array of dm5.Topic
                              //       selectedId: topicmapId
                              //     }
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
      state.topicmapTopics[rootState.workspaces.workspaceId].topics.push(topic)
      dispatch('selectTopicmap', topic.id)
    })
  },

  displayTopicmap (_, id) {
    console.log('Displaying topicmap', id)
    // update state
    state.topicmapId = id
    dm5.utils.setCookie('dm4_topicmap_id', id)
  },

  getTopicmap ({dispatch}, id) {
    getTopicmap(id).then(topicmap => {
      // update state
      state.topicmap = topicmap
      // sync view
      const renderTopicmap = dispatch('syncTopicmap', topicmap)
      const selection = state.selections[id]
      if (selection) {
        renderTopicmap.then(() => {
          dispatch('syncSelect', selection.id)
        })
      }
    })
  },

  selectTopicmap ({dispatch}, id) {
    const selection = state.selections[id]
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

  setTopicSelection ({dispatch}, id) {
    if (!state.topicmapId) {
      throw Error('state.topicmapId is not set')
    }
    console.log('Setting topic selection of topicmap', state.topicmapId, 'to', id)
    // update state
    state.selections[state.topicmapId] = {type: 'topic', id}
    // sync view
    dispatch('syncSelect', id)
  },

  setAssocSelection ({dispatch}, id) {
    if (!state.topicmapId) {
      throw Error('state.topicmapId is not set')
    }
    console.log('Setting assoc selection of topicmap', state.topicmapId, 'to', id)
    // update state
    state.selections[state.topicmapId] = {type: 'assoc', id}
    // sync view
    dispatch('syncSelect', id)
  },

  unsetSelection ({dispatch}) {
    if (!state.topicmapId) {
      throw Error('state.topicmapId is not set')
    }
    console.log('Unsetting selection of topicmap', state.topicmapId)
    // update state
    delete state.selections[state.topicmapId]
    // sync view
    dispatch('syncUnselect')
  },

  // TODO: we need a general approach to unify both situations: when we have the real object at hand,
  // and when we only have its ID. There should be only one "revealTopic" action.

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
   */
  revealTopic ({dispatch}, {topic, pos, select}) {
    // update state + sync view
    const op = _revealTopic(topic, pos, select, dispatch)
    // sync clients
    if (op.type === 'add') {
      dm5.restClient.addTopicToTopicmap(state.topicmap.id, topic.id, op.viewProps)
    } else if (op.type === 'show') {
      dm5.restClient.setTopicVisibility(state.topicmap.id, topic.id, true)
    }
  },

  revealAssoc ({dispatch}, {assoc, select}) {
    // update state + sync view
    const op = _revealAssoc(assoc, select, dispatch)
    // sync clients
    if (op.type === 'add') {
      dm5.restClient.addAssocToTopicmap(state.topicmap.id, assoc.id)
    }
  },

  // TODO: add "select" param?
  revealRelatedTopic ({dispatch}, {relTopic, pos}) {
    // update state + sync view
    const topicOp = _revealTopic(relTopic, pos, true, dispatch)      // select=true
    const assocOp = _revealAssoc(relTopic.assoc, false, dispatch)    // select=false
    // sync clients
    if (topicOp.type || assocOp.type) {
      dm5.restClient.addRelatedTopicToTopicmap(state.topicmap.id, relTopic.id, relTopic.assoc.id, topicOp.viewProps)
    }
  },

  onTopicDragged (_, {id, pos}) {
    state.topicmap.getTopic(id).setPosition(pos)                                  // update state
    // Note: the view is up-to-date already                                       // sync view
    dm5.restClient.setTopicPosition(state.topicmap.id, id, pos)                   // sync clients
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
    state.topicmap.removeAssocs(id)                                       // update state
    state.topicmap.getTopic(id).setVisibility(false)
    dispatch('unselectIf', id)
    // Note: the view is up-to-date already                               // sync view
    dm5.restClient.setTopicVisibility(state.topicmap.id, id, false)       // sync clients
  },

  hideAssoc ({dispatch}, id) {
    state.topicmap.removeAssoc(id)                                        // update state
    dispatch('unselectIf', id)
    // Note: the view is up-to-date already                               // sync view
    dm5.restClient.removeAssocFromTopicmap(state.topicmap.id, id)         // sync clients
  },

  deleteTopic ({dispatch}, id) {
    state.topicmap.removeAssocs(id)                                       // update state
    state.topicmap.removeTopic(id)
    // Note: the view is up-to-date already                               // sync view
    dm5.restClient.deleteTopic(id).then(object => {                       // sync clients
      dispatch('_processDirectives', object.directives)
    })
  },

  deleteAssoc ({dispatch}, id) {
    state.topicmap.removeAssoc(id)                                        // update state
    // Note: the view is up-to-date already                               // sync view
    dm5.restClient.deleteAssoc(id).then(object => {                       // sync clients
      dispatch('_processDirectives', object.directives)
    })
  },

  //

  /**
   * Selects a topicmap once a workspace is selected.
   */
  selectTopicmapForWorkspace ({dispatch}, workspaceId) {
    console.log('selectTopicmapForWorkspace', workspaceId)
    var p   // the topicmap to select (a Promise for a topicmapId)
    const topicmapTopics = state.topicmapTopics[workspaceId]
    if (topicmapTopics) {
      p = Promise.resolve(topicmapTopics.selectedId)
    } else {
      p = dispatch('fetchTopicmapTopics', workspaceId)
    }
    p.then(topicmapId => {
      dispatch('selectTopicmap', topicmapId)
    })
  },

  fetchTopicmapTopics (_, workspaceId) {
    console.log('Fetching topicmap topics for workspace', workspaceId)
    return dm5.restClient.getAssignedTopics(workspaceId, 'dm4.topicmaps.topicmap', true).then(topics => {
      // console.log('### Topicmap topics ready!', topics.length)                  // includeChilds=true
      const topicmapId = topics[0].id
      Vue.set(state.topicmapTopics, workspaceId, {
        topics,
        selectedId: topicmapId
      })
      return topicmapId
    })
  },

  // WebSocket message processing

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
    console.log(`Topicmaps: processing ${directives.length} directives`)
    directives.forEach(dir => {
      switch (dir.type) {
      case "UPDATE_TOPIC":
        updateTopic(dir.arg, dispatch)
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
      case "UPDATE_TOPIC_TYPE":
        // TODO
        console.warn('Directive UPDATE_TOPIC_TYPE not yet implemented')
        break
      case "DELETE_TOPIC_TYPE":
        // TODO
        console.warn('Directive DELETE_TOPIC_TYPE not yet implemented')
        break
      case "UPDATE_ASSOCIATION_TYPE":
        // TODO
        console.warn('Directive UPDATE_ASSOCIATION_TYPE not yet implemented')
        break
      case "DELETE_ASSOCIATION_TYPE":
        // TODO
        console.warn('Directive DELETE_ASSOCIATION_TYPE not yet implemented')
        break
      default:
        throw Error(`"${dir.type}" is an unsupported directive`)
      }
    })
  }
}

export default {
  state,
  actions
}

// ---

// Topicmap Cache

function getTopicmap (id) {
  var p   // a promise for a dm5.Topicmap
  const topicmap = getCachedTopicmap(id)
  if (topicmap) {
    p = Promise.resolve(topicmap)
  } else {
    console.log('Fetching topicmap', id)
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

// update state + sync view

/**
 * @param   topic   the topic to reveal (a dm5.Topic object).
 * @param   pos     the topic position in model coordinates (an object with "x", "y" properties).
 * @paran   select  if true the revealed topic is selected programmatically.
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

// Process Directives

function updateTopic (topic, dispatch) {
  const _topic = state.topicmap.getTopicIfExists(topic.id)
  if (_topic) {
    _topic.value = topic.value              // update state
    dispatch('syncTopicLabel', topic.id)    // sync view
  }
}

function updateAssoc (assoc, dispatch) {
  const _assoc = state.topicmap.getAssocIfExists(assoc.id)
  if (_assoc) {
    _assoc.value = assoc.value              // update state
    dispatch('syncAssocLabel', assoc.id)    // sync view
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
