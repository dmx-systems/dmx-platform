import storeModule from './topicmaps'

export default {

  init ({store}) {
    // install store module
    store.registerModule('topicmaps', storeModule)
    // install component
    store.dispatch('addToToolbar', require('./components/TopicmapSelect.vue'))
  }
}
