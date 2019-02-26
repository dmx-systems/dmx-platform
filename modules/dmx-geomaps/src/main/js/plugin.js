export default {

  storeModule: {
    name: 'geomaps',
    module: require('./geomaps').default
  },

  topicmapType: {
    uri: 'dmx.geomaps.geomap',
    name: "Geomap",
    renderer: () => import('./dm5-geomap-renderer' /* webpackChunkName: "leaflet" */)
  }
}
