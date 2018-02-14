export default store => ({

  storeModule: {
    name: 'details',
    module: require('./details')
  },

  components: [
    {
      comp: require('dm5-detail-panel'),
      mount: 'webclient',
      props: {
        object:          state => state.object,
        writable:        state => state.writable,
        tab:             state => state.details.tab,
        mode:            state => state.details.mode,
        objectRenderers: state => state.objectRenderers
      },
      listeners: {
        'tab-click': tab => {
          // console.log('dynamic tab-click', tab)
          store.dispatch('callRoute', {
            name: store.state.object.isTopic() ? 'topicDetail' : 'assocDetail',
            params: {detail: tab}
          })
        }
      }
    },
    {
      comp: require('./components/dm5-detail-panel-toggle'),
      mount: 'toolbar'
    }
  ]
})
