import dmx from 'dmx-api'

export default {
  props: {
    object: {
      type: dmx.DMXObject,
      required: true
    }
  }
}
