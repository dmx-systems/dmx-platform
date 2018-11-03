import dm5 from 'dm5'

const state = {
  geomap: undefined       // the rendered geomap (dm5.Geomap)
}

const actions = {

  fetchTopicmap (_, id) {
    console.log('fetchTopicmap', id, '(geomap-model)')
    return dm5.restClient.getGeomap(id)
  },

  renderTopicmap (_, {topicmap, writable, selection}) {
    state.geomap = topicmap
  }
}

export default {
  state,
  actions
}
