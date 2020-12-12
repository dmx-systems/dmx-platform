export default {
  valueRenderers: {
    'dmx.datetime.date': require('./components/dmx-date-picker').default,
    'dmx.datetime.time': require('./components/dmx-time-picker').default
  }
}
