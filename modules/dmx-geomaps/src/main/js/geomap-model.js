import dm5 from 'dm5'

const actions = {

  fetchTopicmap (_, id) {
    console.log('fetchTopicmap', id, '(geomap-model)')
    // TODO: use geomaps service
    return dm5.restClient.getTopicmap(id)
  },

  renderTopicmap (_, {topicmap, writable, selection}) {
  }
}

export default {
  actions
}
