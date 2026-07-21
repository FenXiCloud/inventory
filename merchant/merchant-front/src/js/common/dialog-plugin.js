/**
 * TDesign DialogPlugin 封装：确认后自动关闭（官方需手动 destroy）
 */
import {DialogPlugin as RawDialogPlugin} from 'tdesign-vue-next';

function closeDialog(dialog) {
  if (!dialog) return;
  if (typeof dialog.destroy === 'function') {
    dialog.destroy();
  } else if (typeof dialog.hide === 'function') {
    dialog.hide();
  }
}

function wrapCreate(fn) {
  return (options, context) => {
    const opts = options && typeof options === 'object' ? {...options} : {};
    const userOnConfirm = opts.onConfirm;
    let dialog;

    opts.onConfirm = (ctx) => {
      const result = typeof userOnConfirm === 'function' ? userOnConfirm(ctx) : undefined;
      return Promise.resolve(result)
          .then(() => {
            closeDialog(dialog);
          })
          .catch((err) => {
            throw err;
          });
    };

    dialog = fn(opts, context);
    return dialog;
  };
}

const DialogPlugin = wrapCreate(RawDialogPlugin);
DialogPlugin.confirm = wrapCreate(RawDialogPlugin.confirm.bind(RawDialogPlugin));
DialogPlugin.alert = wrapCreate(RawDialogPlugin.alert.bind(RawDialogPlugin));
if (typeof RawDialogPlugin.install === 'function') {
  DialogPlugin.install = RawDialogPlugin.install.bind(RawDialogPlugin);
}
Object.defineProperty(DialogPlugin, '_context', {
  get() {
    return RawDialogPlugin._context;
  },
  set(v) {
    RawDialogPlugin._context = v;
  }
});

export {DialogPlugin};
export default DialogPlugin;
