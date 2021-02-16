export default class FilesRPC {

  constructor (dmx, http) {
    this.dmx = dmx
    this.http = http
  }

  getFolderTopic (repoPath) {
    return this.http.get(`/files/folder/${encodeURIComponent(repoPath)}`).then(response =>
      new this.dmx.Topic(response.data)
    )
  }
}
