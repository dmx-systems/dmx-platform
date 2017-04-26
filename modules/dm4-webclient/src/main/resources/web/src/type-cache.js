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

function getTopicType (uri) {
  const type = store.state.topicTypes[uri]
  if (!type) {
    throw Error(`Topic type ${uri} not found in type cache`)
  }
  return type
}

function getAssocType (uri) {
  const type = store.state.assocTypes[uri]
  if (!type) {
    throw Error(`Assoc type ${uri} not found in type cache`)
  }
  return type
}

export default {
  init,
  getTopicType,
  getAssocType
}
