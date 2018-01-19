export default {

  storeModule: {
    name: 'accesscontrol',
    module: require('./accesscontrol')
  },

  components: [
    {
      comp: require('./components/LoginDialog'),
      mount: 'webclient'
    },
    {
      comp: require('./components/LoginState'),
      mount: 'toolbar'
    }
  ]
}
