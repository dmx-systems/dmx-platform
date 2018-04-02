import dm5 from 'dm5'

const actions = {

  fetchTopicmap (_, id) {
    console.log('Loading geomap', id)
    // TODO: use geomaps service
    return dm5.restClient.getTopicmap(id)
  },

  // TODO: drop
  syncTopicmap (_, topicmap) {
    return topicmap
  }
}

export default {
  actions
}
