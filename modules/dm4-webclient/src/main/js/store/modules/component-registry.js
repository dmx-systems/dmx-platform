var count = 0

const state = {
  components: {}
}

const actions = {
  registerComponent (_, {extensionPoint, component}) {
    var comps = state.components[extensionPoint]
    if (!comps) {
      state.components[extensionPoint] = comps = []
    }
    component._dm5_id = count++
    comps.push(component)
  }
}

export default {
  state,
  actions
}
