const state = {
  items: [
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
  ],
  aboutBoxVisibility: false
}

const actions = {

  openAboutBox () {
    state.aboutBoxVisibility = true
  },

  closeAboutBox () {
    state.aboutBoxVisibility = false
  }
}

export default {
  state,
  actions
}
