<template>
  <el-dialog title="About DMX" custom-class="dmx-about-box" :visible="visible" :modal="false" v-draggable
      @close="close">
    <img src="../dmx-logo.svg">
    <div>
      <div class="field">
        <div class="field-label">Version</div>
        5.2-SNAPSHOT
      </div>
      <div class="field">
        <div class="field-label">Released</div>
        Apr 8, 2021
      </div>
      <div class="field">
        <div class="field-label">License</div>
        <el-link href="https://www.gnu.org/licenses/agpl-3.0.html" type="primary" :underline="false" target="agpl">
          AGPL-3.0
        </el-link>
      </div>
      <div class="field">
        <div class="field-label">Copyright</div>
        2021&nbsp;&nbsp;<el-link href="https://dmx.systems" type="primary" :underline="false" target="dmx">
          DMX Systems
        </el-link>
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
.dmx-about-box .el-dialog__body {
  display: flex;
  align-items: center;
  padding: 10px 20px 30px 20px !important;    /* Element UI default is 30px 20px (was overrideen in App.vue) */
}

.dmx-about-box img {
  width: 50%;
  margin-right: 10%;
}

.dmx-about-box .el-link {
  vertical-align: unset;      /* Element UI default is "middle" */
}

.dmx-about-box .field + .field {
  margin-top: var(--field-spacing);
}
</style>
