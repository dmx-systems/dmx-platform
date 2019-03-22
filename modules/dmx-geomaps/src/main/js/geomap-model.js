import dm5 from 'dm5'

const actions = {

  // Topicmap Panel protocol

  fetchTopicmap (_, id) {
    console.log('fetchTopicmap', id, '(geomap-model)')
    return dm5.restClient.getGeomap(id)
  },

  renderTopicmap ({rootState}, {topicmap, writable, selection}) {
    console.log('renderTopicmap', topicmap.viewProps)
    rootState.geomaps.geomap = topicmap
  },

  // Geomap specific actions (module internal, dispatched from dm5-geomap-renderer component)

  _storeGeomapState ({rootState}, {center, zoom}) {
    // console.log('_storeGeomapState', center, zoom)
    // update server
    // if (_topicmapWritable) {     // TODO
    dm5.restClient.setGeomapState(rootState.geomaps.geomap.id, center[1], center[0], zoom)
    // }
  }
}

export default {
  actions
}
