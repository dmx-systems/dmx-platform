/**
 * The router.
 * - Initializes app state according to start URL.
 * - Adapts app state when URL changes.
 */

import Vue from 'vue'
import VueRouter from 'vue-router'
import { MessageBox } from 'element-ui'
import Webclient from './components/dmx-webclient'
import store from './store/webclient'
import dmx from 'dmx-api'

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

// use a global guard to perform dirty check
router.beforeEach((to, from, next) => {
  // Perform a dirty check if all conditions apply:
  //   - the detail panel is visible (in-map details are NOT dirty checked; TODO?)
  //   - there is a selection (the detail panel is not empty)
  //   - in the route there is a topicmap change or a selection change (or both)
  // Note: at router instantiation time the details plugin is not yet initialized
  // TODO: rethink router instantiation time
  if (store.state.details && store.state.details.visible && store.state.object &&
      (topicmapId(to) !== topicmapId(from) || objectId(to) !== objectId(from))) {
    const detailPanel = document.querySelector('.dm5-detail-panel').__vue__
    const isDirty = detailPanel.isDirty()
    // console.log('isDirty', isDirty, store.state.object.id)
    if (isDirty) {
      MessageBox.confirm('There are unsaved changes', 'Warning', {
        type: 'warning',
        confirmButtonText: 'Save',
        cancelButtonText: 'Discard Changes',
        distinguishCancelAndClose: true
      }).then(action => {   // -> Save
        detailPanel.save()
        next()
      }).catch(action => {
        switch (action) {
        case 'cancel':      // -> Discard Changes
          next()
          break;
        case 'close':       // -> Abort Navigation
          restoreSelection(from)
          next(false)
          break;
        default:
          throw Error(`unexpected MessageBox action: "${action}"`)
        }
      })
    } else {
      next()
    }
  } else {
    next()
  }
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
      callObjectRoute(id, 'topicId', 'topic', 'topicDetail')
    },

    callAssocRoute (_, id) {
      callObjectRoute(id, 'assocId', 'assoc', 'assocDetail')
    },

    stripSelectionFromRoute () {
      router.push({
        name: 'topicmap'
      })
    },

    /**
     * Redirects to "topicDetail" or "assocDetail" route, depending on current selection.
     *
     * Prerequisite: a single selection
     *
     * @param   detail    "info", "related", "meta", "view" or "edit"
     */
    callDetailRoute ({rootState}, detail) {
      const object = rootState.object
      if (!object) {
        throw Error('callDetailRoute() when there is no single selection')
      }
      router.push({
        name: object.isTopic ? 'topicDetail' : 'assocDetail',
        params: {detail}
      })
    },

    /**
     * Redirects to "topicDetail" route.
     *
     * @param   id        a topic ID
     * @param   detail    "info", "related", "meta", "view" or "edit"
     */
    callTopicDetailRoute (_, {id, detail}) {
      router.push({
        name: 'topicDetail',
        params: {topicId: id, detail}
      })
    },

    /**
     * Redirects to "assocDetail" route.
     *
     * @param   id        an assoc ID
     * @param   detail    "info", "related", "meta", "view" or "edit"
     */
    callAssocDetailRoute (_, {id, detail}) {
      router.push({
        name: 'assocDetail',
        params: {assocId: id, detail}
      })
    },

    stripDetailFromRoute ({rootState}) {
      const object = rootState.object
      if (!object) {
        throw Error('stripDetailFromRoute() when there is no single selection')
      }
      router.push({
        name: object.isTopic ? 'topic' : 'assoc'
      })
    }
  }
})

/**
 * Redirects to "topic"/"assoc" route while retaining the selected detail tab (if any).
 *
 * Falls back to "info" tab in 2 cases: when we're coming from ...
 *   - form mode (we don't want stay in form mode when user selects another topic/assoc)
 *   - empty (pinned) detail panel (we don't want restore the tab that was selected before emptying the panel)
 */
function callObjectRoute (id, paramName, routeName, detailRouteName) {
  const location = {
    params: {[paramName]: id}
  }
  // Note: normally the route is the source of truth. But there is an app state not represented in the route: an empty
  // (pinned) detail panel. We detect that by inspecting both, the "details.visible" app state AND the route's "detail"
  // segment.
  if (store.state.details.visible) {
    location.name = detailRouteName
    // fallback to "info" tab
    // Note: when the detail panel is empty there is no "detail" route segment
    const detail = router.currentRoute.params.detail
    if (!detail || detail === 'edit') {
      location.params.detail = 'info'
    }
  } else {
    location.name = routeName
  }
  router.push(location)
}

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
    topicmapId = id(dmx.utils.getCookie('dmx_topicmap_id'))
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
      return dmx.rpc.getTopic(topicmapId)
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
  // 2) topic/assoc selection
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
  // 3) detail panel
  const detail = to.params.detail
  const oldDetail = from.params.detail
  if (detail != oldDetail) {
    store.dispatch('setDetailPanelVisibility', detail !== undefined || store.state.details.pinned)
    if (detail) {
      store.dispatch('selectDetail', detail)
      if (!oldDetail && oldId === newId) {
        store.dispatch('_removeDetail')
      }
    }
  }
}

const getAssignedWorkspace = dmx.rpc.getAssignedWorkspace

/**
 * Fetches the given topic, displays it in the detail panel, and renders it as selected in the topicmap panel.
 *
 * @param   p   a promise resolved once the topicmap rendering is complete.
 */
function fetchTopic (id, p) {
  // console.log('requesting topic', id)
  // detail panel
  const p2 = dmx.rpc.getTopic(id, true, true).then(topic => {  // includeChildren=true, includeAssocChildren=true
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
  const p2 = dmx.rpc.getAssoc(id, true, true).then(assoc => {  // includeChildren=true, includeAssocChildren=true
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

const SETTER = {
  topicDetail: 'setTopic',
  assocDetail: 'setAssoc'
}

/**
 * Restores the selection according to the given route
 */
function restoreSelection (route) {
  const setter = SETTER[route.name]
  if (!setter) {
    throw Error(`unexpected route: "${route.name}"`)
  }
  const id = objectId(route)
  const selection = store.getters.selection
  // update view
  selection.forEachId(id => {
    store.dispatch('_renderAsUnselected', id)
  })
  store.dispatch('_renderAsSelected', id)
  // update model
  selection[setter](id)
}

/**
 * @return  an ID (type Number) or undefined
 */
function objectId(route) {
  return id(route.params.assocId || route.params.topicId)     // Note: 0 is a valid topic ID; check assoc ID first
}

function topicmapId(route) {
  return id(route.params.topicmapId)
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
  // Note: dmx.utils.getCookie may return null, and Number(null) is 0 (and typeof null is 'object')
  if (typeof v === 'number') {
    return v
  } else if (typeof v === 'string') {
    return Number(v)
  } else if (v !== undefined && v !== null) {
    throw Error(`id() expects one of [number|string|undefined|null], got ${v}`)
  }
}
