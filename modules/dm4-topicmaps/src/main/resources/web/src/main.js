console.log('Loading Topicmaps main.js')

import storeModule from './topicmaps'

export default {

  init ({store}) {
    // install component
    console.log('Topicmaps init() called!!')
    store.dispatch('addToToolbar', require('./components/TopicmapSelect.vue'))
    // install store module
    store.registerModule('topicmaps', storeModule)
  }
}
