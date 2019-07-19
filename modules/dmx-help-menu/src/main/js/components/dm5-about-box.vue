<template>
  <el-dialog custom-class="dm5-about-box" :visible="visible" @close="close">
    <img src="../dmx-logo.svg">
    <div>
      <div class="field">
        <div class="field-label">Version</div>
        5.0-beta-4-SNAPSHOT
      </div>
      <div class="field">
        <div class="field-label">Released</div>
        July 19, 2019
      </div>
      <div class="field">
        <div class="field-label">License</div>
        GPL3 or later
      </div>
      <div class="field">
        <div class="field-label">Copyright</div>
        2019<el-link href="https://dmx.systems" type="primary" target="_blank">DMX Systems</el-link>
      </div>
    </div>
  </el-dialog>
</template>

<script>
export default {

  computed: {
    visible () {
      return this.$store.state.helpmenu.aboutBoxVisibility
    }
  },

  methods: {
    close () {
      this.$store.dispatch('closeAboutBox')
    }
  }
}
</script>

<style>
.dm5-about-box .el-dialog__body {
  display: flex;
  align-items: center;
  padding: 10px 20px 30px 20px !important;    /* Element UI default is 30px 20px (was overrideen in App.vue) */
}

.dm5-about-box img {
  width: 50%;
  margin-right: 10%;
}

.dm5-about-box .el-link {
  vertical-align: unset;      /* Element UI default is "middle" */
  margin-left: 0.6em;
}

.dm5-about-box .field + .field {
  margin-top: var(--field-spacing);
}
</style>
