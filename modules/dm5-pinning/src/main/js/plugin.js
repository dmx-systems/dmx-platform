import dm5 from 'dm5'

export default {
  storeModule: {
    name: 'pinning',
    module: {
      actions: {
        setPinned (_, {topicmap, topicId, pinned}) {
          // update state
          topicmap.setTopicViewProp(topicId, 'dm5.pinning.pinned', pinned)
          // sync view
          // update server
          dm5.restClient.setViewProps(topicmap.id, topicId, {
            'dm5.pinning.pinned': pinned
          })
        }
      }
    }
  }
}
