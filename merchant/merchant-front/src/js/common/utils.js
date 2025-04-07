import {clone} from 'xe-utils';

export const CopyObj = (target, original) => {
    if (original) {
        //解除obs
        original = clone(original, true);
        Object.keys(target).forEach(key => {
            target[key] = original[key];
        });
    }
}

export const ObjectUtil = {
    isEmpty: function (obj) {
        if (!obj) return true;
        const call = Object.prototype.toString.call(obj);
        if (call === '[object Array]' && obj.length === 0) return true;
        if (call === '[object Object]' && Object.keys(obj).length === 0) return true;
        obj = `${obj}`;
        return obj.trim() === '';
    }
}
