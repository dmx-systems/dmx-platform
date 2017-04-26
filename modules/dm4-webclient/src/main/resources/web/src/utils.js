function instantiate (objects, clazz) {
  return objects.map(object => new clazz(object))
}

function mapByUri (topics) {
  var map = {}
  topics.forEach(topic => map[topic.uri] = topic)
  return map
}

export default {
  instantiate,
  mapByUri
}
