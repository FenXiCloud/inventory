/**
 * <p>****************************************************************************</p>
 * <p><b>Copyright © 2010-2020 soho team All Rights Reserved<b></p>
 * <ul style="margin:15px;">
 * <li>Description : organization.js</li>
 * <li>Version     : 1.0</li>
 * <li>Creation    : 2022/3/11 9:49</li>
 * <li>@author     : ____′↘TangSheng</li>
 * </ul>
 * <p>****************************************************************************</p>
 */
import Ajax from '@common/Request';

export default {
	save(param) {
		return Ajax[param.id ? 'put' : 'post']('/accountBook', param);
	},
	list(param) {
		return Ajax.get('/accountBook', param);
	},
	listAll() {
		return Ajax.get('/accountBook/all');
	},
	remove(id) {
		return Ajax.delete('/accountBook/' + id);
	},
	select(param) {
		return Ajax.get('/accountBook/select', param);
	},
	changeCurrentAccountBook(id) {
		return Ajax.put('/accountBook/change/current/' + id);
	},

	/* 参数列表 */
	getByAccountBookId(param) {
		const id = typeof param === 'object' ? param.id : param;
		return Ajax.get('/accountBookParameters/load/' + id);
	},
	saveParameters(param) {
		return Ajax.put('/accountBookParameters', param);
	},
	/* 成本法切换预览：期间起始日 + 本期间已审核出入库流水数（二次确认警告文案用） */
	costSwitchPreview(accountBookId) {
		return Ajax.get('/accountBookParameters/costSwitchPreview', {accountBookId});
	},
	/* 当前登录账套参数（含 costAccounting 成本核算方法） */
	parameters() {
		return Ajax.get('/accountBookParameters');
	}
};
