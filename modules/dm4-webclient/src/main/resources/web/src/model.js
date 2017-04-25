class DeepaMehtaObject {
  constructor(object) {
    this.id      = object.id
    this.uri     = object.uri
    this.typeUri = object.type_uri
    this.value   = object.value
    this.childs  = object.childs
  }
}

class Topic extends DeepaMehtaObject {
}

class Assoc extends DeepaMehtaObject {
}

export { Topic, Assoc }
