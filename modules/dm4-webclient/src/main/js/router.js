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
  // Note: route params read from URL are strings. Route params set by push() are numbers.
  if (topicmapId !== id(from.params.topicmapId)) {
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
  const topicChanged = topicId !== id(from.params.topicId)
  const assocChanged = assocId !== id(from.params.assocId)
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
    // Note: the topicmap panel manually syncs the selected object with the topicmap renderer.
    // The "object" state must not be set before a topicmap renderer is instantiated.
    p.then(() => {
      store.dispatch('displayObject', topic)
    })
  })
  // topicmap panel
  p.then(() => {
    store.dispatch('setTopicSelection', {id, p: p2})
    _setTopicSelection(id)
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
    _setAssocSelection(id)
  })
}

/**
 * @param   p   a promise resolved once the topicmap rendering is complete.
 */
function unsetSelection (p) {
  const id = store.state.object.id      // remember id as 'emptyDisplay' action resets "object" state
  console.log('unsetSelection', id, store.state.selection.isMulti())
  // detail panel
  store.dispatch('emptyDisplay')
  // topicmap panel
  p.then(() => {
    store.dispatch('unsetSelection')
    _unsetSelection(id)
  })
}

// Multi-selection handling: update "selection" state and sync view

// Note: by design multi-selection behave different than single selections:
// - multi selections are not represented in the browser URL.
// - the object details of a multi selection are *not* displayed in-map (unless pinned).

function _setTopicSelection (id) {
  _setSelection()
  store.state.selection.setTopic(id)
}

function _setAssocSelection (id) {
  _setSelection()
  store.state.selection.setAssoc(id)
}

function _setSelection () {
  console.log('_setSelection', store.state.selection.topicIds, store.state.selection.assocIds)
  // If there is a multi selection and history navigation leads to a single-selection route, the multi selection must
  // be visually removed and the "selection" state must be updated manuallly (see subsequent call). In contrast when
  // changing the selection by topicmap interaction the "selection" state and view are up-to-date already.
  if (store.state.selection.isMulti()) {
    store.state.selection.forEachId(id => {
      store.dispatch('_syncUnselect', id)     // TODO: pinned multi selection?
    })
  }
}

function _unsetSelection (id) {
  console.log('_unsetSelection', store.state.selection.topicIds, store.state.selection.assocIds)
  if (store.state.selection.isSingle()) {
    // If there is a single selection and history navigation leads to a selection-less route, the "selection" state
    // must be emptied manually. In contrast when removing the selection by topicmap interaction the "selection" state
    // is up-to-date already.
    store.state.selection.empty()
  } else if (store.state.selection.isMulti()) {
    // If a single selection is extended to a multi selection the URL's selection part is stripped, causing the router
    // to remove the single selection from state and view. The former single selection must be visually restored in
    // order to match the multi selection state. This is done by dispatching the low-level '_syncSelect' action, which
    // manipulates the view only. The normal 'syncSelect' action would display the in-map details.
    store.dispatch('_syncSelect', id)
  }
}

//

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
