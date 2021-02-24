import FilesRPC from './files-rpc'

export default ({dmx, axios: http}) => {

  const filesRPC = new FilesRPC(dmx, http)

  const state = {
    visible: false,     // Upload dialog visibility
    folderName: '',     // Name of folder to upload to
    path: ''            // Repo path to upload to
  }

  const actions = {

    revealFileBrowser ({dispatch}) {
      filesRPC.getFolderTopic('/').then(folder => {
        dispatch('revealTopic', {topic: folder})
      })
    },

    openUploadDialog (_, folderId) {
      state.visible = true
      dmx.rpc.getTopic(folderId, true).then(folder => {
        state.folderName = folder.children['dmx.files.folder_name'].value
        state.path = folder.children['dmx.files.path'].value
      })
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
