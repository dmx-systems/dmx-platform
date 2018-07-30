export default store => ({

  storeModule: {
    name: 'typeeditor',
    module: require('./typeeditor').default
  },

  detailPanel: {
    'dmx.core.topic_type': require('./components/dm5-type-renderer').default,
    'dmx.core.assoc_type': require('./components/dm5-type-renderer').default
  },

  extraMenuItems: [
    {
      uri: 'dmx.core.topic_type',
      label: 'Topic Type',
      create: name => {
        store.dispatch('createTopicType', name)
      }
    },
    {
      uri: 'dmx.core.assoc_type',
      label: 'Association Type',
      create: name => {
        store.dispatch('createAssocType', name)
      }
    }
  ]
})
