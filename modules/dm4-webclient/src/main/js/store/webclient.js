import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

var compCount = 0

const state = {
  components: {}
}

const actions = {
  registerComponent (_, {extensionPoint, component}) {
    var comps = state.components[extensionPoint]
    if (!comps) {
      state.components[extensionPoint] = comps = []
    }
    component._dm5_id = compCount++
    comps.push(component)
  }
}

export default new Vuex.Store({
  state,
  actions
})
