import Vue from 'vue'

/**
 * Tracks single select/unselect operations while current tick
 * and handles the accumulated selection in the next tick.
 *
 * TODO: architecture. The reusable dm5 components must not rely on this application specific class.
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
    this._checkAddTopic(id)                 // TODO: drop check and make idempotent?
    this.topicIds.push(id)
    this._defer()
  }

  // Note: called twice while box-selecting an assoc; the 2nd call is ignored
  addAssoc (id) {
    if (!this.includesAssoc(id)) {
      this.assocIds.push(id)
      this._defer()
    }
  }

  removeTopic (id) {
    const i = this._checkRemoveTopic(id)    // TODO: drop check and make idempotent?
    this.topicIds.splice(i, 1)
    this._defer()
  }

  // Note: called twice while unselecting an assoc; the 2nd call is ignored
  removeAssoc (id) {
    const i = this.assocIds.indexOf(id)
    if (i !== -1) {
      this.assocIds.splice(i, 1)
      this._defer()
    }
  }

  // These 4 methods manipulate the selection *silently*, that is without handler invocation.
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

  remove (id) {
    const i1 = this.topicIds.indexOf(id); if (i1 !== -1) this.topicIds.splice(i1, 1)
    const i2 = this.assocIds.indexOf(id); if (i2 !== -1) this.assocIds.splice(i2, 1)
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

  includesTopic (id) {
    return this.topicIds.includes(id)
  }

  includesAssoc (id) {
    return this.assocIds.includes(id)
  }

  includesId (id) {
    return this.includesTopic(id) || this.includesAssoc(id)
  }

  forEachId (visitor) {
    this.topicIds.forEach(visitor)
    this.assocIds.forEach(visitor)
  }

  // ---

  _checkAddTopic (id) {
    if (this.includesTopic(id)) {
      throw Error(`${id} is already in the selected topic list`)
    }
  }

  _checkRemoveTopic (id) {
    const i = this.topicIds.indexOf(id)
    if (i === -1) {
      throw Error(`${id} not found in the selected topic list`)
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
