import FilesRPC from './files-rpc'

export default ({dmx, axios: http}) => {

  const filesRPC = new FilesRPC(dmx, http)

  return {
    actions: {
      revealFileBrowser ({dispatch}) {
        filesRPC.getFolderTopic('/').then(folder => {
          dispatch('revealTopic', {topic: folder})
        })
      }
    }
  }
}
