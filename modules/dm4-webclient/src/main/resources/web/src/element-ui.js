import Vue from 'vue'
import {Select, Option} from 'element-ui'
import lang from 'element-ui/lib/locale/lang/en'
import locale from 'element-ui/lib/locale'

// configure language
locale.use(lang)

// register components
Vue.use(Select)
Vue.use(Option)
