console.log('Loading Workspaces main.js')

import storeModule from './workspaces'
import http from 'axios'

export default {

  init ({store}) {
    // install component
    console.log('Workspaces init() called!!')
    store.dispatch("addToToolbar", require('./components/WorkspaceSelect'))
    // install store module
    store.registerModule('workspaces', storeModule)
    // init store state
    http.get('/core/topic/by_type/dm4.workspaces.workspace').then(response => {
      store.state.workspaces.workspaceTopics = response.data
    })
  }
}
