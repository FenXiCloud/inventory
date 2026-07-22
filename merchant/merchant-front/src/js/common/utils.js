/**
 * 轻量工具：clone / pick / toArrayTree / CopyObj
 */

export const clone = (obj, deep = false) => {
	if (obj == null || typeof obj !== 'object') return obj;
	if (!deep) {
		if (Array.isArray(obj)) return obj.slice();
		return Object.assign({}, obj);
	}
	return JSON.parse(JSON.stringify(obj));
};

export const pick = (obj, keys = []) => {
	if (obj == null) return {};
	const result = {};
	keys.forEach((key) => {
		if (Object.prototype.hasOwnProperty.call(obj, key)) {
			result[key] = obj[key];
		}
	});
	return result;
};

/**
 * 扁平数组转树
 * options: { key, parentKey, children, strict }
 */
export const toArrayTree = (list = [], options = {}) => {
	const key = options.key || 'id';
	const parentKey = options.parentKey || 'parentId';
	const childrenKey = options.children || 'children';
	const strict = !!options.strict;

	const nodes = (list || []).map((item) => ({ ...item }));
	const map = {};
	nodes.forEach((node) => {
		map[node[key]] = node;
		if (!Array.isArray(node[childrenKey])) {
			node[childrenKey] = [];
		}
	});

	const roots = [];
	nodes.forEach((node) => {
		const parentId = node[parentKey];
		const hasParent = parentId != null && parentId !== '' && parentId !== 0 && map[parentId];
		if (hasParent) {
			map[parentId][childrenKey].push(node);
		} else if (!strict || parentId == null || parentId === '' || parentId === 0) {
			roots.push(node);
		} else if (!strict) {
			roots.push(node);
		}
	});

	const prune = (arr) => {
		arr.forEach((n) => {
			if (n[childrenKey] && n[childrenKey].length) prune(n[childrenKey]);
			else delete n[childrenKey];
		});
	};
	prune(roots);
	return roots;
};

export const CopyObj = (target, original) => {
	if (original) {
		//解除obs
		original = clone(original, true);
		Object.keys(target).forEach((key) => {
			target[key] = original[key];
		});
	}
};

export const ObjectUtil = {
	isEmpty: function (obj) {
		if (!obj) return true;
		const call = Object.prototype.toString.call(obj);
		if (call === '[object Array]' && obj.length === 0) return true;
		if (call === '[object Object]' && Object.keys(obj).length === 0) return true;
		obj = `${obj}`;
		return obj.trim() === '';
	}
};
