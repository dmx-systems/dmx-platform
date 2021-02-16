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

  valueRenderers: {
    'dmx.files.folder_content': require('./components/dmx-folder-content').default
  }
}
