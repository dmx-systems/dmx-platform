export default {

  storeModule: {
    name: 'typeeditor',
    module: require('./typeeditor')
  },

  components: {
    detailPanel: {
      'dm4.core.topic_type': require('./components/TypeRenderer')
    }
  }
}
