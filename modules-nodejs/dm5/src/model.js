import typeCache from './type-cache'
import utils from './utils'

class DeepaMehtaObject {
  constructor (object) {
    this.id      = object.id
    this.uri     = object.uri
    this.typeUri = object.type_uri
    this.value   = object.value
    this.childs  = utils.instantiateChilds(object.childs)
  }
}

class Topic extends DeepaMehtaObject {
  getType () {
    return typeCache.getTopicType(this.typeUri)
  }
}

class Assoc extends DeepaMehtaObject {

  constructor (assoc) {
    super(assoc)
    this.role1 = assoc.role_1
    this.role2 = assoc.role_2
  }

  getType () {
    return typeCache.getAssocType(this.typeUri)
  }

  getRole (roleTypeUri) {
    var match1 = this.role1.role_type_uri === roleTypeUri
    var match2 = this.role2.role_type_uri === roleTypeUri
    if (match1 && match2) {
      throw Error(`Both role types of association ${this.id} match ${roleTypeUri}`)
    }
    return match1 ? this.role1 : match2 ? this.role2 : undefined
  }
}

class Type extends Topic {

  constructor (type) {
    super(type)
    this.dataType   = type.data_type_uri
    this.indexModes = type.index_mode_uris
    this.assocDefs  = utils.instantiateMany(type.assoc_defs, AssocDef)
    this.viewConfig = type.view_config_topics
  }

  isSimple () {
    return this.dataType !== 'dm4.core.composite'
  }
}

class TopicType extends Type {
}

class AssocType extends Type {
}

class AssocDef extends Assoc {

  constructor (assocDef) {
    super(assocDef)
    this.parentCard = assocDef.parent_cardinality_uri
    this.childCard  = assocDef.child_cardinality_uri
    // derived properties
    this.parentTypeUri = this.getRole('dm4.core.parent_type').topic_uri
    this.childTypeUri  = this.getRole('dm4.core.child_type').topic_uri
    //
    const customAssocType = this.childs['dm4.core.assoc_type#dm4.core.custom_assoc_type']
    this.customAssocTypeUri = customAssocType && customAssocType.uri
    this.assocDefUri = this.childTypeUri + (this.customAssocTypeUri ? "#" + this.customAssocTypeUri : "")
    //
    this.includeInLabel = this.childs['dm4.core.include_in_label'].value
  }

  getChildType () {
    return typeCache.getTopicType(this.childTypeUri)
  }
}

class Topicmap extends Topic {

  constructor (topicmap) {
    super(topicmap.info)
    this.topics = utils.mapById(utils.instantiateMany(topicmap.topics, TopicmapTopic))
    this.assocs = utils.mapById(utils.instantiateMany(topicmap.assocs, Assoc))
  }

  getTopic (id) {
    var topic = this.topics[id]
    if (!topic) {
      throw Error(`Topic ${id} not found in topicmap ${this.id}`)
    }
    return topic
  }

  getAssoc (id) {
    var assoc = this.assocs[id]
    if (!assoc) {
      throw Error(`Assoc ${id} not found in topicmap ${this.id}`)
    }
    return assoc
  }

  forEachTopic (visitor) {
    this.forEachValue(this.topics, visitor)
  }

  forEachAssoc (visitor) {
    this.forEachValue(this.assocs, visitor)
  }

  forEachValue (map, visitor) {
    for (var key in map) {
      visitor(map[key])
    }
  }
}

class TopicmapTopic extends Topic {

  constructor (topic) {
    super(topic)
    this.viewProps = topic.view_props
  }

  getPosition () {
    return {
      x: this.viewProps['dm4.topicmaps.x'],
      y: this.viewProps['dm4.topicmaps.y']
    }
  }

  setPosition (x, y) {
    this.viewProps['dm4.topicmaps.x'] = x
    this.viewProps['dm4.topicmaps.y'] = y
  }
}

export { Topic, Assoc, TopicType, AssocType, Topicmap }
