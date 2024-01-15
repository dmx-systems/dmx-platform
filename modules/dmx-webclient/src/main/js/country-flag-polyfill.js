import {polyfillCountryFlagEmojis} from "country-flag-emoji-polyfill"
const isNeeded = polyfillCountryFlagEmojis()
console.log('[DMX] country-flag-emoji-polyfill:', isNeeded)
