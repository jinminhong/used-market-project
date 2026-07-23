package com.side.project.domain.orders.ordersdto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class TrackingUpdateDto {

    @NotEmpty
    private String trackingCompany;

    @NotEmpty
    private String trackingNumber;
}
