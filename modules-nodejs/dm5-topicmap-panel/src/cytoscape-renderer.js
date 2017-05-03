import cytoscape from 'cytoscape'

// Note: the topicmap is not vuex state. (This store module provides no state at all, only actions.)
// In conjunction with Cytoscape the topicmap is not considered reactive data.
// We have to bind topicmap data to the Cytoscape graph model manually anyways.
// (This is because Cytoscape deploys a canvas, not a DOM).

var topicmap            // topicmap to render (a Topicmap object)

var cy = initialize()   // the Cytoscape instance
var events = false      // tracks Cytoscape event listener registration, which is lazy

const actions = {

  setTopicmap ({dispatch}, _topicmap) {
    topicmap = _topicmap
    eventListeners(dispatch)
    refresh()
  },

  // WebSocket messages

  _setTopicPosition ({rootState}, {topicmapId, topicId, pos}) {
    if (topicmapId === topicmap.id) {
      cy.getElementById(topicId).position(pos)
    }
  }
}

export default {
  actions
}

// Cytoscape

function initialize() {
  return cytoscape({
    container: document.getElementById('cytoscape-renderer'),
    style: [
      {
        selector: 'node',
        style: {
          'shape': 'rectangle',
          'background-color': 'hsl(210, 100%, 90%)',
          'padding': '3px',
          'width': 'label',
          'height': 'label',
          'label': 'data(label)',
          'text-valign': 'center'
        }
      },
      {
        selector: 'edge',
        style: {
          'width': 3,
          'line-color': 'rgb(178, 178, 178)',
          'curve-style': 'bezier',
          'label': 'data(label)',
          'text-rotation': 'autorotate'
        }
      },
      {
        selector: 'node:selected',
        style: {
          'border-width': 2,
          'border-color': 'red'
        }
      },
      {
        selector: 'edge:selected',
        style: {
          'line-color': 'red'
        }
      }
    ],
    layout: {
      name: 'preset'
    },
    wheelSensitivity: 0.2
  })
}

// lazy registration of Cytoscape event listeners
function eventListeners(dispatch) {
  if (!events) {
    cy.on('select', 'node', e => {
      dispatch('selectTopic', e.target.id())
    })
    cy.on('select', 'edge', e => {
      dispatch('selectAssoc', e.target.id())
    })
    cy.on('tapstart', 'node', e => {
      var node = e.target
      var drag = false
      node.one('tapdrag', e => {
        drag = true
      })
      cy.one('tapend', e => {
        if (drag) {
          dispatch('setTopicPosition', {
            id: node.id(),
            pos: node.position()
          })
        }
      })
    })
    events = true
  }
}

function refresh () {
  console.log('refresh')
  var elems = []
  topicmap.forEachTopic(topic => {
    elems.push({
      data: {
        id:    topic.id,
        label: topic.value
      },
      position: topic.getPosition()
    })
  })
  topicmap.forEachAssoc(assoc => {
    elems.push({
      data: {
        id:     assoc.id,
        label:  assoc.value,
        source: assoc.role1.topic_id,
        target: assoc.role2.topic_id
      }
    })
  })
  cy.add(elems)
}
