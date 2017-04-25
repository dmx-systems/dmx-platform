const state = {
  object: undefined   // topic/assoc to display
}

const actions = {
  displayObject (_, object) {
    state.object = object
  }
}

export default {
  state,
  actions
}
