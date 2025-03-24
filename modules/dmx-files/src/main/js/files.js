import FilesRPC from './files-rpc'

export default ({dmx, axios: http}) => {

  const filesRPC = new FilesRPC(dmx, http)

  return {

    state: {
      url: undefined      // URL of file to download
    },

    actions: {

      revealFileBrowser ({dispatch}) {
        filesRPC.getFolderTopic('/').then(folder => {
          dispatch('revealTopic', {topic: folder})
        })
      },

      downloadFile ({state, rootState}) {
        const repoPath = rootState.object.children['dmx.files.path'].value
        state.url = filesRPC.filerepoURL(repoPath) + '?download'
      },

      // RPC delegates

      getChildFileTopic (_, {folderId, repoPath}) {
        return filesRPC.getChildFileTopic(folderId, repoPath)
      },

      getChildFolderTopic (_, {folderId, repoPath}) {
        return filesRPC.getChildFolderTopic(folderId, repoPath)
      },

      getDirectoryListing (_, repoPath) {
        return filesRPC.getDirectoryListing(repoPath)
      },

      getFileContent (_, repoPath) {
        return filesRPC.getFileContent(repoPath)
      },

      openFile (_, fileTopicId) {
        filesRPC.openFile(fileTopicId)
      }
    }
  }
}
