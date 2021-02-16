<template>
  <div class="dmx-folder-renderer">
    <dmx-value-renderer :object="object" :level="0" :path="[]" :context="context" :no-heading="true">
    </dmx-value-renderer>
    <ul class="listing">
      <li v-for="(item, i) in items">
        <el-button type="text" @click="reveal(item)">
          <span :class="['fa', 'fa-fw', icons[i]]"></span> {{item.name}}
        </el-button>
      </li>
    </ul>
  </div>
</template>

<script>
const ICONS = {
  file: 'fa-file-o',
  directory: 'fa-folder-o'
}

export default {

  created () {
    console.log('dmx-folder-renderer created', this.object)
    this.initListing()
  },

  mixins: [
    require('./mixins/object').default,
    require('./mixins/context').default
  ],

  data () {
    return {
      items: []
    }
  },

  computed: {

    path () {
      return this.object.children['dmx.files.path'].value
    },

    icons () {
      return this.items.map(item => ICONS[item.kind])
    }
  },

  methods: {

    initListing () {
      this.$store.dispatch('getDirectoryListing', this.path)
        .then(listing => {
          console.log(listing)
          this.items = listing.items
        })
    },

    reveal (item) {
      console.log(item)
    }
  }
}
</script>

<style>
.dmx-folder-renderer ul.listing {
  list-style-type: none;
  padding-left: 0;
}

.dmx-folder-renderer ul.listing li button {
  padding-left: 0 !important;
  padding-right: 0 !important;
}
</style>