import Vue from 'vue'

/**
 * Tracks single select/unselect operations while this tick
 * and handles the aggreagated selection in the next tick.
 */
export default class Selection {

  constructor (handler) {
    this.topicIds = []
    this.assocIds = []
    this.handler = handler
    this.p = false    // tracks deferred handler invocation
  }

  addTopic (id) {
    if (this.topicIds.includes(id)) {
      throw Error(`${id} is already in the selected topic list`)
    }
    this.topicIds.push(id)
    this._postpone()
  }

  addAssoc (id) {
    if (this.assocIds.includes(id)) {
      throw Error(`${id} is already in the selected assoc list`)
    }
    this.assocIds.push(id)
    this._postpone()
  }

  removeTopic (id) {
    const i = this.topicIds.indexOf(id)
    if (i === -1) {
      throw Error(`${id} not found in the selected topic list`)
    }
    this.topicIds.splice(i, 1)
    this._postpone()
  }

  removeAssoc (id) {
    const i = this.assocIds.indexOf(id)
    if (i === -1) {
      throw Error(`${id} not found in the selected assoc list`)
    }
    this.assocIds.splice(i, 1)
    this._postpone()
  }

  // ---

  isEmpty () {
    return this._size() === 0
  }

  isSingle () {
    return this._size() === 1
  }

  singleTopicId () {
    if (!this.isSingle()) {
      throw Error(`singleTopicId() called when there is no single selection`)
    }
    return this.topicIds[0]
  }

  singleAssocId () {
    if (!this.isSingle()) {
      throw Error(`singleAssocId() called when there is no single selection`)
    }
    return this.assocIds[0]
  }

  // ---

  _postpone () {
    if (!this.p) {
      Vue.nextTick(() => {
        this.handler()
        this.p = false
      })
      this.p = true
    }
  }

  _size () {
    return this.topicIds.length + this.assocIds.length
  }
}
