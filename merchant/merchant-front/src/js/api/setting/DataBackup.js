import Ajax from '@common/Request';

export default {
  list(param) {
    return Ajax.get('/dataBackup', param);
  },
  create(param) {
    return Ajax.post('/dataBackup/create', param || {});
  },
  restore(id) {
    return Ajax.post('/dataBackup/restore/' + id);
  },
  restoreUpload(formData) {
    return Ajax.post('/dataBackup/restoreUpload', formData, {
      'Content-Type': 'multipart/form-data',
      repeatable: true
    });
  },
  remove(id) {
    return Ajax.delete('/dataBackup/' + id);
  },
  download(id) {
    return Ajax.get('/dataBackup/' + id + '/download', null, { responseType: 'blob' });
  }
};
