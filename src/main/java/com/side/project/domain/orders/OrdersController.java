package com.side.project.domain.orders;

import com.side.project.domain.orders.ordersdto.*;
import com.side.project.web.login.LoginMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrdersController {

    private final OrdersService ordersService;

    @PostMapping("/{itemId}")
    public ResponseEntity<?> orderItem(@PathVariable(name = "itemId") Long itemId,
                                       @AuthenticationPrincipal LoginMember loginMember) {
        ordersService.save(itemId , loginMember.getMemberId() , OrderStatus.PAY_COMPLETED);
        return ResponseEntity.ok(Map.of("status","ok","message","구매가 완료되었습니다."));
    }

    @PatchMapping("/{orderId}")
    public ResponseEntity<OrdersActionResponseDto> changeOrderStatus(@PathVariable(name = "orderId") Long orderId,
                                               @AuthenticationPrincipal LoginMember loginMember,
                                               @RequestBody OrderActionRequest request) {
        OrdersActionResponseDto response = ordersService.changeOrderStatus(orderId, request.action(), loginMember.getMemberId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/purchases")
    public ResponseEntity<PurchasesPageResponseDto> purchasesItemList(@AuthenticationPrincipal LoginMember loginMember,
                                             @RequestParam(name = "status", required = false) List<OrderStatus> statuses,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "10") int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        PurchasesPageResponseDto purchasesList = ordersService.getPurchasesList(loginMember.getMemberId(), statuses, pageRequest);
        return ResponseEntity.ok(purchasesList);
    }

    @GetMapping("/sales")
    public ResponseEntity<SalesPageResponseDto> salesItemList(@AuthenticationPrincipal LoginMember loginMember,
                                                              @RequestParam(name = "status", required = false) List<OrderStatus> statuses,
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "10") int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        SalesPageResponseDto salesList = ordersService.getSalesList(loginMember.getMemberId(), statuses, pageRequest);
        return ResponseEntity.ok(salesList);
    }

    @PatchMapping("/{orderId}/tracking")
    public ResponseEntity<OrdersResponseDto> registerTracking(@PathVariable Long orderId,
                                                              @AuthenticationPrincipal LoginMember loginMember,
                                                              @Valid @RequestBody TrackingUpdateDto trackingUpdateDto) {
        OrdersResponseDto response = ordersService.registerTracking(orderId, loginMember.getMemberId(), trackingUpdateDto);
        return ResponseEntity.ok(response);
    }
}
