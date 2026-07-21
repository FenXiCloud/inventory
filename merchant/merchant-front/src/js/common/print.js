import {h} from 'vue'
import {openDialog, closeDialog} from '@common/dialog'
import PrintPreview from '@/views/common/PrintPreview.vue'

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
