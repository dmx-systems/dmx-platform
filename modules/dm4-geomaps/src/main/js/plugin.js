export default {

  topicmapType: {
    uri: 'dm4.geomaps.geomap_renderer',
    name: "Geomap",
    storeModule: require('./geomaps').default,
    comp: require('./components/dm5-geomap').default
  }
}
