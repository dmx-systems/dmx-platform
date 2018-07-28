export default {
  topicmapType: {
    uri: 'dm4.geomaps.geomap_renderer',
    name: "Geomap",
    renderer: () => import('./dm5-geomap-renderer' /* webpackChunkName: "leaflet" */)
  }
}
