import dm5 from 'dm5'

const actions = {

  fetchTopicmap (_, id) {
    console.log('fetchTopicmap', id, '(geomap-model)')
    return dm5.restClient.getGeomap(id)
  },

  renderTopicmap ({rootState}, {topicmap, writable, selection}) {
    rootState.geomaps.geomap = topicmap
  }
}

export default {
  actions
}
