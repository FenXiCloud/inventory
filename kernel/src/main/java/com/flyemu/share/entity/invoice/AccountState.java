package com.flyemu.share.entity.invoice;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

/**
 * 数电账户认证状态 — 每个商户一条，重启后不丢失 aggOrgId / accountId。
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table
@DynamicUpdate
public class AccountState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("组织ID（产品订购后获得）")
    @Column(length = 64)
    private String aggOrgId;

    @Comment("平台账号ID")
    @Column(length = 64)
    private String accountId;

    @Comment("纳税人识别号")
    @Column(length = 64)
    private String nsrsbh;

    @Comment("企业名称")
    @Column(length = 200)
    private String aggOrgName;

    @Comment("地区编码")
    @Column(length = 8)
    private String dq;

    @Comment("是否已登录（税局会话有效）")
    private boolean loggedIn;

    @Comment("扫脸时长（秒）")
    private Integer scanDuration;

    @Comment("当前人脸识别的 rzid（用于回调后查询扫脸状态）")
    @Column(length = 128)
    private String qrRzid;

    @Comment("当前二维码原始文本 ewm（用于生成二维码图片）")
    @Column(length = 1024)
    private String qrEwm;

    @Comment("最后一次扫码成功时间")
    @Column(length = 32)
    private String lastScanTime;

    @Comment("最后一次扫码返回消息")
    @Column(length = 500)
    private String lastScanMessage;

    @Comment("登录人真实姓名（从配置读取，不持久化到数据库）")
    @Transient
    private String realName;

    @Comment("登录身份（从配置读取，不持久化到数据库）")
    @Transient
    private String roleName;

    @Comment("登录手机号（从配置读取，不持久化到数据库）")
    @Transient
    private String phone;

    @Comment("商户ID")
    @Column(nullable = false)
    private Long merchantId;
}
