import FilesRPC from './files-rpc'

export default ({dmx, axios: http}) => {

  const filesRPC = new FilesRPC(dmx, http)

  return {
    actions: {

      revealFileBrowser ({dispatch}) {
        filesRPC.getFolderTopic('/').then(folder => {
          dispatch('revealTopic', {topic: folder})
        })
      },

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
      }
    }
  }
}
