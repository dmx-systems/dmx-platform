export default {

  state: {

    visible: false,             // search widget visibility

    pos: undefined,             // search widget position in `model` and `render` coordinates
                                // (objects with 'x' and 'y' properties)
    options: {
      noSelect: false,          // Optional: if trueish the revealed topic will not be selected. Otherwise it will.
      topicHandler: undefined   // Optional: a handler that is invoked subsequently to "revealTopic".
                                // The revealed topic is passed.
    },

    extraMenuItems: []          // Extra type menu items which require special create logic.
  },

  actions: {

    /**
     * @param   pos   `model` and `render` coordinates
     */
    openSearchWidget ({state}, {pos, options}) {
      // console.log('openSearchWidget', pos, options)
      state.visible = true
      state.pos = pos
      state.options = options || {}
    },

    closeSearchWidget ({state}) {
      state.visible = false
    },

    registerExtraMenuItems ({state}, items) {
      state.extraMenuItems = [...state.extraMenuItems, ...items]
    }
  },

  getters: {
    createTopicTypes: (state, getters, rootState) => {
      const topicTypes = rootState.typeCache.topicTypes     // undefined while webclient launch
      return topicTypes && Object.values(topicTypes)
        .filter(topicType => topicType.getViewConfig('dmx.webclient.add_to_create_menu'))
        .sort((tt1, tt2) => tt1.value.localeCompare(tt2.value))
    }
  }
}
