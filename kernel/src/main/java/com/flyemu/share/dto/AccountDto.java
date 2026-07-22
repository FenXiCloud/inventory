package com.flyemu.share.dto;

import com.flyemu.share.entity.setting.AccountBook;
import com.flyemu.share.entity.setting.Admin;
import com.flyemu.share.entity.setting.Merchant;
import com.flyemu.share.entity.setting.Role;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
@Getter
@Setter
@NoArgsConstructor
public class AccountDto implements Serializable {

    private Admin admin;

    private Merchant merchant;

    private AccountBook accountBook;

    private Role role;

    private LocalDate checkDate;

    /**
     * 授权功能
     */
    private List<String> granted;

    public AccountDto(Admin admin, Merchant merchant, Role role, AccountBook accountBook) {
        this.admin = admin;
        this.merchant = merchant;
        this.role = role;
        this.accountBook = accountBook;
        this.checkDate = accountBook.getCheckoutDate();

    }

    public AccountDto(Admin admin, Merchant merchant, Role role) {
        this.admin = admin;
        this.merchant = merchant;
        this.role = role;
    }

    public Long getAdminId() {
        return admin.getId();
    }

    public Long getMerchantId() {
        return merchant.getId();
    }

    public Long getAccountBookId() {
        if (accountBook != null){
            return accountBook.getId();
        }else {
            return null;
        }
    }

}
