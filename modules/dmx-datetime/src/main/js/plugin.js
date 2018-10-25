export default {
  valueRenderers: {
    'dmx.datetime.date': require('./components/dm5-date-picker').default,
    'dmx.datetime.time': require('./components/dm5-time-picker').default
  }
}
