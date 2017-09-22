/**
 * Syncs the route with the store.
 */
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

    callRoute (_, location) {
      router.push(location)
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

    stripSelectionFromRoute () {
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

/**
 * Selects the intial topicmap and workspace, and pushes the initial route if needed.
 */
function initialNavigation (route) {
  var topicmapId = route.params.topicmapId                  // FIXME: convert to number?
  const topicId  = route.params.topicId
  const assocId  = route.params.assocId
  console.log('### Initial navigation (topicmapId, topicId, assocId)', topicmapId, topicId, assocId)
  // 1) select topicmap
  if (topicmapId) {
    store.dispatch('displayTopicmap', topicmapId)           // just display topicmap, no route push
    if (topicId) {                                          // FIXME: 0 is a valid topic ID
      fetchTopic(topicId)
    }
    if (assocId) {
      fetchAssoc(assocId)
    }
  } else {
    topicmapId = dm5.utils.getCookie('dm4_topicmap_id')     // FIXME: convert to number?
    if (topicmapId) {
      console.log('Selecting topicmap', topicmapId, '(ID obtained from cookie)')
      store.dispatch('selectTopicmap', topicmapId)          // push initial route
    } else {
      console.log('No topicmap cookie present')
    }
  }
  // 2) select workspace
  // Note: at this stage a topicmap ID might or might not known. If *known* (either obtained from URL or from cookie)
  // the route is already up-to-date, no (further) push required. If *not* known the route still needs to be pushed.
  if (topicmapId) {
    getAssignedWorkspace(topicmapId).then(workspace => {
      console.log('Topicmap', topicmapId, 'is assigned to workspace', workspace.id)
      store.dispatch('setWorkspaceId', workspace.id)        // no route push
      store.dispatch('fetchTopicmapTopics', workspace.id)   // fetch data for topicmap selector
      console.log('### Initial navigation complete!')
    })
  } else {
    const workspace = store.state.workspaces.workspaceTopics[0]
    store.dispatch('selectWorkspace', workspace.id)         // push initial route (indirectly)
    console.log('### Initial navigation complete!')
  }
}

function navigate (to, from) {
  const topicmapId = to.params.topicmapId
  const oldTopicmapId = from.params.topicmapId
  // Note: path param values read from URL are strings. Path param values set by push() are numbers.
  // So we do *not* use exact equality (!==) here.
  if (topicmapId != oldTopicmapId) {
    store.dispatch('displayTopicmap', topicmapId)
    //
    getAssignedWorkspace(topicmapId).then(workspace => {
      store.dispatch('setWorkspaceId', workspace.id)
    })
  }
  //
  var selected
  //
  const topicId = to.params.topicId
  const oldTopicId = from.params.topicId
  if (topicId != oldTopicId) {
    if (topicId) {  // FIXME: 0 is a valid topic ID
      fetchTopic(topicId)
      selected = true
    }
  }
  //
  const assocId = to.params.assocId
  const oldAssocId = from.params.assocId
  if (assocId != oldAssocId) {
    if (assocId) {
      fetchAssoc(assocId)
      selected = true
    }
  }
  //
  if (!selected) {
    store.dispatch('emptyDisplay')
    store.dispatch('unsetSelection')
  }
}

// ---

const getAssignedWorkspace = dm5.restClient.getAssignedWorkspace

// ---

function fetchTopic (id) {
  dm5.restClient.getTopic(id, true).then(topic => {    // includeChilds=true
    store.dispatch('displayObject', topic)
    store.dispatch('setTopicSelection', id)
  })
}

function fetchAssoc (id) {
  dm5.restClient.getAssoc(id, true).then(assoc => {    // includeChilds=true
    store.dispatch('displayObject', assoc)
    store.dispatch('setAssocSelection', id)
  })
}
