export default {

  storeModule: {
    name: 'files',
    module: require('./files').default
  },

  components: [
    {
      comp: require('./components/dmx-filebrowser-reveal').default,
      mount: 'toolbar-left'
    }
  ],

  objectRenderers: {
    'dmx.files.file':   require('./components/dmx-file-renderer').default,
    'dmx.files.folder': require('./components/dmx-folder-renderer').default
  }
}
