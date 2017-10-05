export default {

  storeModule: {
    name: 'accesscontrol',
    module: require('./accesscontrol')
  },

  components: {
    webclient: require('./components/LoginDialog'),
    toolbar: require('./components/LoginState')
  }
}
