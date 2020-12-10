import dmx from 'dmx-api'

export default {
  props: {
    compDef: {
      type: dmx.CompDef,
      required: true
    }
  }
}
