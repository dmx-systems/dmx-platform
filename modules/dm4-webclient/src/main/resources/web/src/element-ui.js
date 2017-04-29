import Vue from 'vue'
import { Button, Select, Option } from 'element-ui'
import locale from 'element-ui/lib/locale'

// set locale
locale.use(require('element-ui/lib/locale/lang/en').default)

// register components
Vue.use(Button)
Vue.use(Select)
Vue.use(Option)
