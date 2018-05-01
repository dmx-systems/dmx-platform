import Vue from 'vue'

/**
 * Tracks single select/unselect operations while current tick
 * and handles the accumulated selection in the next tick.
 */
export default class Selection {

  constructor (handler) {
    this.topicIds = []
    this.assocIds = []
    this._handler = handler
    this._p = false    // tracks deferred handler invocation
  }

  // These 4 methods accumulate state changes, and defers handler invocation to the next tick.
  // They are called *before* a route change.

  addTopic (id) {
    this._checkAddTopic(id)
    this.topicIds.push(id)
    this._defer()
  }

  addAssoc (id) {
    this._checkAddAssoc(id)
    this.assocIds.push(id)
    this._defer()
  }

  removeTopic (id) {
    const i = this._checkRemoveTopic(id)
    this.topicIds.splice(i, 1)
    this._defer()
  }

  removeAssoc (id) {
    const i = this._checkRemoveAssoc(id)
    this.assocIds.splice(i, 1)
    this._defer()
  }

  // These 3 methods manipulate the selection *silently*, that is without handler invocation.
  // They are called *after* a route change in order to adapt the state.

  setTopic (id) {
    // this._checkAddTopic(id)    // TODO: think about
    this.topicIds = [id]
    this.assocIds = []
  }

  setAssoc (id) {
    // this._checkAddAssoc(id)    // TODO: think about
    this.topicIds = []
    this.assocIds = [id]
  }

  empty () {
    this.topicIds = []
    this.assocIds = []
  }

  // ---

  size () {
    return this.topicIds.length + this.assocIds.length
  }

  isEmpty () {
    return this.size() === 0
  }

  isSingle () {
    return this.size() === 1
  }

  isMulti () {
    return this.size() > 1
  }

  getObjectId () {
    this._checkSingle('getObjectId')
    return this.getType() === 'topic' ? this.topicIds[0] : this.assocIds[0]
  }

  getType () {
    this._checkSingle('getType')
    return this.topicIds.length ? 'topic' : 'assoc'
  }

  forEachId (visitor) {
    this.topicIds.forEach(visitor)
    this.assocIds.forEach(visitor)
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

  _checkSingle (fnName) {
    if (!this.isSingle()) {
      throw Error(`${fnName}() called when there is no single selection`)
    }
  }

  // ---

  _defer () {
    if (!this._p) {
      Vue.nextTick(() => {
        this._handler(this)
        this._p = false
      })
      this._p = true
    }
  }
}