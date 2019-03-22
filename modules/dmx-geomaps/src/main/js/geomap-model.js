import dm5 from 'dm5'

const actions = {

  // Topicmap Panel protocol

  fetchTopicmap (_, id) {
    console.log('fetchTopicmap', id, '(geomap-model)')
    return dm5.restClient.getGeomap(id)
  },

  renderTopicmap ({rootState}, {topicmap, writable, selection}) {
    rootState.geomaps.geomap = topicmap
  },

  // Geomap specific actions (module internal, dispatched from dm5-geomap-renderer component)

  _syncGeomapState ({rootState}, {center, zoom}) {
    // console.log('_syncGeomapState', center, zoom)
    // update server
    // if (_topicmapWritable) {     // TODO
      dm5.restClient.setGeomapState(rootState.geomaps.geomap.id, center.lng, center.lat, zoom)
    // }
  }
}

export default {
  actions
}
