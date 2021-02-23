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

  contextCommands: {
    topic: topic => {
      if (topic.typeUri === 'dmx.files.folder') {
        return [{
          label: 'Upload File',
          handler: id => {
            store.dispatch('openUploadDialog')
          }
        }]
      }
    }
  }
})
