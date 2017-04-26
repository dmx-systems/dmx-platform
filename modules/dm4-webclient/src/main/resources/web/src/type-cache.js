import dm5 from './rest-client'
import store from './store/webclient'
import Utils from './utils'

function init () {
  dm5.getAllTopicTypes().then(topicTypes => {
    store.state.topicTypes = Utils.mapByUri(topicTypes)
  }).catch(error => {
    console.error(error)
  })
  dm5.getAllAssocTypes().then(assocTypes => {
    store.state.assocTypes = Utils.mapByUri(assocTypes)
  }).catch(error => {
    console.error(error)
  })
}

export default {
  init
}
