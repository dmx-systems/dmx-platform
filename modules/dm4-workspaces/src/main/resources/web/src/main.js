console.log('Loading Workspaces main.js')

import state from './state'

export default {

  init ({store}) {

    // install component
    console.log('Workspaces init() called!!')
    store.dispatch("addToToolbar", require('./components/WorkspaceSelect'))

    // install store module
    store.registerModule('workspaces', state)
  }
}
