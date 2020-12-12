export default {

  storeModule: {
    name: 'helpmenu',
    module: require('./help-menu').default
  },

  components: [
    {
      comp: require('./components/dmx-help-menu').default,
      mount: 'toolbar-right'
    },
    {
      comp: require('./components/dmx-about-box').default,
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
