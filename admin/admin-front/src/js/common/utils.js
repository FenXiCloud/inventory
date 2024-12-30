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
