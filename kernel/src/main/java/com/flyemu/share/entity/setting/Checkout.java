package com.flyemu.share.entity.setting;

import com.flyemu.share.common.TenantAware;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = { "checkDate", "accountBookId", "merchantId"})})
public class Checkout implements Serializable, TenantAware{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    @org.hibernate.annotations.ColumnDefault("0")
    private Long version = 0L;

    @Comment("创建时间")
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Comment("结账人")
    private Long checkId;

    @Comment("结账日期")
    private LocalDate checkDate;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;

}
