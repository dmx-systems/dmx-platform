export default {

  storeModule: {
    name: 'accesscontrol',
    module: require('./accesscontrol').default
  },

  components: [
    {
      comp: require('./components/dm5-login-dialog').default,
      mount: 'webclient'
    },
    {
      comp: require('./components/dm5-login-state').default,
      mount: 'toolbar-right'
    }
  ]
}
