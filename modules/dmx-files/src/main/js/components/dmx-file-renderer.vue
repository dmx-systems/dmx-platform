<template>
  <div class="dmx-file-renderer">
    <embed v-if="isPDF" :src="fileUrl" :type="mediaType" width="100%" height="450"></embed>
    <dmx-value-renderer :object="object" :level="0" :path="[]" :context="context" :no-heading="true">
    </dmx-value-renderer>
  </div>
</template>

<script>
export default {

  created () {
    // console.log('dmx-file-renderer created', this.object)
  },

  mixins: [
    require('./mixins/object').default,
    require('./mixins/context').default
  ],

  computed: {

    path () {
      return this.object.children['dmx.files.path'].value
    },

    mediaType () {
      return this.object.children['dmx.files.media_type'].value
    },

    fileUrl () {
      return '/filerepo/' + encodeURIComponent(this.path)
    },

    isPDF () {
      return this.mediaType === 'application/pdf'
    }
  }
}
</script>

<style>
</style>
