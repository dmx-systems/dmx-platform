/**
 * The router.
 * - Adapts app state when route changes.
 * - Sets up initial app state according to start URL.
 */

import Vue from 'vue'
import VueRouter from 'vue-router'
import Webclient from './components/dm5-webclient'
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
    },
    {
      path: '/topicmap/:topicmapId/topic/:topicId/:detail',
      name: 'topicDetail',
      component: Webclient
    },
    {
      path: '/topicmap/:topicmapId/assoc/:assocId/:detail',
      name: 'assocDetail',
      component: Webclient
    }
  ]
})

export default router

store.registerModule('routerModule', {

  state: {
    router
  },

  actions: {

    initialNavigation () {
      initialNavigation(router.currentRoute)
    },

    callRoute (_, location) {
      // console.log('callRoute', location)
      router.push(location)
    },

    callTopicmapRoute (_, id) {
      router.push({
        name: 'topicmap',
        params: {topicmapId: id}
      })
    },

    callTopicRoute ({rootState}, id) {
      router.push({
        name: rootState.details.visible ? 'topicDetail' : 'topic',
        params: {topicId: id}
      })
    },

    callAssocRoute ({rootState}, id) {
      router.push({
        name: rootState.details.visible ? 'assocDetail' : 'assoc',
        params: {assocId: id}
      })
    },

    stripSelectionFromRoute () {
      router.push({
        name: 'topicmap'
      })
    },

    callDetailRoute (_, detail) {
      const object = store.state.object
      if (!object) {
        throw Error('callDetailRoute when no object is selected')
      }
      router.push({
        name: object.isTopic() ? 'topicDetail' : 'assocDetail',
        params: {detail}
      })
    },

    stripDetailFromRoute () {
      const object = store.state.object
      if (!object) {
        throw Error('stripDetailFromRoute when no object is selected')
      }
      router.push({
        name: object.isTopic() ? 'topic' : 'assoc'
      })
    }
  }
})

function registerRouteWatcher () {
  store.watch(
    state => state.routerModule.router.currentRoute,
    (to, from) => {
      // console.log('### Route watcher', to, from)
      navigate(to, from)
    }
  )
}

/**
 * Sets up initial app state according to start URL.
 * Selects the intial topicmap and workspace, and pushes the initial route if needed.
 */
function initialNavigation (route) {
  //
  registerRouteWatcher()
  //
  let urlPresent
  // 1) select topicmap
  let topicmapId = route.params.topicmapId                        // FIXME: convert to number?
  const topicId  = route.params.topicId
  const assocId  = route.params.assocId
  if (topicmapId) {
    // console.log('### Initial navigation (topicmapId, topicId, assocId obtained from URL)', topicmapId, topicId,
    // assocId)
    urlPresent = true
  } else {
    topicmapId = dm5.utils.getCookie('dm4_topicmap_id')           // FIXME: convert to number?
    if (topicmapId) {
      // console.log('### Initial navigation (topicmap ID', topicmapId, 'obtained from cookie)')
    } else {
      // console.log('### Initial navigation (no topicmap cookie present)')
    }
  }
  // 2) select workspace
  // Note: at this stage a topicmap ID might or might not known. If *known* (either obtained from URL or from cookie)
  // the route is already up-to-date, no (further) push required. If *not* known the route still needs to be pushed.
  if (topicmapId) {
    getAssignedWorkspace(topicmapId).then(workspace => {
      // console.log('Topicmap', topicmapId, 'is assigned to workspace', workspace.id)
      store.dispatch('_selectWorkspace', workspace.id)            // no route push
      if (urlPresent) {
        const p = store.dispatch('displayTopicmap', topicmapId)   // no route push
        topicId && fetchTopic(topicId, p)                         // FIXME: 0 is a valid topic ID
        assocId && fetchAssoc(assocId, p)
      } else {
        store.dispatch('callTopicmapRoute', topicmapId)           // push initial route
      }
    })
  } else {
    store.dispatch('selectFirstWorkspace')                        // push initial route (indirectly)
  }
  // 3) setup detail panel
  const detail = route.params.detail
  if (detail) {
    store.dispatch('setDetailPanelVisibility', true)
    store.dispatch('selectDetail', detail)
  }
  // console.log('### Initial navigation complete!')
}

/**
 * Adapts app state when route changes.
 */
function navigate (to, from) {
  // console.log('navigate', to, from)
  var p     // a promise resolved once the topicmap rendering is complete
  // 1) topicmap
  const topicmapId = to.params.topicmapId
  // Note: path param values read from URL are strings. Path param values set by push() are numbers.
  // So we do *not* use exact equality (!==) here.
  if (topicmapId != from.params.topicmapId) {
    // Note: the workspace must be set *before* the topicmap is displayed.
    // See preconditions at "displayTopicmap".
    p = new Promise(resolve => {
      getAssignedWorkspace(topicmapId).then(workspace => {
        store.dispatch('_selectWorkspace', workspace.id)
        store.dispatch('displayTopicmap', topicmapId).then(resolve)
      })
    })
  } else {
    p = Promise.resolve()
  }
  // 2) selection
  const topicId = to.params.topicId
  const assocId = to.params.assocId
  const topicChanged = topicId != from.params.topicId
  const assocChanged = assocId != from.params.assocId
  if (topicChanged && topicId) {                                    // FIXME: 0 is a valid topic ID
    fetchTopic(topicId, p)
  }
  if (assocChanged && assocId) {
    fetchAssoc(assocId, p)
  }
  if ((topicChanged || assocChanged) && !topicId && !assocId) {     // FIXME: 0 is a valid topic ID
    unsetSelection(p)
  }
  // 3) detail
  const detail = to.params.detail
  if (detail != from.params.detail) {
    store.dispatch('setDetailPanelVisibility', detail !== undefined)
    if (detail) {
      store.dispatch('selectDetail', detail)
    }
  }
}

//

const getAssignedWorkspace = dm5.restClient.getAssignedWorkspace

//

/**
 * Fetches the given topic, displays it in the detail panel, and renders it as selected in the topicmap panel.
 *
 * @param   p   a promise resolved once the topicmap rendering is complete.
 */
function fetchTopic (id, p) {
  // console.log('requesting topic', id)
  // detail panel
  const p2 = dm5.restClient.getTopic(id, true, true).then(topic => {    // includeChilds=true, includeAssocChilds=true
    // console.log('topic', id, 'arrived')
    store.dispatch('displayObject', topic)
  })
  // topicmap panel
  p.then(() => {
    store.dispatch('setTopicSelection', {id, p: p2})
  })
}

/**
 * Fetches the given assoc, displays it in the detail panel, and renders it as selected in the topicmap panel.
 *
 * @param   p   a promise resolved once the topicmap rendering is complete.
 */
function fetchAssoc (id, p) {
  // detail panel
  const p2 = dm5.restClient.getAssoc(id, true, true).then(assoc => {    // includeChilds=true, includeAssocChilds=true
    store.dispatch('displayObject', assoc)
  })
  // topicmap panel
  p.then(() => {
    store.dispatch('setAssocSelection', {id, p: p2})
  })
}

function unsetSelection(p) {
  // detail panel
  store.dispatch('emptyDisplay')
  // topicmap panel
  p.then(() => {
    store.dispatch('unsetSelection')
  })
}
