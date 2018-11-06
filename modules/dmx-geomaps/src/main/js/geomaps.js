const state = {
  geomap: undefined       // the rendered geomap (dm5.Geomap)
}

const actions = {

  // WebSocket messages

  _newGeoCoord (_, {geoCoordTopic}) {
    console.log('_newGeoCoord', geoCoordTopic)
    if (state.geomap) {
      state.geomap.geoCoordTopics.push(geoCoordTopic)
    } else {
      // Note: if the geomap is not loaded no update is required
      console.log('No geomap loaded')
    }
  }
}

export default {
  state,
  actions
}
