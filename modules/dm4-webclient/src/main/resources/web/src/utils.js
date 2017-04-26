function newTypes (types, typeClass) {
  return types.map(type => new typeClass(type))
}

function mapByUri (topics) {
  var map = {}
  topics.forEach(topic => map[topic.uri] = topic)
  return map
}

export default {
  newTypes,
  mapByUri
}
