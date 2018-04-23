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
        throw Error('callDetailRoute() called when nothing is selected')
      }
      router.push({
        name: object.isTopic() ? 'topicDetail' : 'assocDetail',
        params: {detail}
      })
    },

    callTopicDetailRoute (_, {id, detail}) {
      router.push({
        name: 'topicDetail',
        params: {topicId: id, detail}
      })
    },

    callAssocDetailRoute (_, {id, detail}) {
      router.push({
        name: 'assocDetail',
        params: {assocId: id, detail}
      })
    },

    stripDetailFromRoute () {
      const object = store.state.object
      if (!object) {
        throw Error('stripDetailFromRoute when nothing is selected')
      }
      router.push({
        name: object.isTopic() ? 'topic' : 'assoc'
      })
    }
  }
})

// TODO: why does the watcher kick in when an initial URL is present?
// Since when is it this way?
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
  // Note: route params read from URL are strings (may be undefined). Route params set by push() are numbers.
  let topicmapId = id(route.params.topicmapId)
  const topicId  = id(route.params.topicId)
  const assocId  = id(route.params.assocId)
  if (topicmapId) {
    // console.log('### Initial navigation (topicmapId, topicId, assocId obtained from URL)', topicmapId, topicId,
    // assocId)
    urlPresent = true
  } else {
    topicmapId = id(dm5.utils.getCookie('dm4_topicmap_id'))
    if (topicmapId) {
      // console.log('### Initial navigation (topicmap ID', topicmapId, 'obtained from cookie)')
    } else {
      // console.log('### Initial navigation (no topicmap cookie present)')
    }
  }
  // 2) select workspace
  // Note: at this stage a topicmap ID might be available or not. If available it is either obtained from URL or from
  // cookie. If obtained from URL the route is already up-to-date, no (further) route push is required. On the other
  // hand, if obtained from cookie or if no topicmapId is available, an initial route still needs to be pushed.
  if (topicmapId) {
    getAssignedWorkspace(topicmapId).then(workspace => {
      // console.log('Topicmap', topicmapId, 'is assigned to workspace', workspace.id)
      const p1 = store.dispatch('_selectWorkspace', workspace.id)                 // no route push
      // p1 is a promise resolved once the workspace's topicmap topics are available
      if (urlPresent) {
        const p2 = p1.then(() => store.dispatch('displayTopicmap', topicmapId))   // no route push
        topicId && fetchTopic(topicId, p2)                                        // FIXME: 0 is a valid topic ID
        assocId && fetchAssoc(assocId, p2)
      } else {
        store.dispatch('callTopicmapRoute', topicmapId)                           // push initial route
      }
    })
  } else {
    store.dispatch('selectFirstWorkspace')                                        // push initial route (indirectly)
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
  const topicmapId = id(to.params.topicmapId)
  const topicmapChanged = topicmapId !== id(from.params.topicmapId)
  // Note: route params read from URL are strings. Route params set by push() are numbers.
  if (topicmapChanged) {
    // Note: the workspace must be set *before* the topicmap is displayed.
    // See preconditions at "displayTopicmap".
    p = new Promise(resolve => {
      getAssignedWorkspace(topicmapId)
        .then(workspace => store.dispatch('_selectWorkspace', workspace.id))
        .then(() => store.dispatch('displayTopicmap', topicmapId))
        .then(resolve)
    })
  } else {
    p = Promise.resolve()
  }
  // 2) selection
  const topicId = id(to.params.topicId)
  const assocId = id(to.params.assocId)
  const oldTopicId = id(from.params.topicId)
  const oldAssocId = id(from.params.assocId)
  const oldId = oldTopicId || oldAssocId
  const topicChanged = topicId !== oldTopicId
  const assocChanged = assocId !== oldAssocId
  if (topicChanged && topicId) {                                    // FIXME: 0 is a valid topic ID
    fetchTopic(topicId, p)
  }
  if (assocChanged && assocId) {
    fetchAssoc(assocId, p)
  }
  if ((topicChanged || assocChanged) && !topicId && !assocId) {     // FIXME: 0 is a valid topic ID
    // detail panel
    store.dispatch('emptyDisplay')
    // topicmap panel
    if (!topicmapChanged) {
      p.then(() => store.dispatch('unsetSelection', oldId))
    }
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

const getAssignedWorkspace = dm5.restClient.getAssignedWorkspace

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
    // Note: the topicmap panel manually syncs the selected object with the topicmap renderer.
    // The "object" state must not be set before a topicmap renderer is instantiated.
    p.then(() => {
      store.dispatch('displayObject', topic)
    })
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
    // Note: the topicmap panel manually syncs the selected object with the topicmap renderer.
    // The "object" state must not be set before a topicmap renderer is instantiated.
    p.then(() => {
      store.dispatch('displayObject', assoc)
    })
  })
  // topicmap panel
  p.then(() => {
    store.dispatch('setAssocSelection', {id, p: p2})
  })
}

/**
 * Converts the given value into Number.
 *
 * @return  the number, or undefined if `undefined` or `null` is given.
 *          Never returns `null`.
 *
 * @throws  if the given value is not one of Number/String/undefined/null.
 */
function id (v) {
  // Note: Number(undefined) is NaN, and NaN != NaN is true!
  // Note: dm5.utils.getCookie may return null, and Number(null) is 0 (and typeof null is 'object')
  if (typeof v === 'number') {
    return v
  } else if (typeof v === 'string') {
    return Number(v)
  } else if (v !== undefined && v !== null) {
    throw Error(`id() expects one of [number|string|undefined|null], but got ${v}`)
  }
}
