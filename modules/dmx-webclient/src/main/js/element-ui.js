import Vue from 'vue'
import {
  Button, Input, Select, Option, OptionGroup, Checkbox, Radio, RadioGroup, Dialog, Tabs, TabPane, ColorPicker,
  Loading, Notification
} from 'element-ui'
import locale from 'element-ui/lib/locale'

// set locale
locale.use(require('element-ui/lib/locale/lang/en').default)

// global config
Vue.prototype.$ELEMENT = {
  size: 'mini'
}

// register components
Vue.use(Button)
Vue.use(Input)
Vue.use(Select)
Vue.use(Option)
Vue.use(OptionGroup)
Vue.use(Checkbox)
Vue.use(Radio)
Vue.use(RadioGroup)
Vue.use(Dialog)
Vue.use(Tabs)
Vue.use(TabPane)
Vue.use(ColorPicker)

Vue.use(Loading.directive)

Vue.prototype.$notify = Notification
