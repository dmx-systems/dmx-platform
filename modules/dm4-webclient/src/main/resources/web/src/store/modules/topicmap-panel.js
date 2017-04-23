const state = {
  renderer: undefined
}

const actions = {
  registerTopicmapRenderer (_, renderer) {
    if (state.renderer) {
      throw new Error('Only one topicmap renderer can be registered')
    }
    state.renderer = renderer
  }
}

export default {
  state,
  actions
}
