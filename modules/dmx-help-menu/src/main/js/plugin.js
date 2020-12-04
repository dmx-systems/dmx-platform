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
      comp: require('./components/dm5-about-box').default,
      mount: 'webclient'
    }
  ],

  helpMenuItems: [
    {
      label: 'Documentation',
      href: 'https://docs.dmx.systems'
    },
    {
      label: 'Forum',
      href: 'https://forum.dmx.systems'
    },
    {
      label: 'About DMX',
      action: 'openAboutBox',
      divided: true
    }
  ]
}
