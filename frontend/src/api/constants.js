export const CATEGORIES = ["All", "OUTER", "TOP", "BOTTOM", "BAG", "SHOES", "ACCESSORY", "ETC"];
export const STATUSES = ["SELLING", "RESERVED", "SOLD"];

export const CATEGORY_LABELS = {
  All: "전체",
  OUTER: "아우터",
  TOP: "상의",
  BOTTOM: "하의",
  BAG: "가방",
  SHOES: "신발",
  ACCESSORY: "액세서리",
  ETC: "기타",
};

export const STATUS_LABELS = {
  SELLING: "판매중",
  RESERVED: "예약중",
  SOLD: "판매완료",
};

export const ORDER_STATUS_LABELS = {
  REQUESTED: "구매 요청",
  ACCEPTED: "승인됨",
  PAY_COMPLETED: "결제완료",
  SHIPPING: "배송중",
  COMPLETED: "거래완료",
  CANCELED: "취소됨",
};

export const sampleItems = [
  { itemId: 1, memberId: 1, name: "Archive Leather Jacket", description: "부드러운 양가죽 소재의 빈티지 재킷입니다. 자연스러운 사용감이 있고 전체적인 컨디션은 좋습니다.", price: 182000, status: "SELLING", nickName: "hong", category: "OUTER", imageUrl: "https://images.unsplash.com/photo-1551028719-00167b16eac5?auto=format&fit=crop&w=900&q=80" },
  { itemId: 2, memberId: 2, name: "Washed Cotton Shirt", description: "워싱감이 예쁜 코튼 셔츠입니다. 단품이나 이너로 활용하기 좋습니다.", price: 46000, status: "RESERVED", nickName: "lee", category: "TOP", imageUrl: "https://images.unsplash.com/photo-1602810318383-e386cc2a3ccf?auto=format&fit=crop&w=900&q=80" },
  { itemId: 3, memberId: 1, name: "Minimal Cross Bag", description: "데일리로 들기 좋은 크로스백입니다. 내부 수납 깨끗합니다.", price: 69000, status: "SELLING", nickName: "hong", category: "BAG", imageUrl: "https://images.unsplash.com/photo-1594223274512-ad4803739b7c?auto=format&fit=crop&w=900&q=80" },
  { itemId: 4, memberId: 3, name: "Suede Loafers", description: "스웨이드 로퍼 260 사이즈입니다. 밑창 마모 적습니다.", price: 88000, status: "SOLD", nickName: "mori", category: "SHOES", imageUrl: "https://images.unsplash.com/photo-1614252369475-531eba835eb1?auto=format&fit=crop&w=900&q=80" },
];
