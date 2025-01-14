import app from './app'

Error.prototype.toString = function () {
  return `${this.message}${this.cause ? `, Caused by ${this.cause}` : ''}`
}

export default function onHttpError (error) {
  // enhancement
  error.message = error.response.data.error || error.message
  error.cause = error.response.data.cause
  // alert box
  const level = error.response.data.level || 'ERROR'
  app.config.globalProperties.$notify({
    title: level,
    type: level.toLowerCase(),
    message: `<p>${error.message}</p>${error.cause ? `<p>Cause: ${error.cause}</p>` : ''}`,
    dangerouslyUseHTMLString: true,
    duration: 0
  })
}
