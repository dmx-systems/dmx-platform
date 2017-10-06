export default (store) => ({

  storeModule: {
    name: 'topicmaps',
    module: require('./topicmaps')
  },

  components: {
    webclient: require('dm5-topicmap-panel'),
    toolbar: require('./components/TopicmapSelect')
  },

  extraMenuItems: [{
    uri: 'dm4.topicmaps.topicmap',
    label: 'Topicmap',
    create: name => {
      store.dispatch('createTopicmap', name)
    }
  }]
})
