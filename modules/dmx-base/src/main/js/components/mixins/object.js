import dm5 from 'dmx-api'

export default {
  props: {
    object: {
      type: dm5.DMXObject,
      required: true
    }
  }
}
