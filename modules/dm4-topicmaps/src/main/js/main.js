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
      extensionPoint: 'dm5.webclient',
      component: require('dm5-topicmap-panel')
    }
  ]
}
