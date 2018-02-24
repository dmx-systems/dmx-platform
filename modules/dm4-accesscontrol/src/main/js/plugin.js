export default {

  storeModule: {
    name: 'accesscontrol',
    module: require('./accesscontrol').default
  },

  components: [
    {
      comp: require('./components/dm5-login-dialog'),
      mount: 'webclient'
    },
    {
      comp: require('./components/dm5-login-state'),
      mount: 'toolbar-right'
    }
  ]
}
