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
  let urlPresent
  // 1) select topicmap
  let topicmapId = route.params.topicmapId                        // FIXME: convert to number?
  const topicId  = route.params.topicId
  const assocId  = route.params.assocId
  if (topicmapId) {
    console.log('### Initial navigation (topicmapId, topicId, assocId obtained from URL)', topicmapId, topicId, assocId)
    urlPresent = true
  } else {
    topicmapId = dm5.utils.getCookie('dm4_topicmap_id')           // FIXME: convert to number?
    if (topicmapId) {
      console.log('### Initial navigation (topicmap ID', topicmapId, 'obtained from cookie)')
    } else {
      console.log('### Initial navigation (no topicmap cookie present)')
    }
  }
  // 2) select workspace
  // Note: at this stage a topicmap ID might or might not known. If *known* (either obtained from URL or from cookie)
  // the route is already up-to-date, no (further) push required. If *not* known the route still needs to be pushed.
  if (topicmapId) {
    getAssignedWorkspace(topicmapId).then(workspace => {
      console.log('Topicmap', topicmapId, 'is assigned to workspace', workspace.id)
      store.dispatch('setWorkspaceId', workspace.id)              // no route push
      store.dispatch('fetchTopicmapTopics')                       // data for topicmap selector
      if (urlPresent) {
        const p = store.dispatch('displayTopicmap', topicmapId)   // no route push
        topicId && fetchTopic(topicId, p)                         // FIXME: 0 is a valid topic ID
        assocId && fetchAssoc(assocId, p)
      } else {
        store.dispatch('callTopicmapRoute', topicmapId)           // push initial route
      }
    })
  } else {
    const workspace = store.state.workspaces.workspaceTopics[0]
    store.dispatch('selectWorkspace', workspace.id)               // push initial route (indirectly)
  }
  console.log('### Initial navigation complete!')
}

function navigate (to, from) {
  const topicmapId = to.params.topicmapId
  const oldTopicmapId = from.params.topicmapId
  var p   // a promise resolved once the topicmap rendering is complete
  // Note: path param values read from URL are strings. Path param values set by push() are numbers.
  // So we do *not* use exact equality (!==) here.
  if (topicmapId != oldTopicmapId) {
    // Note: the workspace must be set *before* the topicmap is displayed.
    // See preconditions at "displayTopicmap".
    p = new Promise(resolve => {
      getAssignedWorkspace(topicmapId).then(workspace => {
        store.dispatch('setWorkspaceId', workspace.id)
        store.dispatch('displayTopicmap', topicmapId).then(resolve)
      })
    })
  } else {
    p = Promise.resolve()
  }
  //
  var selected
  //
  const topicId = to.params.topicId
  const oldTopicId = from.params.topicId
  if (topicId != oldTopicId) {
    if (topicId) {  // FIXME: 0 is a valid topic ID
      fetchTopic(topicId, p)
      selected = true
    }
  }
  const assocId = to.params.assocId
  const oldAssocId = from.params.assocId
  if (assocId != oldAssocId) {
    if (assocId) {
      fetchAssoc(assocId, p)
      selected = true
    }
  }
  if (!selected) {
    unsetSelection(p)
  }
}

// ---

const getAssignedWorkspace = dm5.restClient.getAssignedWorkspace

// ---

/**
 * Fetches the topic with the given ID, displays it in the detail panel, and render it as selected in the topicmap
 * panel.
 *
 * @param   p   a promise resolved once the topicmap rendering is complete.
 */
function fetchTopic (id, p) {
  p.then(() => {
    store.dispatch('setTopicSelection', id)
  })
  dm5.restClient.getTopic(id, true).then(topic => {    // includeChilds=true
    store.dispatch('displayObject', topic)
  })
}

/**
 * Fetches the assoc with the given ID, displays it in the detail panel, and render it as selected in the topicmap
 * panel.
 *
 * @param   p   a promise resolved once the topicmap rendering is complete.
 */
function fetchAssoc (id, p) {
  p.then(() => {
    store.dispatch('setAssocSelection', id)
  })
  dm5.restClient.getAssoc(id, true).then(assoc => {    // includeChilds=true
    store.dispatch('displayObject', assoc)
  })
}

function unsetSelection(p) {
  p.then(() => {
    store.dispatch('unsetSelection')
  })
  store.dispatch('emptyDisplay')
}
