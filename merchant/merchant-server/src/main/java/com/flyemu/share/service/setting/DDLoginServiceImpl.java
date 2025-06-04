package com.flyemu.share.service.setting;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.dingtalkoauth2_1_0.models.GetAccessTokenResponse;
import com.aliyun.tea.TeaException;
import com.aliyun.teaopenapi.models.Config;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiUserGetuserinfoRequest;
import com.dingtalk.api.request.OapiUserListidRequest;
import com.dingtalk.api.request.OapiV2DepartmentListsubRequest;
import com.dingtalk.api.request.OapiV2UserGetRequest;
import com.dingtalk.api.response.OapiUserGetuserinfoResponse;
import com.dingtalk.api.response.OapiUserListidResponse;
import com.dingtalk.api.response.OapiV2DepartmentListsubResponse;
import com.dingtalk.api.response.OapiV2UserGetResponse;
import com.flyemu.share.entity.setting.Admin;
import com.flyemu.share.entity.setting.Dept;
import com.flyemu.share.entity.setting.Role;
import com.flyemu.share.repository.AdminRepository;
import com.taobao.api.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * @author: wangwenjia
 * @since: 2025/5/20 15:11
 * @description:
 */
@Service
@Slf4j
public class DDLoginServiceImpl implements DDLoginService{

    private static String accessToken = null;

    @Value("${dingtalk.appKey}")
    private String appKey;
    @Value("${dingtalk.appSecret}")
    private String appSecret;
    @Value("${dingtalk.corpId}")
    private String corpId;
    @Value("${dingtalk.agentID}")
    private String agentID;

    @Autowired
    private AdminService adminService;
    @Autowired
    private DeptService deptService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private AdminRepository adminRepository;

    /**
     * 登录回调
     *
     * @param code
     * @Return java.lang.String
     */
    @Override
    public String getLoginAuth(String code, String corpId) {
        log.info("钉钉登录,回调code：{}", code);
        com.aliyun.dingtalkoauth2_1_0.Client client = null;
        //根据corpId找到对应的组织
        //省略校验
       /* List<JSONObject> infos = sysDeptMapper.selectDeptDDInfo(corpId);
        String appKey = "";
        String appSecret = "";
        Long deptId = null;
        for (JSONObject infoJson : infos) {
            if (ObjectUtils.isEmpty(infoJson)) {
                continue;
            }
            JSONObject jsonObject = infoJson.getJSONObject("info");
            if (ObjectUtils.isEmpty(jsonObject) || ObjectUtils.isNotEmpty(jsonObject.getString("appKey"))
                    || ObjectUtils.isNotEmpty(jsonObject.getString("appSecret"))
                    || ObjectUtils.isNotEmpty(jsonObject.getString("cropId"))   ) {
                continue;
            }
            if (!corpId.equals(jsonObject.getString("CropId"))) {
                continue;
            }
            deptId = infoJson.getLong("deptId");
            appKey = jsonObject.getString("AppKey");
            appSecret = jsonObject.getString("AppSecret");
        }
        if (ObjectUtils.isEmpty(deptId)) {
            throw new RuntimeException("该公司暂未配置钉钉免密码登录");
        }*/
        try {
            client = createClient();
            com.aliyun.dingtalkoauth2_1_0.models.GetAccessTokenRequest getAccessTokenRequest = new com.aliyun.dingtalkoauth2_1_0.models.GetAccessTokenRequest()
                    .setAppKey(appKey)
                    .setAppSecret(appSecret);

            com.aliyun.dingtalkoauth2_1_0.models.GetAccessTokenResponse res = client.getAccessToken(getAccessTokenRequest);
            com.aliyun.dingtalkoauth2_1_0.models.GetAccessTokenResponseBody body = res.body;

            String accessToken = body.getAccessToken();
            log.info("钉钉登录,获取用企业token：{}", accessToken);
            // 获取用户信息
            DingTalkClient client1 = new DefaultDingTalkClient("https://oapi.dingtalk.com/user/getuserinfo");
            OapiUserGetuserinfoRequest request = new OapiUserGetuserinfoRequest();
            request.setCode(code);
            request.setHttpMethod("GET");
            OapiUserGetuserinfoResponse response;
            try {
                response = client1.execute(request, accessToken);
                log.info("钉钉登录获取用户信息：{}", JSON.toJSONString(response));
            } catch (ApiException e) {
                log.info("钉钉登录获取用户信息，失败：{}", e);
                throw new RuntimeException("登录失败！请联系管理员！");
            }
            // 查询得到当前用户的userId
            String userId = response.getUserid();
//            String userId = "01020445356138043786";
            if(ObjectUtils.isEmpty(userId)){
                throw new RuntimeException("系统中未含该成员！请联系管理员！");
            }

    /*        SysUser user = new SysUser();
            user.setDingDingUserId(userId);
            user.setDeptId(deptId);*/
            Admin admin = adminService.selectAdminByDingDingUserId(userId);
            if (ObjectUtils.isEmpty(admin)) {
                throw new RuntimeException("系统中未含该成员！请联系管理员！");
            }
            //登录
            return admin.getMobile();
        } catch (Exception e) {
            log.error("钉钉登录回调异常：" ,e);
            throw new RuntimeException(e);
        }
    }

    class Counter {
        int sumNum = 0;
        int successNum = 0;
        int failureNum = 0;
    }

    public static com.aliyun.dingtalkoauth2_1_0.Client createClient() throws Exception {
        Config config = new Config();
        config.protocol = "https";
        config.regionId = "central";
        return new com.aliyun.dingtalkoauth2_1_0.Client(config);
    }

    @Override
    public String addUserByDingDing() {
        Counter counter = new Counter();
        Set<String> userIds = new HashSet<>();
        //获取accessToken
        accessToken = this.getAccessToken();
        log.info("accessToken:" + accessToken);
        try {
            DingTalkClient client1 = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/v2/department/listsub");
            OapiV2DepartmentListsubRequest req1 = new OapiV2DepartmentListsubRequest();
            //获取部门id列表
            OapiV2DepartmentListsubResponse rsp1 = client1.execute(req1, accessToken);
            //查询默认角色
            Long roleId = null;
            List<Role> roleList = roleService.systemDefaultRole();
            if(!ObjectUtils.isEmpty(roleList) && roleList.size() > 0){
                roleId = roleList.get(0).getId();
            }
            this.insertUserByDingDing(rsp1, counter, userIds, roleId);
        } catch (ApiException e) {
            log.error("获取钉钉用户异常：", e);
            e.printStackTrace();
        }
        String message = "共拉取到：" + counter.sumNum + "条；<br/>成功："
                + counter.successNum + "条；<br/>更新：" + counter.failureNum + "条；";
        log.info(message);
        return message;
    }

    @Override
    public String getToken() {
        return this.getAccessToken();
    }

    /**
     * 获取accessToken
     * @return accessToken信息
     */
    private String getAccessToken() {
        GetAccessTokenResponse accessTokenRsp = new GetAccessTokenResponse();
        try {
            com.aliyun.dingtalkoauth2_1_0.Client client = createClient();
            com.aliyun.dingtalkoauth2_1_0.models.GetAccessTokenRequest getAccessTokenRequest = new com.aliyun.dingtalkoauth2_1_0.models.GetAccessTokenRequest()
                    .setAppKey(appKey)
                    .setAppSecret(appSecret);
            accessTokenRsp = client.getAccessToken(getAccessTokenRequest);
        } catch (TeaException err) {
            if (!com.aliyun.teautil.Common.empty(err.code) && !com.aliyun.teautil.Common.empty(err.message)) {
                // err 中含有 code 和 message 属性，可帮助开发定位问题
                log.error("code:" + err.code + ";message:" + err.message );
            }
        } catch (Exception _err) {
            TeaException err = new TeaException(_err.getMessage(), _err);
            if (!com.aliyun.teautil.Common.empty(err.code) && !com.aliyun.teautil.Common.empty(err.message)) {
                // err 中含有 code 和 message 属性，可帮助开发定位问题
                log.error("code:" + err.code + ";message:" + err.message );
            }
        }

        return accessTokenRsp.getBody().getAccessToken();
    }

    public void insertUserByDingDing(OapiV2DepartmentListsubResponse rsp1, Counter counter, Set<String> userIds, Long roleId) throws ApiException {
        if (ObjectUtils.isEmpty(rsp1.getResult())) {
            return;
        }
        DingTalkClient client1 = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/v2/department/listsub");
        DingTalkClient client2 = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/user/listid");
        DingTalkClient client3 = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/v2/user/get");
        OapiV2DepartmentListsubRequest req1 = new OapiV2DepartmentListsubRequest();
        OapiUserListidRequest req2 = new OapiUserListidRequest();
        OapiV2UserGetRequest req3 = new OapiV2UserGetRequest();

        //循环部门列表
        for (OapiV2DepartmentListsubResponse.DeptBaseResponse deptBaseResponse : rsp1.getResult()) {
            //判断这个组织是否存在
            String deptName = deptBaseResponse.getName();
//            SysDept sysDept = sysDeptMapper.checkDeptNameUnique(deptName, SecurityUtils.getDeptId());
//            SysDept info = sysDeptMapper.selectDeptById(SecurityUtils.getDeptId());

            Dept sysDept = deptService.checkDeptNameUnique(deptName);

            if (ObjectUtils.isEmpty(sysDept)) {
                //新建部门
                sysDept = new Dept();
                sysDept.setDeptName(deptName);
//                sysDept.setParentId(SecurityUtils.DeptId);
                sysDept.setDelFlag("0");
                sysDept.setStatus("0");
//                sysDept.setAncestors(info.getAncestors() + "," + info.getId());
                sysDept.setAncestors("");
                deptService.insertDept(sysDept);
            }
            req1.setDeptId(deptBaseResponse.getDeptId());
            //获取部门id列表
            OapiV2DepartmentListsubResponse rsp11 = client1.execute(req1, accessToken);
            this.insertUserByDingDing(rsp11, counter, userIds, roleId);

            req2.setDeptId(deptBaseResponse.getDeptId());
            //获取部门下的员工列表
            OapiUserListidResponse rsp2 = client2.execute(req2, accessToken);
            log.info("钉钉拉取用户用户列表：{}", JSONObject.toJSONString(rsp2));
            if (ObjectUtils.isEmpty(rsp2) || ObjectUtils.isEmpty(rsp2.getResult())) {
                log.error("钉钉拉取用户用户列表为空：{}", deptBaseResponse.getDeptId());
                continue;
            }
            for (String userId : rsp2.getResult().getUseridList()) {
                if (ObjectUtils.isEmpty(userId)) {
                    log.error("钉钉拉取用户userId为空：{}", userId);
                    continue;
                }
                if (!userIds.contains(userId)) {
                    userIds.add(userId);
                    req3.setUserid(userId);
                    //获取员工信息
                    OapiV2UserGetResponse rsp3 = client3.execute(req3, accessToken);
                    log.info("钉钉拉取用户信息：{}", JSONObject.toJSONString(rsp3));
                    if (ObjectUtils.isEmpty(rsp3) || ObjectUtils.isEmpty(rsp3.getResult())) {
                        log.error("钉钉拉取用户信息为空：{}", userId);
                        continue;
                    }

                    Admin admin = new Admin();
                    admin.setEmail(rsp3.getResult().getEmail());
                    admin.setUsername(rsp3.getResult().getName());
                    admin.setName(rsp3.getResult().getName());
                    admin.setMobile(rsp3.getResult().getMobile());
                    admin.setDeptId(sysDept.getId());
                    admin.setDingDingUserId(userId);
                    admin.setMerchantId(1L);
                    admin.setRoleId(roleId);
                    //如果手机号为空 取钉钉id后6位
                    if(StringUtils.isEmpty(admin.getMobile())){
                        admin.setMobile(admin.getDingDingUserId().substring(admin.getDingDingUserId().length() - 6));
                        admin.setPassword(DigestUtil.bcrypt(admin.getDingDingUserId().substring(admin.getDingDingUserId().length() - 6)));
                    }else{
                        admin.setPassword(DigestUtil.bcrypt(admin.getMobile().substring(admin.getMobile().length() - 6)));
                    }

                    // 验证是否存在这个用户
                    counter.sumNum++;
//                    SysUser u = userMapper.selectUserByUserName(sysUser.getUserName());
                    Admin u = adminService.selectAdminByMobile(admin.getMobile());
                    if (ObjectUtils.isEmpty(u)) {
                        //存在名称相同+数字
                        int num = adminService.queryNumByUserName(admin.getUsername());
                        if(num > 0){
                            admin.setUsername(rsp3.getResult().getName() + "_" + (num));
                        }
                        this.adminSave(admin);
                        counter.successNum++;
                    } else{
                        u.setDingDingUserId(userId);
                        this.adminSave(u);
                        counter.failureNum++;
                    }
                }
            }
        }
    }

    /**
     * 添加/更新用户
     * @param admin 实体
     */
    private void adminSave(Admin admin) {
        if (admin.getId() != null) {
            Admin original = adminService.selectByPrimaryKey(admin.getId());
            //判断是否更新了手机号码，更新手机号需要检查重复
            BeanUtil.copyProperties(admin, original, CopyOptions.create().ignoreNullValue());
            adminRepository.save(original);
        }else{
            admin.setSystemDefault(false);
            admin.setEnabled(true);
            adminRepository.save(admin);
        }

    }

}
