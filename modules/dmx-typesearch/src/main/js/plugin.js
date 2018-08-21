export default store => ({

  storeModule: {
    name: 'search',
    module: require('./search').default
  },

  components: [
    {
      comp: require('dm5-search-widget').default,
      mount: 'webclient',
      props: {
        visible:        state => state.search.visible,
        pos:            state => state.search.pos,
        options:        state => state.search.options,
        extraMenuItems: state => state.search.extraMenuItems,
        menuTopicTypes: (_, getters) => getters && getters.menuTopicTypes // TODO: why is getters undefined on 1st call?
      }
    }
  ]
})
