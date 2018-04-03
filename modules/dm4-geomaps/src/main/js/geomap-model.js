import dm5 from 'dm5'

const actions = {

  fetchTopicmap (_, id) {
    console.log('Loading geomap', id)
    // TODO: use geomaps service
    return dm5.restClient.getTopicmap(id)
  },

  renderTopicmap (_, topicmap) {
  }
}

export default {
  actions
}
