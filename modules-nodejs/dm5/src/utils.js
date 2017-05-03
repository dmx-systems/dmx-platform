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

function mapById (objects) {
  return mapByProp(objects, 'id')
}

function mapByUri (objects) {
  return mapByProp(objects, 'uri')
}

function mapByProp (objects, prop) {
  var map = {}
  objects.forEach(object => map[object[prop]] = object)
  return map
}

// ---

export default {
  instantiateMany,
  instantiateChilds,
  mapById,
  mapByUri
}
