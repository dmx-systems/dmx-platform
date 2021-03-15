export default ({store}) => ({

  storeModule: {
    name: 'accesscontrol',
    module: require('./accesscontrol').default
  },

  components: [
    {
      comp: require('dmx-login-dialog').default,
      mount: 'webclient',
      props: {
        visible:    state => state.accesscontrol.visible,
        extensions: state => state.accesscontrol.extensions
      },
      listeners: {
        'logged-in': username => store.dispatch('initTypeCache').then(() => {
                                   store.dispatch('loggedIn', username)
                                 }),
        close:       _        => store.dispatch('closeLoginDialog')
      }
    },
    {
      comp: require('./components/dmx-login-state').default,
      mount: 'toolbar-right'
    }
  ],

  extraMenuItems: [{
    uri: 'dmx.accesscontrol.user_account',
    optionsComp: require('./components/dmx-user-account-options').default,
    create: (username, options, pos) => {
      store.dispatch('createUserAccount', {
        username,
        password: options.password
      }).then(topic =>
        store.dispatch('revealTopic', {topic, pos})
      )
    }
  }]
})
