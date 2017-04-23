<template>
  <div id="topicmap">HAHA</div>
</template>

<script>
import cytoscape from 'cytoscape'

export default {
  mounted () {
    var cy = cytoscape({
      container: document.getElementById('topicmap'),
      style: [
        {
          selector: 'node',
          style: {
            'shape': 'rectangle',
            'width': 'label',
            'height': 'label',
            'label': 'data(id)',
            'text-valign': 'center'
          }
        },
        {
          selector: 'edge',
          style: {
            'width': 5,
            'curve-style': 'bezier',
            'target-arrow-shape': 'triangle',
            'label': 'data(id)',
            'text-rotation': 'autorotate'
          }
        }
      ],
      layout: {
        name: 'preset'
      }
    })
  }
}
</script>

<style>
#topicmap {
  height: 100%;
}
</style>
