import Vue from 'vue'

/**
 * Tracks single select/unselect operations while this tick
 * and handles the accumulated selection in the next tick.
 */
export default class Selection {

  constructor (handler) {
    this.topicIds = []
    this.assocIds = []
    this.handler = handler
    this.p = false    // tracks deferred handler invocation
  }

  // These 4 methods accumulate state changes, and invoke the handler in the next tick.
  // They are called *before* a route change.

  addTopic (id) {
    this._checkAddTopic(id)
    this.topicIds.push(id)
    this._postpone()
  }

  addAssoc (id) {
    this._checkAddAssoc(id)
    this.assocIds.push(id)
    this._postpone()
  }

  removeTopic (id) {
    const i = this._checkRemoveTopic(id)
    this.topicIds.splice(i, 1)
    this._postpone()
  }

  removeAssoc (id) {
    const i = this._checkRemoveAssoc(id)
    this.assocIds.splice(i, 1)
    this._postpone()
  }

  // These 2 methods manipulate the selection *silently*, that is without handler invocation.
  // They are called *after* a route change in order to adapt the state.

  setTopic (id) {
    // this._checkAddTopic(id)    // TODO: think about
    this.topicIds = [id]
  }

  setAssoc (id) {
    // this._checkAddAssoc(id)    // TODO: think about
    this.assocIds = [id]
  }

  // ---

  empty () {
    this.topicIds = []
    this.assocIds = []
  }

  isEmpty () {
    return this._size() === 0
  }

  isSingle () {
    return this._size() === 1
  }

  isMulti () {
    return this._size() > 1
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

  _checkAddTopic (id) {
    if (this.topicIds.includes(id)) {
      throw Error(`${id} is already in the selected topic list`)
    }
  }

  _checkAddAssoc (id) {
    if (this.assocIds.includes(id)) {
      throw Error(`${id} is already in the selected assoc list`)
    }
  }

  _checkRemoveTopic (id) {
    const i = this.topicIds.indexOf(id)
    if (i === -1) {
      throw Error(`${id} not found in the selected topic list`)
    }
    return i
  }

  _checkRemoveAssoc (id) {
    const i = this.assocIds.indexOf(id)
    if (i === -1) {
      throw Error(`${id} not found in the selected assoc list`)
    }
    return i
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
