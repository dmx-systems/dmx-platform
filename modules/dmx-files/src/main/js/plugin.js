export default ({store}) => ({

  storeModule: {
    name: 'files',
    module: require('./files').default
  },

  components: [
    {
      comp: require('./components/dmx-filebrowser-reveal').default,
      mount: 'toolbar-left'
    },
    {
      comp: require('./components/dmx-upload-dialog').default,
      mount: 'webclient'
    }
  ],

  objectRenderers: {
    'dmx.files.file':   require('./components/dmx-file-renderer').default,
    'dmx.files.folder': require('./components/dmx-folder-renderer').default
  },

  detailPanelButtons: {
    'dmx.files.folder': [
      {
        label: 'Upload File',
        handler: _ => store.dispatch('openUploadDialog')
      }
    ]
  }
})
