package com.side.project.domain.activitylog;

public enum ActivityType {
    ITEM_VIEW,
    WISHLIST_ADD,
    WISHLIST_DELETE,
    ORDER_CREATE,
    ORDER_ACCEPTED,
    ORDER_PAID,
    ORDER_SHIPPED,
    ORDER_CONFIRMED,
    ORDER_CANCELED;

    public static ActivityType fromOrderAction(String action) {
        return switch (action) {
            case "ACCEPT" -> ORDER_ACCEPTED;
            case "PAY" -> ORDER_PAID;
            case "SHIP" -> ORDER_SHIPPED;
            case "CONFIRM" -> ORDER_CONFIRMED;
            case "CANCEL" -> ORDER_CANCELED;
            default -> throw new IllegalArgumentException("알 수 없는 주문 action 입니다: " + action);
        };
    }
}
