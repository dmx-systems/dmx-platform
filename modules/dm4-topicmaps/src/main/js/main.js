export default {

  storeModule: {
    name: 'topicmaps',
    module: require('./topicmaps')
  },

  components: {
    webclient: require('dm5-topicmap-panel'),
    toolbar: require('./components/TopicmapSelect')
  }
}
