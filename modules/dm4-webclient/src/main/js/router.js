/**
 * Adapts app state to route changes.
 * Creates the initial app state, based on start URL.
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

    callTopicRoute (_, id) {
      router.push({
        name: 'topic',
        params: {topicId: id}
      })
    },

    callAssocRoute (_, id) {
      router.push({
        name: 'assoc',
        params: {assocId: id}
      })
    },

    stripSelectionFromRoute () {
      router.push({
        name: 'topicmap'
      })
    },

    // TODO: needed?
    callTopicDetailRoute (_, {id, detail}) {
      router.push({
        name: 'topicDetail',
        params: {topicId: id, detail}
      })
    },

    // TODO: needed?
    callAssocDetailRoute (_, {id, detail}) {
      router.push({
        name: 'assocDetail',
        params: {assocId: id, detail}
      })
    },

    callDetailRoute (_, detail) {
      const object = store.state.object
      if (!object) {
        throw 'callDetailRoute when no object is selected'
      }
      router.push({
        name: object.isTopic() ? 'topicDetail' : 'assocDetail',
        params: {detail}
      })
    },

    stripDetailFromRoute () {
      const object = store.state.object
      if (!object) {
        throw 'stripDetailFromRoute when no object is selected'
      }
      router.push({
        name: object.isTopic() ? 'topic' : 'assoc'
      })
    }
  }
})

function registerWatcher () {
  store.watch(
    state => state.routerModule.router.currentRoute,
    (to, from) => {
      // console.log('### Route watcher', to, from)
      navigate(to, from)
    }
  )
}

/**
 * Selects the intial topicmap and workspace, and pushes the initial route if needed.
 */
function initialNavigation (route) {
  //
  registerWatcher()
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
  // console.log('### Initial navigation complete!')
}

function navigate (to, from) {
  // topicmap
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
        store.dispatch('_selectWorkspace', workspace.id)
        store.dispatch('displayTopicmap', topicmapId).then(resolve)
      })
    })
  } else {
    p = Promise.resolve()
  }
  // selection
  const topicId = to.params.topicId
  const oldTopicId = from.params.topicId
  if (topicId != oldTopicId) {
    if (topicId) {                                // FIXME: 0 is a valid topic ID
      fetchTopic(topicId, p)
    }
  }
  const assocId = to.params.assocId
  const oldAssocId = from.params.assocId
  if (assocId != oldAssocId) {
    if (assocId) {                                // FIXME: 0 is a valid topic ID
      fetchAssoc(assocId, p)
    }
  }
  const topicCleared = oldTopicId && !topicId     // FIXME: 0 is a valid topic ID
  const assocCleared = oldAssocId && !assocId     // FIXME: 0 is a valid topic ID
  if (topicCleared || assocCleared) {
    unsetSelection(p)
  }
  // detail
  const detail = to.params.detail
  const oldDetail = from.params.detail
  if (detail != oldDetail) {
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
  // includeChilds=true, includeAssocChilds=true
  const p2 = dm5.restClient.getTopic(id, true, true).then(topic => {
    // console.log('topic', id, 'arrived')
    store.dispatch('displayObject', topic)            // detail panel
  })
  p.then(() => {
    store.dispatch('setTopicSelection', {id, p: p2})  // topicmap panel
  })
}

/**
 * Fetches the given assoc, displays it in the detail panel, and renders it as selected in the topicmap panel.
 *
 * @param   p   a promise resolved once the topicmap rendering is complete.
 */
function fetchAssoc (id, p) {
  // includeChilds=true, includeAssocChilds=true
  const p2 = dm5.restClient.getAssocWithPlayers(id, true, true).then(assoc => {
    store.dispatch('displayObject', assoc)            // detail panel
  })
  p.then(() => {
    store.dispatch('setAssocSelection', {id, p: p2})  // topicmap panel
  })
}

function unsetSelection(p) {
  store.dispatch('emptyDisplay')                      // detail panel
  p.then(() => {
    store.dispatch('unsetSelection')                  // topicmap panel
  })
}
