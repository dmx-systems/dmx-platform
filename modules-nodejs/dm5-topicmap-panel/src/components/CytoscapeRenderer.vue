<template>
  <div class="cytoscape-renderer" ref="container"></div>
</template>

<script>
import cytoscape from 'cytoscape'

export default {

  props: ['topicmap'],

  data: () => ({
    cy: undefined
  }),

  watch: {
    topicmap: function() {
      this.refresh()
    }
  },

  mounted () {
    this.cy = cytoscape({
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
      }
    })
    //
    this.cy.on('select', 'node', event => {
      this.$store.dispatch('selectTopic', event.target.id())
    })
    this.cy.on('select', 'edge', event => {
      this.$store.dispatch('selectAssoc', event.target.id())
    })
  },

  methods: {
    refresh () {
      this.cy.add(this.topicmap.topics.map(topic => ({
        data: {
          id: topic.id,
          label: topic.value
        },
        position: {
          x: topic.view_props['dm4.topicmaps.x'],
          y: topic.view_props['dm4.topicmaps.y']
        }
      })))
      this.cy.add(this.topicmap.assocs.map(assoc => ({
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
