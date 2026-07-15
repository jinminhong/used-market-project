package com.side.project.domain.orders;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrdersController {

    private final OrdersService ordersService;

    @PostMapping("{itemId}")
    public ResponseEntity<?> orderItem(@PathVariable(name = "itemId") Long itemId) {
        ordersService.

        return ResponseEntity.ok(new Orders());
    }
}
