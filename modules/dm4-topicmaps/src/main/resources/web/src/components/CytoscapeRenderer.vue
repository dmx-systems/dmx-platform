<template>
  <div class="cytoscape-renderer" ref="container"></div>
</template>

<script>
import cytoscape from 'cytoscape'

export default {
  data: () => ({
    cy: undefined
  }),

  mounted () {
    console.log('CytoscapeRenderer created!', this.$refs.container)
    this.cy = cytoscape({
      container: this.$refs.container,
      elements: [
        {data: {id: 'n1', label: 'Click Me!'}, position: {x: 100, y: 40}}
      ],
      style: [
        {
          selector: 'node',
          style: {
            'shape': 'rectangle',
            'background-color': 'hsl(210, 100%, 90%)',
            'padding': '2px',
            'width': 'label',
            'height': 'label',
            'label': 'data(label)',
            'text-valign': 'center'
          }
        },
        {
          selector: 'edge',
          style: {
            'width': 4,
            'line-color': 'rgb(178, 178, 178)',
            'curve-style': 'bezier',
            'label': 'data(label)',
            'text-rotation': 'autorotate'
          }
        },
        {
          selector: 'node:selected',
          style: {
            'border-width': 3,
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
    this.$store.watch(
      state => state.topicmaps.topicmap,
      topicmap => this.setTopicmapData(topicmap)
    )
  },

  methods: {
    setTopicmapData (topicmap) {
      console.log('setTopicmapData', topicmap)
      // topics
      var topics = topicmap.topics.map(topic => ({
        data: {
          id: topic.id,
          label: topic.value
        },
        position: {
          x: topic.view_props['dm4.topicmaps.x'],
          y: topic.view_props['dm4.topicmaps.y']
        }
      }))
      this.cy.add(topics)
      // assocs
      var assocs = topicmap.assocs.map(assoc => ({
        data: {
          id: assoc.id,
          label: assoc.value,
          source: assoc.role_1.topic_id,
          target: assoc.role_2.topic_id
        }
      }))
      this.cy.add(assocs)
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
