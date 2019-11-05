import Vue from 'vue'

export default ({store}) => {
  return {

    storeModule: {
      name: 'details',
      module: require('./details').default
    },

    storeWatcher: [{
      getter: state => state.details.visible,
      callback: () => {
        Vue.nextTick(() => {
          store.dispatch('resizeTopicmapRenderer')
        })
      }
    }],

    components: [
      {
        comp: require('dm5-detail-panel').default,
        mount: 'webclient',
        props: {
          object:          (_, getters) => getters && getters.object,   // TODO: why is getters undefined on 1st call?
          writable:        state => state.writable,
          visible:         state => state.details.visible,
          pinned:          state => state.details.pinned,
          tab:             state => state.details.tab,
          mode:            state => state.details.mode,
          markerIds:       (_, getters) => getters && getters.visibleTopicIds,
          detailRenderers: state => state.detailRenderers,
          types:           state => ({
                             assocTypes: state.typeCache.assocTypes,
                             roleTypes:  state.typeCache.roleTypes
                           }),
          quillConfig:     state => state.quillConfig
        },
        listeners: {
          'tab-click':           tabClick,
          'edit':                ()              =>  store.dispatch('callDetailRoute', 'edit'),
          'submit':              object          => {store.dispatch('submit', object)
                                                     store.dispatch('callDetailRoute', 'info')},
          'submit-inline':       object          =>  store.dispatch('submit', object),
          'submit-view-config':  viewConfigTopic =>  store.dispatch('submit', viewConfigTopic),
          'child-topic-reveal':  relTopic        =>  store.dispatch('revealRelatedTopic', {relTopic}),
          'related-topic-click': relTopic        =>  store.dispatch('revealRelatedTopic', {relTopic}),
          'related-icon-click':  relTopic        =>  store.dispatch('revealRelatedTopic', {relTopic, noSelect: true}),
          'pin':                 pinned          =>  store.dispatch('setDetailPanelPinned', pinned)
        }
      }
    ]
  }

  function tabClick (tab) {
    // clicking 1st tab while in form mode is a no-op
    const details = store.state.details
    if (tab === 'info' && details.tab === 'info' && details.mode === 'form') {
      return
    }
    //
    store.dispatch('callDetailRoute', tab)
  }
}
