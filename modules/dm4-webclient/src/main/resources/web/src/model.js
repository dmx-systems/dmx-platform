class DeepaMehtaObject {
  constructor (object) {
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

class Type extends Topic {
  constructor (object) {
    super(object)
    this.dataType   = object.data_type_uri
    this.indexModes = object.index_mode_uris
    this.assocDefs  = object.assoc_defs
    this.viewConfig = object.view_config_topics
  }
}

class TopicType extends Type {
}

class AssocType extends Type {
}

export { Topic, Assoc, TopicType, AssocType }
