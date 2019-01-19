export default ({store}) => ({

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
  ],

  extraMenuItems: [{
    uri: 'dmx.accesscontrol.user_account',
    optionsComp: require('./components/dm5-user-account-options').default,
    create: (username, options, pos) => {
      store.dispatch('createUserAccount', {
        username,
        password: options.password
      }).then(topic =>
        store.dispatch('revealTopic', {topic, pos, select: true})
      )
    }
  }]
})
