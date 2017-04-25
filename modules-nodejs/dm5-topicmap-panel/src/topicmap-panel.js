const state = {
  topicmap: undefined,
  renderer: require('./components/CytoscapeRenderer.vue')
}

const actions = {
  registerTopicmapRenderer (_, renderer) {
    if (state.renderer) {
      // ### TODO: register multiple renderers by URI
      throw new Error('Only one topicmap renderer can be registered')
    }
    state.renderer = renderer
  },
  displayTopicmap (_, topicmap) {
    state.topicmap = topicmap
  }
}

export default {
  state,
  actions
}
