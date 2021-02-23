import FilesRPC from './files-rpc'

export default ({dmx, axios: http}) => {

  const filesRPC = new FilesRPC(dmx, http)

  const state = {
    visible: false      // Upload dialog visibility
  }

  const actions = {

    revealFileBrowser ({dispatch}) {
      filesRPC.getFolderTopic('/').then(folder => {
        dispatch('revealTopic', {topic: folder})
      })
    },

    openUploadDialog () {
      state.visible = true
    },

    closeUploadDialog () {
      state.visible = false
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
    }
  }

  return {
    state,
    actions
  }
}
