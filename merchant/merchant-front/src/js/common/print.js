import {h} from 'vue'
import {openDialog, closeDialog} from '@common/dialog'
import PrintPreview from '@/views/common/PrintPreview.vue'
import TemplatePreview from '@/views/common/TemplatePreview.vue'

/**
 * Open print preview dialog for a document.
 * @param {string} documentType - PrintTemplate DocumentType enum name
 * @param {{header?: object, items?: array}} data
 */
export function openPrint(documentType, data) {
  const dialogId = openDialog({
    header: '打印预览',
    width: '900px',
    closeOnOverlayClick: false,
    body: h(PrintPreview, {
      documentType,
      data: data || {},
      onClose: () => closeDialog(dialogId)
    })
  })
  return dialogId
}

/**
 * 打印模板列表页的「预览」：用示例数据渲染指定模板效果。
 * @param {{name?: string, documentType: string, systemDefault?: boolean, content: array|string}} template
 */
export function openTemplatePreview(template) {
  const dialogId = openDialog({
    header: '打印模板预览',
    width: '880px',
    closeOnOverlayClick: false,
    body: h(TemplatePreview, {
      template,
      onClose: () => closeDialog(dialogId)
    })
  })
  return dialogId
}
