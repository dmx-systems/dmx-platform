export default {
  topicmapType: {
    uri: 'dmx.geomaps.geomap_renderer',
    name: "Geomap",
    renderer: () => import('./dm5-geomap-renderer' /* webpackChunkName: "leaflet" */)
  }
}
