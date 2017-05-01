export default {

  storeModule: {
    name: 'topicmaps',
    module: require('./topicmaps')
  },

  components: [
    {
      extensionPoint: 'dm5.webclient.toolbar',
      component: require('./components/TopicmapSelect')
    },
    {
      extensionPoint: 'dm5.webclient.content',
      component: require('./components/TopicmapPanel')
    }
  ]
}
