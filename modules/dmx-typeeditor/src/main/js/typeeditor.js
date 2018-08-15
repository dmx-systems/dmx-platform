import dm5 from 'dm5'

function defaultType (name) {
  return {
    // Note: no type "uri" is set here. A new type gets its default URI at server-side.
    // Also the "typeUri" is provided at server-side (see ModelFactoryImpl).
    value: name,
    dataTypeUri: 'dmx.core.text',
    indexModeUris: ['dmx.core.key', 'dmx.core.fulltext'],
    viewConfigTopics: [{
      typeUri: 'dmx.webclient.view_config',
      childs: {
        'dmx.webclient.show_in_create_menu': true
      }
    }]
  }
}

const actions = {
  createTopicType ({dispatch}, {name, pos}) {
    console.log('Creating topic type', name)
    dm5.restClient.createTopicType(defaultType(name)).then(topicType => {
      console.log('Created', topicType)
      dispatch('putTopicType', topicType)
      dispatch('revealTopic', {topic: topicType, pos, select: true})
    })
  }
}

export default {
  actions
}
