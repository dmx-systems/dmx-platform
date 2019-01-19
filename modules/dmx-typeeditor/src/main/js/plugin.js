export default ({store}) => ({

  storeModule: {
    name: 'typeeditor',
    module: require('./typeeditor').default
  },

  objectRenderers: {
    'dmx.core.topic_type': require('./components/dm5-type-renderer').default,
    'dmx.core.assoc_type': require('./components/dm5-type-renderer').default
  },

  extraMenuItems: [
    {
      uri: 'dmx.core.topic_type',
      create: (name, _, pos) => {
        store.dispatch('createTopicType', {name, pos})
      }
    },
    {
      uri: 'dmx.core.assoc_type',
      create: (name, _, pos) => {
        store.dispatch('createAssocType', {name, pos})
      }
    }
  ]
})
