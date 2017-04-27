import { Topic } from './model'

function instantiateMany (objects, clazz) {
  return objects.map(object => new clazz(object))
}

// ---

function instantiateChilds (childs) {
  for (var assocDefUri in childs) {
    childs[assocDefUri] = instantiateChild(childs[assocDefUri])
  }
  return childs
}

function instantiateChild (child) {
  if (Array.isArray(child)) {
    return child.map(topic => new Topic(topic))
  } else {
    return new Topic(child)
  }
}

// ---

function mapByUri (topics) {
  var map = {}
  topics.forEach(topic => map[topic.uri] = topic)
  return map
}

export default {
  instantiateMany,
  instantiateChilds,
  mapByUri
}
