export default class FilesRPC {

  constructor (dmx, http) {
    this.dmx = dmx
    this.http = http
  }

  // === File System Representation ===

  getFolderTopic (repoPath) {
    return this.http.get(`/files/folder/${encodeURIComponent(repoPath)}`)
      .then(response => new this.dmx.Topic(response.data))
  }

  // === File Repository ===

  getDirectoryListing (repoPath) {
    return this.http.get(`/files/${encodeURIComponent(repoPath)}`)
      .then(response => response.data)
  }
}
