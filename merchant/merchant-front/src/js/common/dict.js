/**
 * <p>****************************************************************************</p>
 * <p><b>Copyright © 2010-2019纷析云（杭州）科技有限公司All Rights Reserved<b></p>
 * <ul style="margin:15px;">
 * <li>Description : 字典</li>
 * <li>Version     : 1.0</li>
 * <li>Creation    : 2019年08月02日</li>
 * <li>@author     : ____′↘夏悸</li>
 * </ul>
 * <p>****************************************************************************</p>
 */
import {heyuiConfig} from "heyui.ext";

heyuiConfig.initDict({
  defaultRadios: [{key: true, title: '是'}, {key: false, title: '否'}],
  statusRadios: [{key: true, title: '上架'}, {key: false, title: '下架'}],
  enableRadios: [{key: true, title: '启用'}, {key: false, title: '禁用'}],
  relationRadios: [{key: true, title: '关联'}, {key: false, title: '不关联'}],
});
