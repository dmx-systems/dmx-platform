import FilesRPC from './files-rpc'

export default ({dmx, axios: http}) => {

  const filesRPC = new FilesRPC(dmx, http)

  const state = {
    // dialog
    visible: false,     // Upload dialog visibility
    // download
    url: undefined,     // URL of file to download
    // upload
    folderName: '',     // Name of folder to upload to
    path: ''            // Repo path to upload to
  }

  const actions = {

    revealFileBrowser ({dispatch}) {
      filesRPC.getFolderTopic('/').then(folder => {
        dispatch('revealTopic', {topic: folder})
      })
    },

    downloadFile ({rootState}) {
      const repoPath = rootState.object.children['dmx.files.path'].value
      state.url = filesRPC.filerepoURL(repoPath) + '?download'
    },

    openUploadDialog ({rootState}) {
      state.visible = true
      const folder = rootState.object
      state.folderName = folder.children['dmx.files.folder_name'].value
      state.path = folder.children['dmx.files.path'].value
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
