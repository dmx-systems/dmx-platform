export default {

  topicmapType: {
    uri: 'dm4.geomaps.geomap_renderer',
    name: "Geomap",
    storeModule: require('./geomaps').default,  // TODO
    comp: require('./components/dm5-geomap-renderer').default
  }
}
