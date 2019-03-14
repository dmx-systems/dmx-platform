import dm5 from 'dm5'

const actions = {

  createTopicType ({dispatch}, {name, pos}) {
    console.log('Creating topic type', name)
    dm5.restClient.createTopicType(defaultTopicType(name)).then(topicType => {
      console.log('Created', topicType)
      dispatch('putTopicType', topicType)
      dispatch('revealTopic', {topic: topicType, pos})
    })
  },

  createAssocType ({dispatch}, {name, pos}) {
    console.log('Creating assoc type', name)
    dm5.restClient.createAssocType(defaultAssocType(name)).then(assocType => {
      console.log('Created', assocType)
      dispatch('putAssocType', assocType)
      dispatch('revealTopic', {topic: assocType, pos})
    })
  }
}

export default {
  actions
}

const defaultTopicType = name => ({
  // Note: a new type gets its default URI at server-side.
  // Also "typeUri" is provided at server-side (see ModelFactoryImpl).
  value: name,
  dataTypeUri: 'dmx.core.text',
  viewConfigTopics: [{
    typeUri: 'dmx.webclient.view_config',
    childs: {
      'dmx.webclient.add_to_create_menu': true
    }
  }]
})

const defaultAssocType = name => ({
  // Note: a new type gets its default URI at server-side.
  // Also "typeUri" is provided at server-side (see ModelFactoryImpl).
  value: name,
  dataTypeUri: 'dmx.core.text'
})
