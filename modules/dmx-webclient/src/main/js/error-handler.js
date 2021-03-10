import {Notification} from 'element-ui'

export default function onHttpError (error) {
  const report = error.response.data
  Notification.error({
    title: 'Error',
    message: `
      <div class="field">
        <div class="field-label">Problem</div>
        <div>${report.problem}.</div>
      </div>
      <div class="field">
        <div class="field-label">Cause</div>
        <div>${report.cause}.</div>
      </div>`,
    dangerouslyUseHTMLString: true,
    duration: 0
  })
}
