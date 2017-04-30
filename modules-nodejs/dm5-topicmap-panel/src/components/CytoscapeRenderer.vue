<template>
  <div class="cytoscape-renderer" ref="container"></div>
</template>

<script>
import cytoscape from 'cytoscape'

var cy

export default {

  props: ['topicmap'],

  watch: {
    topicmap: function() {
      this.refresh()
    }
  },

  mounted () {
    cy = cytoscape({
      container: this.$refs.container,
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
    //
    cy.on('select', 'node', e => {
      this.$store.dispatch('selectTopic', e.target.id())
    })
    cy.on('select', 'edge', e => {
      this.$store.dispatch('selectAssoc', e.target.id())
    })
    cy.on('tapstart', 'node', e => {
      var node = e.target
      var drag = false
      node.one('tapdrag', e => {
        drag = true
      })
      cy.one('tapend', e => {
        if (drag) {
          this.$store.dispatch('setTopicPosition', {
            id: node.id(),
            pos: node.position()
          })
        }
      })
    })
  },

  methods: {
    refresh () {
      cy.add(this.topicmap.topics.map(topic => ({
        data: {
          id: topic.id,
          label: topic.value
        },
        position: {
          x: topic.view_props['dm4.topicmaps.x'],
          y: topic.view_props['dm4.topicmaps.y']
        }
      })))
      cy.add(this.topicmap.assocs.map(assoc => ({
        data: {
          id: assoc.id,
          label: assoc.value,
          source: assoc.role_1.topic_id,
          target: assoc.role_2.topic_id
        }
      })))
    }
  }
}
</script>

<style>
.cytoscape-renderer {
  flex: auto;
  overflow: hidden;
}
</style>
