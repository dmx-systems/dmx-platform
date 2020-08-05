import dm5 from 'dmx-api'

export default {
  props: {
    // the Topic/Assoc to render; is never undefined;
    // may be an "empty" topic/assoc, without ID, with just type set
    object: {
      type: dm5.DMXObject,
      required: true
    }
  }
}
