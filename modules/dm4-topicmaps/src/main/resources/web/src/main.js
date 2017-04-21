import storeModule from './topicmaps'

export default {

  init ({store}) {
    // install component
    store.dispatch('addToToolbar', require('./components/TopicmapSelect.vue'))
    // install store module
    store.registerModule('topicmaps', storeModule)
  }
}
