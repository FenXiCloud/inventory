package com.flyemu.share.dto;

import com.flyemu.share.entity.setting.AccountBook;
import com.flyemu.share.entity.setting.Merchant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class AccountBookTreeDto {

    private Merchant merchant;
    private List<AccountBook> accountBookList;
}
