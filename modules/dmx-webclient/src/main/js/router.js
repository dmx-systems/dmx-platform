/**
 * The router.
 * - Initializes app state according to start URL.
 * - Adapts app state when URL changes.
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
        throw Error('callDetailRoute() when nothing is selected')
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
        throw Error('stripDetailFromRoute() when nothing is selected')
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
    topicmapId = id(dm5.utils.getCookie('dmx_topicmap_id'))
    if (topicmapId) {
      // console.log('### Initial navigation (topicmap ID', topicmapId, 'obtained from cookie)')
    } else {
      // console.log('### Initial navigation (no topicmap cookie present)')
    }
  }
  // topicmap validity check
  let workspaceId   // valid only if topicmapId is defined after validity check
  let p             // a promise resolved once validity check is complete
  if (topicmapId) {
    // console.log(`Checking workspace of topicmap ${topicmapId}`)
    // Note: get-assigned-workspace responses are not cached by the browser.
    // In contrast get-topic responses *are* cached by the browser.
    // Doing get-assigned-workspace first avoids working with stale data.
    p = getAssignedWorkspace(topicmapId).then(workspace => {
      // console.log('Workspace retrieved', workspace)
      workspaceId = workspace.id
      // console.log(`Retrieving topic ${topicmapId}`)
      return dm5.restClient.getTopic(topicmapId)
    }).then(topic => {
      // console.log('Topic retrieved', topic)
      if (topic.typeUri !== "dmx.topicmaps.topicmap") {
        throw Error(`${topicmapId} is not a topicmap (but a ${topic.typeUri})`)
      }
    }).catch(error => {
      console.warn(`Topicmap ${topicmapId} check failed`, error)
      topicmapId = undefined
    })
  } else {
    p = Promise.resolve()
  }
  // 2) select workspace
  // Note: at this stage a topicmap ID might be available or not. If available it is either obtained from URL or from
  // cookie. If obtained from URL the route is already up-to-date, no (further) route push is required.
  // If obtained from cookie or if no topicmapId is available, an initial route still needs to be pushed.
  p.then(() => {
    // console.log('[DMX] Initial topicmap/workspace', topicmapId, workspaceId)
    if (topicmapId) {
      store.dispatch('_selectWorkspace', workspaceId).then(() => {    // no route push
        // the workspace's topicmap topics are now available
        if (urlPresent) {
          // Note: 'displayTopicmap' relies on the topicmap topics in order to tell what topicmap renderer to use
          const p = store.dispatch('displayTopicmap', topicmapId)     // no route push
          topicId !== undefined && fetchTopic(topicId, p)             // Note: 0 is a valid topic ID
          assocId               && fetchAssoc(assocId, p)
        } else {
          // Note: when the topicmap changes '_selectWorkspace' is dispatched again (see navigate() below).
          // Calling the topicmap route only when the topicmap topics are available avoids loading them twice.
          // TODO: avoid dispatching '_selectWorkspace' twice in the first place?
          store.dispatch('callTopicmapRoute', topicmapId)             // push initial route
        }
      })
    } else {
      store.dispatch('selectFirstWorkspace')                          // push initial route (indirectly)
    }
  })
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
  const oldId = oldAssocId || oldTopicId        // Note: oldAssocId is checked first as oldId must be a number
  const newId = assocId    || topicId           // Note: assocId    is checked first as newId must be a number
  const topicChanged = topicId !== oldTopicId
  const assocChanged = assocId !== oldAssocId
  if ((topicChanged || topicmapChanged) && topicId !== undefined) {             // Note: 0 is a valid topic ID
    fetchTopic(topicId, p)
  }
  if ((assocChanged || topicmapChanged) && assocId) {
    fetchAssoc(assocId, p)
  }
  if ((topicChanged || assocChanged) && topicId === undefined && !assocId) {    // Note: 0 is a valid topic ID
    // detail panel
    store.dispatch('emptyDisplay')
    // topicmap panel
    if (!topicmapChanged) {
      p.then(() => store.dispatch('unsetSelection', oldId))
    }
  }
  // 3) detail
  const detail    = to.params.detail
  const oldDetail = from.params.detail
  if (detail != oldDetail) {
    store.dispatch('setDetailPanelVisibility', detail !== undefined)
    if (detail) {
      store.dispatch('selectDetail', detail)
      if (!oldDetail && oldId === newId) {
        store.dispatch('_removeDetail')
      }
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
  }).catch(error => {
    // FIXME: do not just report the crash! Instead return the promise and attach the error handler at a higher level.
    // If the topicmap panel fails to render the topic the detail panel is supposed to stay empty.
    console.error(`Rendering topic ${id} as selected failed`, error)
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
  }).catch(error => {
    // FIXME: do not just report the crash! Instead return the promise and attach the error handler at a higher level.
    // If the topicmap panel fails to render the assoc the detail panel is supposed to stay empty.
    console.error(`Rendering assoc ${id} as selected failed`, error)
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
    throw Error(`id() expects one of [number|string|undefined|null], got ${v}`)
  }
}
