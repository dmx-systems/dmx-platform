export default (store) => ({

  storeModule: {
    name: 'typeeditor',
    module: require('./typeeditor')
  },

  components: {
    detailPanel: {
      'dm4.core.topic_type': require('./components/TypeRenderer'),
      'dm4.core.assoc_type': require('./components/TypeRenderer')
    }
  },

  extraMenuItems: [
    {
      uri: 'dm4.core.topic_type',
      label: 'Topic Type',
      create: name => {
        store.dispatch('createTopicType', name)
      }
    },
    {
      uri: 'dm4.core.assoc_type',
      label: 'Association Type',
      create: name => {
        store.dispatch('createAssocType', name)
      }
    }
  ]
})
