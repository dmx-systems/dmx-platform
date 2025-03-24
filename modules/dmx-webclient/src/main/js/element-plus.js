import { ElMessageBox, ElNotification } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import 'element-plus/theme-chalk/el-message-box.css'
import 'element-plus/theme-chalk/el-notification.css'
import app from './app'
// import DialogDraggable from 'vue-element-dialog-draggable'       // TODO?

export default () => undefined    // import('./element-plus-ext')   // TODO

// set locale       // TODO?
// import locale from 'element-plus/lib/locale'
// locale.use(require('element-plus/lib/locale/lang/en').default)

// global config    // TODO?
// Vue.prototype.$ELEMENT = {
//   size: 'mini'
// }

// register app assets
app.use(ElMessageBox)
app.use(ElNotification)

app.component('ArrowDown', ArrowDown)

// Vue.use(Loading.directive)   // TODO?
// Vue.use(DialogDraggable)     // TODO?
