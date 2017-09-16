import Vue from 'vue'
import VueRouter from 'vue-router'
import Webclient from './components/Webclient'
import store from './store/webclient'
import dm5 from 'dm5'

Vue.use(VueRouter)

const router = new VueRouter({
  routes: [
    {
      path: '/',
      component: Webclient
    },
    {
      path: '/topicmap/:topicmapId',
      name: 'topicmap',
      component: Webclient
    },
    {
      path: '/topicmap/:topicmapId/topic/:topicId',
      name: 'topic',
      component: Webclient
    },
    {
      path: '/topicmap/:topicmapId/assoc/:assocId',
      name: 'assoc',
      component: Webclient
    }
  ]
})

export default router

// Track initial navigation as it needs to be treated special.
// Note: the route store watcher is fired for the initial navigation too.
var isInitialNavigation = true

store.registerModule('routerModule', {

  state: {
    router
  },

  actions: {

    initialNavigation () {
      initialNavigation(router.currentRoute)
    },

    callTopicmapRoute (_, id) {
      router.push({
        name: 'topicmap',
        params: {
          topicmapId: id
        }
      })
    },

    callTopicRoute (_, id) {
      router.push({
        name: 'topic',
        params: {
          topicId: id
        }
      })
    },

    callAssocRoute (_, id) {
      router.push({
        name: 'assoc',
        params: {
          assocId: id
        }
      })
    },

    stripTopicOrAssocFromRoute () {
      router.push({
        name: 'topicmap'
      })
    }
  }
})

store.watch(
  state => state.routerModule.router.currentRoute,
  (to, from) => {
    if (isInitialNavigation) {
      isInitialNavigation = false
    } else {
      console.log('### Route watcher', to, from)
      navigate(to, from)
    }
  }
)

function initialNavigation (route) {
  var topicmapId = route.params.topicmapId                  // FIXME: convert to number?
  const topicId  = route.params.topicId
  const assocId  = route.params.assocId
  console.log('### Initial navigation (topicmapId, topicId, assocId)', topicmapId, topicId, assocId)
  // select topicmap
  if (topicmapId) {
    store.dispatch('displayTopicmap', topicmapId)
    if (topicId) {                                          // FIXME: 0 is a valid topic ID
      store.dispatch('fetchTopic', topicId)
    }
    if (assocId) {
      store.dispatch('fetchAssoc', assocId)
    }
  } else {
    topicmapId = dm5.utils.getCookie('dm4_topicmap_id')     // FIXME: convert to number?
    if (topicmapId) {
      console.log('Selecting topicmap', topicmapId, '(ID obtained from cookie)')
      store.dispatch('callTopicmapRoute', topicmapId)
    } else {
      console.log('No topicmap cookie present')
    }
  }
  // select workspace
  var p
  if (topicmapId) {
    p = dm5.restClient.getAssignedWorkspace(topicmapId).then(workspace => {
      console.log('Topicmap', topicmapId, 'is assigned to workspace', workspace.id)
      return workspace.id
    })
  } else {
    const workspaceId = store.state.workspaces.workspaceTopics[0].id
    p = Promise.resolve(workspaceId)
  }
  p.then(workspaceId => {
    store.dispatch('selectWorkspace', workspaceId)
  })
}

function navigate (to, from) {
  const topicmapId = to.params.topicmapId
  const oldTopicmapId = from.params.topicmapId
  console.log('$route watcher topicmapId', topicmapId, oldTopicmapId, topicmapId != oldTopicmapId)
  // Note: path param values read from URL are strings. Path param values set by push() are numbers.
  // So we do *not* use exact equality (!==) here.
  if (topicmapId != oldTopicmapId) {
    store.dispatch('displayTopicmap', topicmapId)
  }
  //
  var selected
  //
  const topicId = to.params.topicId
  const oldTopicId = from.params.topicId
  console.log('$route watcher topicId', topicId, oldTopicId, topicId != oldTopicId)
  if (topicId != oldTopicId) {
    if (topicId) {  // FIXME: 0 is a valid topic ID
      store.dispatch('fetchTopic', topicId)
      selected = true
    }
  }
  //
  const assocId = to.params.assocId
  const oldAssocId = from.params.assocId
  console.log('$route watcher assocId', assocId, oldAssocId, assocId != oldAssocId)
  if (assocId != oldAssocId) {
    if (assocId) {
      store.dispatch('fetchAssoc', assocId)
      selected = true
    }
  }
  //
  if (!selected) {
    store.dispatch('_unselect')
  }
}
