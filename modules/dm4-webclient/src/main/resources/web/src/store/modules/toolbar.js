var compId = 0

const state = {
  components: []
}

const actions = {
  addToToolbar (_, component) {
    component._dm4_id = compId++
    state.components.push(component)
  }
}

export default {
  state,
  actions
}
