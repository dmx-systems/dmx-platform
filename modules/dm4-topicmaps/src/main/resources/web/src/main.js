import storeModule from './topicmaps'

export default {

  init ({store}) {
    // install store module
    store.registerModule('topicmaps', storeModule)
    // install components
    store.dispatch('registerComponent', {
      extensionPoint: 'dm5.webclient.toolbar',
      component: require('./components/TopicmapSelect')
    })
    store.dispatch('registerComponent', {
      extensionPoint: 'dm5.webclient.content',
      component: require('./components/TopicmapPanel')
    })
  }
}
