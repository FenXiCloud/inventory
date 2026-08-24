package com.flyemu.share.controller.invoice;

import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.dto.invoice.PagedResponse;
import com.flyemu.share.entity.invoice.GoodsItem;
import com.flyemu.share.repository.invoice.GoodsItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/invoice/goods")
@RequiredArgsConstructor
public class GoodsController {

    private final GoodsItemRepository goodsRepo;

    // ========== List / Search ==========

    @GetMapping
    public JsonResult list(@RequestParam(required = false) String keyword,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "20") int size,
                           @SaAccountVal AccountDto accountDto) {
        Page<GoodsItem> result = goodsRepo.search(accountDto.getMerchantId(), keyword, PageRequest.of(page, size));
        return JsonResult.successful(PagedResponse.of(result.getContent(), result.getTotalElements(), PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public JsonResult get(@PathVariable Long id, @SaAccountVal AccountDto accountDto) {
        GoodsItem item = goodsRepo.findById(id)
                .filter(g -> accountDto.getMerchantId().equals(g.getMerchantId()))
                .orElseThrow(() -> new RuntimeException("商品不存在: id=" + id));
        return JsonResult.successful(item);
    }

    // ========== CRUD ==========

    @PostMapping
    @Transactional
    public JsonResult create(@RequestBody GoodsItem item, @SaAccountVal AccountDto accountDto) {
        item.setId(null);
        item.setMerchantId(accountDto.getMerchantId());
        item.setAccountBookId(accountDto.getAccountBookId());
        if (item.getGoodsCode() != null && goodsRepo.findByMerchantIdAndGoodsCode(accountDto.getMerchantId(), item.getGoodsCode()).isPresent()) {
            throw new RuntimeException("税收编码已存在: " + item.getGoodsCode());
        }
        return JsonResult.successful(goodsRepo.save(item));
    }

    @PutMapping("/{id}")
    @Transactional
    public JsonResult update(@PathVariable Long id, @RequestBody GoodsItem item, @SaAccountVal AccountDto accountDto) {
        GoodsItem existing = goodsRepo.findById(id)
                .filter(g -> accountDto.getMerchantId().equals(g.getMerchantId()))
                .orElseThrow(() -> new RuntimeException("商品不存在: id=" + id));
        existing.setGoodsName(item.getGoodsName());
        existing.setGoodsCode(item.getGoodsCode());
        existing.setSpec(item.getSpec());
        existing.setUnit(item.getUnit());
        existing.setTaxRateLabel(item.getTaxRateLabel());
        existing.setDefault(item.isDefault());
        existing.setTaxPreference(item.isTaxPreference());
        return JsonResult.successful(goodsRepo.save(existing));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public JsonResult delete(@PathVariable Long id, @SaAccountVal AccountDto accountDto) {
        goodsRepo.findById(id)
                .filter(g -> accountDto.getMerchantId().equals(g.getMerchantId()))
                .ifPresent(goodsRepo::delete);
        return JsonResult.successful(Map.of("message", "删除成功"));
    }
}
