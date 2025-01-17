import dmx from 'dmx-api'

export default {

  actions: {

    createTopicType ({dispatch}, {name, pos}) {
      dmx.rpc.createTopicType(defaultTopicType(name)).then(topicType => {
        dispatch('putTopicType', topicType)
        dispatch('revealTopic', {topic: topicType, pos})
      })
    },

    createAssocType ({dispatch}, {name, pos}) {
      dmx.rpc.createAssocType(defaultAssocType(name)).then(assocType => {
        dispatch('putAssocType', assocType)
        dispatch('revealTopic', {topic: assocType, pos})
      })
    },

    createRoleType ({dispatch}, {name, pos}) {
      dmx.rpc.createRoleType(defaultRoleType(name)).then(roleType => {
        dispatch('putRoleType', roleType)
        dispatch('revealTopic', {topic: roleType, pos})
      })
    }
  }
}

function defaultTopicType (name) {
  return {
    // Note: a new type gets its default URI at server-side.
    // Also "typeUri" is provided at server-side (see ModelFactoryImpl).
    value: name,
    dataTypeUri: 'dmx.core.text',
    viewConfigTopics: [{
      typeUri: 'dmx.webclient.view_config',
      children: {
        'dmx.webclient.add_to_create_menu': true
      }
    }]
  }
}

function defaultAssocType (name) {
  return {
    // Note: a new type gets its default URI at server-side.
    // Also "typeUri" is provided at server-side (see ModelFactoryImpl).
    value: name,
    dataTypeUri: 'dmx.core.text'
  }
}

function defaultRoleType (name) {
  return {
    // Note: a new type gets its default URI at server-side.
    // Also "typeUri" is provided at server-side (see AccessLayer#createRoleType).
    value: name
  }
}
