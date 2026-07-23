package com.side.project.domain.orders;

import com.side.project.domain.orders.ordersdto.OrdersResponseDto;
import com.side.project.domain.orders.ordersdto.PurchasesPageResponseDto;
import com.side.project.domain.orders.ordersdto.SalesPageResponseDto;
import com.side.project.domain.orders.ordersdto.TrackingUpdateDto;
import com.side.project.web.argumentresolver.Login;
import com.side.project.web.login.LoginMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrdersController {

    private final OrdersService ordersService;

    @PostMapping("/{itemId}")
    public ResponseEntity<?> orderItem(@PathVariable(name = "itemId") Long itemId,
                                       @Login LoginMember loginMember) {
        ordersService.save(itemId , loginMember.getMemberId());
        return ResponseEntity.ok(Map.of("status","ok","message","구매가 완료되었습니다."));
    }

    @GetMapping("/purchases")
    public ResponseEntity<PurchasesPageResponseDto> purchasesItemList(@Login LoginMember loginMember,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "10") int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        PurchasesPageResponseDto purchasesList = ordersService.getPurchasesList(loginMember.getMemberId(), pageRequest);
        return ResponseEntity.ok(purchasesList);
    }

    @GetMapping("/sales")
    public ResponseEntity<SalesPageResponseDto> salesItemList(@Login LoginMember loginMember,
                                                              @RequestParam(required = false) OrderStatus status,
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "10") int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        SalesPageResponseDto salesList = ordersService.getSalesList(loginMember.getMemberId(), status, pageRequest);
        return ResponseEntity.ok(salesList);
    }

    @PatchMapping("/{orderId}/tracking")
    public ResponseEntity<OrdersResponseDto> registerTracking(@PathVariable Long orderId,
                                                                @Login LoginMember loginMember,
                                                                @Valid @RequestBody TrackingUpdateDto trackingUpdateDto) {
        OrdersResponseDto response = ordersService.registerTracking(orderId, loginMember.getMemberId(), trackingUpdateDto);
        return ResponseEntity.ok(response);
    }
}
