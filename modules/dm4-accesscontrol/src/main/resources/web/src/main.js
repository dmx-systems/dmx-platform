export default {

  storeModule: {
    name: 'accesscontrol',
    module: require('./accesscontrol')
  },

  components: [
    {
      extensionPoint: 'dm5.webclient.toolbar',
      component: require('./components/LoginState')
    },
    {
      extensionPoint: 'dm5.webclient.toolbar',
      component: require('./components/LoginDialog')
    }
  ]
}
