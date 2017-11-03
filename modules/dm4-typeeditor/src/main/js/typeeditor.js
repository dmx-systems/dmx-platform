import dm5 from 'dm5'

function defaultType (name) {
  return {
    // Note: no type "uri" is set here. A new  type gets its default URI at server-side.
    // Also the "typeUri" is provided at server-side (see ModelFactoryImpl).
    value: name,
    dataTypeUri: 'dm4.core.text',
    indexModeUris: ['dm4.core.key', 'dm4.core.fulltext'],
    viewConfigTopics: [{
      typeUri: 'dm4.webclient.view_config',
      childs: {
        'dm4.webclient.show_in_create_menu': true
      }
    }]
  }
}

const actions = {
  createTopicType ({dispatch}, name) {
    console.log('Creating topic type', name)
    dm5.restClient.createTopicType(defaultType(name)).then(topicType => {
      console.log('Topic type', topicType)
      dispatch('putTopicType', topicType)
      dispatch('revealTopic', {
        topic: topicType,
        pos: {x: 100, y: 100},   // TODO
        select: true
      })
    })
  }
}

export default {
  actions
}
