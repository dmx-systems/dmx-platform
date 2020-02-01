import store from './store/webclient'
import dm5 from 'dm5'

export default {
  topic: [
    {label: 'Hide', multi: true, handler: idLists => store.dispatch('hideMulti', idLists)},
    {
      label: 'Edit', handler: id => store.dispatch('callTopicDetailRoute', {id, detail: 'edit'}),
      disabled: isTopicEditDisabled
    },
    {label: 'Related', handler: id => store.dispatch('callTopicDetailRoute', {id, detail: 'related'})},
    {label: 'Details', handler: id => store.dispatch('callTopicDetailRoute', {id, detail: 'info'})}
  ],
  topic_danger: [{
    label: 'Delete', multi: true, handler: idLists => store.dispatch('deleteMulti', idLists),
    disabled: isTopicDeleteDisabled
  }],
  assoc: [
    {label: 'Hide', multi: true, handler: idLists => store.dispatch('hideMulti', idLists)},
    {
      label: 'Edit', handler: id => store.dispatch('callAssocDetailRoute', {id, detail: 'edit'}),
      disabled: isAssocEditDisabled
    },
    {label: 'Related', handler: id => store.dispatch('callAssocDetailRoute', {id, detail: 'related'})},
    {label: 'Details', handler: id => store.dispatch('callAssocDetailRoute', {id, detail: 'info'})}
  ],
  assoc_danger: [{
    label: 'Delete', multi: true, handler: idLists => store.dispatch('deleteMulti', idLists),
    disabled: isAssocDeleteDisabled
  }]
}

/**
 * @return    a promise for a boolean
 */
function isTopicEditDisabled (id) {
  return isTopicWritable(id).then(writable => !writable)
}

/**
 * @return    a promise for a boolean
 */
function isAssocEditDisabled (id) {
  return isAssocWritable(id).then(writable => !writable)
}

/**
 * @return    a boolean or a promise for a boolean
 */
function isTopicDeleteDisabled (idLists) {
  return containUnselectedTopicmap(idLists) ||    // returns a boolean, so must be checked first
         containUnwritableObject(idLists)         // returns a promise
}

/**
 * @return    a promise for a boolean
 */
function isAssocDeleteDisabled (idLists) {
  return containUnwritableObject(idLists)
}

/**
 * @return    a boolean
 */
function containUnselectedTopicmap (idLists) {
  // only the selected topicmap is enabled for deletion
  const topicmap = store.state.topicmaps.topicmap
  return idLists.topicIds.some(id => {
    const topic = topicmap.getTopic(id)
    return topic.typeUri === 'dmx.topicmaps.topicmap' && topic.id !== topicmap.id
  })
}

/**
 * @return    a promise for a boolean
 */
function containUnwritableObject (idLists) {
  return Promise.all([
    ...idLists.topicIds.map(isTopicWritable),
    ...idLists.assocIds.map(isAssocWritable)
  ]).then(writables =>
    writables.some(writable => !writable)
  )
}

function isTopicWritable (id) {
  return dm5.permCache.isTopicWritable(id)
}

function isAssocWritable (id) {
  return dm5.permCache.isAssocWritable(id)
}
