import dm5 from 'dm5'

const state = {
  topicmap: undefined,      // view model: the rendered topicmap (a Topicmap object)
  topicmapTopics: []
}

const actions = {

  renderTopicmap ({dispatch}, id) {
    dm5.restClient.getTopicmap(id).then(topicmap => {
      // update view model
      state.topicmap = topicmap
      // sync view
      dispatch('syncTopicmap', topicmap)
    }).catch(error => {
      console.error(error)
    })
  },

  revealTopic ({dispatch}, {topic, pos, select}) {
    // update view model + sync view
    const op = _revealTopic(topic, pos, select, dispatch)
    // sync clients
    if (op.type === 'add') {
      dm5.restClient.addTopicToTopicmap(state.topicmap.id, topic.id, op.viewProps)
    } else if (op.type === 'show') {
      dm5.restClient.setTopicVisibility(state.topicmap.id, topic.id, true)
    }
  },

  revealAssoc ({dispatch}, {assoc, select}) {
    // update view model + sync view
    const op = _revealAssoc(assoc, select, dispatch)
    // sync clients
    if (op.type === 'add') {
      dm5.restClient.addAssocToTopicmap(state.topicmap.id, assoc.id)
    }
  },

  // TODO: add "select" param?
  revealRelatedTopic ({dispatch}, {relTopic, pos}) {
    // update view model + sync view
    const topicOp = _revealTopic(relTopic, pos, true, dispatch)      // select=true
    const assocOp = _revealAssoc(relTopic.assoc, false, dispatch)    // select=false
    // sync clients
    if (topicOp.type || assocOp.type) {
      dm5.restClient.addRelatedTopicToTopicmap(state.topicmap.id, relTopic.id, relTopic.assoc.id, topicOp.viewProps)
    }
  },

  onTopicDragged (_, {id, pos}) {
    state.topicmap.getTopic(id).setPosition(pos)                                  // update view model
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

  onHideTopic (_, id) {
    state.topicmap.removeAssocs(id)                                       // update view model
    state.topicmap.getTopic(id).setVisibility(false)
    // Note: the view is up-to-date already                               // sync view
    dm5.restClient.setTopicVisibility(state.topicmap.id, id, false)       // sync clients
  },

  onHideAssoc (_, id) {
    state.topicmap.removeAssoc(id)                                        // update view model
    // Note: the view is up-to-date already                               // sync view
    dm5.restClient.removeAssocFromTopicmap(state.topicmap.id, id)         // sync clients
  },

  onDeleteTopic ({dispatch}, id) {
    state.topicmap.removeAssocs(id)                                       // update view model
    state.topicmap.removeTopic(id)
    // Note: the view is up-to-date already                               // sync view
    dm5.restClient.deleteTopic(id).then(object => {                       // sync clients
      dispatch('_processDirectives', object.directives)
    })
  },

  onDeleteAssoc ({dispatch}, id) {
    state.topicmap.removeAssoc(id)                                        // update view model
    // Note: the view is up-to-date already                               // sync view
    dm5.restClient.deleteAssoc(id).then(object => {                       // sync clients
      dispatch('_processDirectives', object.directives)
    })
  },

  // WebSocket message processing

  _addTopicToTopicmap ({dispatch}, {topicmapId, viewTopic}) {
    if (topicmapId === state.topicmap.id) {
      state.topicmap.addTopic(new dm5.ViewTopic(viewTopic))               // update view model
      dispatch('syncAddTopic', viewTopic.id)                              // sync view
    }
  },

  _addAssocToTopicmap ({dispatch}, {topicmapId, assoc}) {
    if (topicmapId === state.topicmap.id) {
      state.topicmap.addAssoc(new dm5.Assoc(assoc))                       // update view model
      dispatch('syncAddAssoc', assoc.id)                                  // sync view
    }
  },

  _setTopicPosition ({dispatch}, {topicmapId, topicId, pos}) {
    if (topicmapId === state.topicmap.id) {
      state.topicmap.getTopic(topicId).setPosition(pos)                   // update view model
      dispatch('syncTopicPosition', topicId)                              // sync view
    }
  },

  _setTopicVisibility ({dispatch}, {topicmapId, topicId, visibility}) {
    if (topicmapId === state.topicmap.id) {
      // update view model
      if (!visibility) {
        state.topicmap.removeAssocs(topicId)
      }
      state.topicmap.getTopic(topicId).setVisibility(visibility)
      // sync view
      dispatch('syncTopicVisibility', topicId)
    }
  },

  _removeAssocFromTopicmap ({dispatch}, {topicmapId, assocId}) {
    if (topicmapId === state.topicmap.id) {
      state.topicmap.removeAssoc(assocId)                                 // update view model
      dispatch('syncRemoveAssoc', assocId)                                // sync view
    }
  },

  _processDirectives ({dispatch}, directives) {
    console.log(`Topicmaps: processing ${directives.length} directives ...`)
    directives.forEach(dir => {
      switch (dir.type) {
      case "UPDATE_TOPIC":
        updateTopic(dir.arg, dispatch)
        break
      case "DELETE_TOPIC":
        // TODO
        console.warn('Directive DELETE_TOPIC not yet implemented')
        break
      case "UPDATE_ASSOCIATION":
        updateAssoc(dir.arg, dispatch)
        break
      case "DELETE_ASSOCIATION":
        // TODO
        console.warn('Directive DELETE_ASSOCIATION not yet implemented')
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

// init state
dm5.restClient.getTopicsByType('dm4.topicmaps.topicmap').then(topics => {
  state.topicmapTopics = topics
})

export default {
  state,
  actions
}

// ---

// update view model + sync view

function _revealTopic (topic, pos, select, dispatch) {
  const op = {}
  const viewTopic = state.topicmap.getTopicIfExists(topic.id)
  if (!viewTopic) {
    const viewProps = {
      'dm4.topicmaps.x': pos.x,
      'dm4.topicmaps.y': pos.y,
      'dm4.topicmaps.visibility': true,
    }
    state.topicmap.addTopic(topic.newViewTopic(viewProps))                      // update view model
    dispatch('syncAddTopic', topic.id)                                          // sync view
    op.type = 'add'
    op.viewProps = viewProps
  } else {
    if (!viewTopic.isVisible()) {
      viewTopic.setVisibility(true)                                             // update view model
      dispatch('syncAddTopic', topic.id)                                        // sync view
      op.type = 'show'
    }
  }
  select && dispatch('syncSelect', topic.id)
  return op
}

function _revealAssoc (assoc, select, dispatch) {
  const op = {}
  const viewAssoc = state.topicmap.getAssocIfExists(assoc.id)
  if (!viewAssoc) {
    state.topicmap.addAssoc(assoc)                                              // update view model
    dispatch('syncAddAssoc', assoc.id)                                          // sync view
    op.type = 'add'
  }
  select && dispatch('syncSelect', assoc.id)
  return op
}

// Process Directives

function updateTopic (topic, dispatch) {
  const _topic = state.topicmap.getTopicIfExists(topic.id)
  if (_topic) {
    _topic.value = topic.value              // update view model
    dispatch('syncTopicLabel', topic.id)    // sync view
  }
}

function updateAssoc (assoc, dispatch) {
  const _assoc = state.topicmap.getAssocIfExists(assoc.id)
  if (_assoc) {
    _assoc.value = assoc.value              // update view model
    dispatch('syncAssocLabel', assoc.id)    // sync view
  }
}
