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
import Ajax from "@common/Request";

export default {
	save(param) {
		return Ajax[param.id ? 'put' : 'post']('/accountBook', param)
	},
	list(param) {
		return Ajax.get('/accountBook', param)
	},
	listAll(merchantId) {
		return Ajax.get('/accountBook/all', {merchantId})
	},
	remove(id) {
		return Ajax.delete('/accountBook/' + id);
	},
	select(param){
		return Ajax.get('/accountBook/all', param)
	}
}
