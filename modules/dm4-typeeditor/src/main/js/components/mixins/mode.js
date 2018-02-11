export default {
  props: {
    mode: {
      type: String,
      required: true,
      validator: mode => ['info', 'form'].includes(mode)
    }
  }
}
