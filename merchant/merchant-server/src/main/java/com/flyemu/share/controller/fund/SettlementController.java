package com.flyemu.share.controller.fund;

import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.form.SettlementForm;
import com.flyemu.share.service.fund.SettlementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/settlement")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    @GetMapping
    public JsonResult list(Page page, SettlementService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(settlementService.query(page, query));
    }

    @GetMapping("/total")
    public JsonResult queryTotal(SettlementService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(settlementService.queryTotal(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid SettlementForm form, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(form.getOrder(), accountDto);
        settlementService.save(form);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid SettlementForm form, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(form.getOrder(), accountDto);
        settlementService.save(form);
        return JsonResult.successful();
    }

    @DeleteMapping("/{id}")
    public JsonResult delete(@PathVariable Long id, @SaAccountVal AccountDto accountDto) {
        settlementService.delete(id, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/load/{id}")
    public JsonResult load(@PathVariable Long id, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(settlementService.load(id, accountDto.getMerchantId()));
    }

    @PostMapping("/approved/{state}")
    public JsonResult approved(@RequestBody List<Long> ids, @PathVariable OrderStatus state, @SaAccountVal AccountDto accountDto) {
        settlementService.approved(ids, state, accountDto.getAdminId(), accountDto.getMerchantId());
        return JsonResult.successful();
    }

    @GetMapping("/writeOff")
    public JsonResult writeOff(Page page, SettlementService.WriteOffQuery query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(settlementService.writeOffCandidates(page, query));
    }

    /**
     * 一次性清理历史遗留的"未平账空单"（无任何结算明细的结算单主表）。
     * 与 /inventory/rebuildCostChain 同属后台维护接口；merchantId/accountBookId 缺省则全库清理。
     */
    @PostMapping("/cleanOrphanSettlements")
    public JsonResult cleanOrphanSettlements(@RequestParam(required = false) Long merchantId,
                                             @RequestParam(required = false) Long accountBookId) {
        int removed = settlementService.cleanOrphanSettlements(merchantId, accountBookId);
        return JsonResult.successful(removed).setMsg("已清理 " + removed + " 张空结算单");
    }

    /**
     * 一次性迁移：结算单取号切到 code_seed 后，把各账套归零桶计数器回填到历史最大流水号，
     * 保证新单从旧号之后接续且不重号。后台维护接口，正式切换取号前调用一次。
     */
    @PostMapping("/migrateSettlementSerialSeed")
    public JsonResult migrateSettlementSerialSeed() {
        int seeded = settlementService.migrateSettlementSerialSeed();
        return JsonResult.successful(seeded).setMsg("已将结算单取号接续到历史流水，共回填 " + seeded + " 处");
    }
}
