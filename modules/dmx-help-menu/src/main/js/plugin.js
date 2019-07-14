export default {

  storeModule: {
    name: 'helpmenu',
    module: require('./help-menu').default
  },

  components: [
    {
      comp: require('./components/dm5-help-menu').default,
      mount: 'toolbar-right'
    },
    {
      comp: require('./components/dm5-help-dialog').default,
      mount: 'webclient'
    }
  ]
}
