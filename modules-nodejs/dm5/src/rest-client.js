import http from 'axios'
import utils from './utils'
import { Topic, Assoc, TopicType, AssocType } from './model'

export default {

  // Note: exceptions thrown in the handlers passed to then/catch are swallowed silently!
  // They do not reach the caller. Apparently they're swallowed by the ES6 Promises
  // implementation, not by axios. See Matt Zabriskie's (the author of axios) comment here:
  // https://github.com/mzabriskie/axios/issues/42
  // See also:
  // http://jamesknelson.com/are-es6-promises-swallowing-your-errors/
  //
  // As a workaround we catch here explicitly and log the error at least.
  // Note: the caller continues to work with flawed (undefined) data then!

  getTopic (id, includeChilds) {
    const config = {params: {include_childs: includeChilds}}
    return http.get(`/core/topic/${id}`, config).then(response =>
      new Topic(response.data)
    ).catch(error => {
      console.error(error)
    })
  },

  getTopicsByType (typeUri) {
    return http.get(`/core/topic/by_type/${typeUri}`).then(response =>
      utils.instantiateMany(response.data, Topic)
    ).catch(error => {
      console.error(error)
    })
  },

  getAssoc (id, includeChilds) {
    const config = {params: {include_childs: includeChilds}}
    return http.get(`/core/association/${id}`, config).then(response =>
      new Assoc(response.data)
    ).catch(error => {
      console.error(error)
    })
  },

  getAllTopicTypes () {
    return http.get('/core/topictype/all').then(response =>
      utils.instantiateMany(response.data, TopicType)
    ).catch(error => {
      console.error(error)
    })
  },

  getAllAssocTypes () {
    return http.get('/core/assoctype/all').then(response =>
      utils.instantiateMany(response.data, AssocType)
    ).catch(error => {
      console.error(error)
    })
  },

  // Topicmaps ### TODO: move to topicmaps module and provide rest-client extension mechanism?

  getTopicmap (topicmapId) {
    return http.get(`/topicmap/${topicmapId}`).then(response =>
      response.data
    ).catch(error => {
      console.error(error)
    })
  },

  setTopicPosition (topicmapId, topicId, pos) {
    http.put(`/topicmap/${topicmapId}/topic/${topicId}/${pos.x}/${pos.y}`)
    .catch(error => {
      console.error(error)
    })
  }
}
