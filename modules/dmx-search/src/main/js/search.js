import dm5 from 'dm5'

const state = {

  visible: false,             // search widget visibility

  pos: undefined,             // search widget position in `model` and `render` coordinates
                              // (objects with 'x' and 'y' properties)
  options: {
    noSelect: false,          // Optional: if trueish the revealed topic will not be selected. Otherwise it will.
    topicHandler: undefined   // Optional: a handler that is invoked subsequently to "revealTopic".
                              // The revealed topic is passed.
  },

  extraMenuItems: []          // Extra type menu items which require special create logic.
}

const actions = {

  /**
   * @param   pos   `model` and `render` coordinates
   */
  openSearchWidget (_, {pos, options}) {
    // console.log('openSearchWidget', pos, options)
    state.visible = true
    state.pos = pos
    state.options = options
  },

  closeSearchWidget () {
    state.visible = false
  },

  registerExtraMenuItems (_, items) {
    state.extraMenuItems = [...state.extraMenuItems, ...items]
  }
}

const getters = {
  menuTopicTypes: (state, getters, rootState) => dm5.utils.filter(
    rootState.typeCache.topicTypes,
    topicType => topicType.getViewConfig('dmx.webclient.add_to_create_menu')
  )
}

export default {
  state,
  actions,
  getters
}
