console.log('Loading Topicmaps main.js')

import storeModule from './topicmaps'
import http from 'axios'

export default {

  init ({store}) {
    // install component
    console.log('Topicmaps init() called!!')
    store.dispatch('addToToolbar', require('./components/TopicmapSelect'))
    // install store module
    store.registerModule('topicmaps', storeModule)
    // init store state
    http.get('/core/topic/by_type/dm4.topicmaps.topicmap').then(response => {
      store.state.topicmaps.topicmapTopics = response.data
    })
  }
}
