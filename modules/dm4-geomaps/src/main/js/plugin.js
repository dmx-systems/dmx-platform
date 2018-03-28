export default {

  topicmapType: {
    uri: 'dm4.geomaps.geomap_renderer',
    name: "Geomap",
    storeModule: require('./geomaps').default,  // TODO
    comp: () => import('./components/dm5-geomap-renderer' /* webpackChunkName: "leaflet" */)
  }
}
