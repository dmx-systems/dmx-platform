import restClient from './rest-client'
import utils from './utils'

const state = {
  topicTypes: undefined,
  assocTypes: undefined
}

function init (store) {
  store.registerModule('typeCache', {
    state
  })
  //
  restClient.getAllTopicTypes().then(topicTypes => {
    state.topicTypes = utils.mapByUri(topicTypes)
  }).catch(error => {
    console.error(error)
  })
  restClient.getAllAssocTypes().then(assocTypes => {
    state.assocTypes = utils.mapByUri(assocTypes)
  }).catch(error => {
    console.error(error)
  })
}

function getTopicType (uri) {
  const type = state.topicTypes[uri]
  if (!type) {
    throw Error(`Topic type ${uri} not found in type cache`)
  }
  return type
}

function getAssocType (uri) {
  const type = state.assocTypes[uri]
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
