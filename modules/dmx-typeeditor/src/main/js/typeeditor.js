import dm5 from 'dm5'

const actions = {

  createTopicType ({dispatch}, {name, pos}) {
    console.log('Creating topic type', name)
    dm5.restClient.createTopicType(defaultTopicType(name)).then(topicType => {
      console.log('Created', topicType)
      dispatch('putTopicType', topicType)
      dispatch('revealTopic', {topic: topicType, pos, select: true})
    })
  },

  createAssocType ({dispatch}, {name, pos}) {
    console.log('Creating assoc type', name)
    dm5.restClient.createAssocType(defaultAssocType(name)).then(assocType => {
      console.log('Created', assocType)
      dispatch('putAssocType', assocType)
      dispatch('revealTopic', {topic: assocType, pos, select: true})
    })
  }
}

export default {
  actions
}

const defaultTopicType = name => ({
  // Note: no type "uri" is set here. A new type gets its default URI at server-side.
  // Also the "typeUri" is provided at server-side (see ModelFactoryImpl).
  value: name,
  dataTypeUri: 'dmx.core.text',
  indexModeUris: ['dmx.core.key', 'dmx.core.fulltext'],
  viewConfigTopics: [{
    typeUri: 'dmx.webclient.view_config',
    childs: {
      'dmx.webclient.add_to_create_menu': true
    }
  }]
})

const defaultAssocType = name => ({
  // Note: no type "uri" is set here. A new type gets its default URI at server-side.
  // Also the "typeUri" is provided at server-side (see ModelFactoryImpl).
  value: name,
  dataTypeUri: 'dmx.core.text',
  indexModeUris: ['dmx.core.key', 'dmx.core.fulltext']
})
