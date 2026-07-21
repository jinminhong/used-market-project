export function defaultImage() {
  return "https://images.unsplash.com/photo-1496747611176-843222e1e57c?auto=format&fit=crop&w=900&q=80";
}

export function imageUrlFromItemImages(itemImages) {
  const storedFileName = itemImages?.[0]?.storedFileName ?? itemImages?.[0]?.storedFilename;
  return storedFileName ? `/api/images/${encodeURIComponent(storedFileName)}` : "";
}

export function imageUrlFromUploadFile(uploadFile) {
  const storedFileName = uploadFile?.storedFileName ?? uploadFile?.storedFilename;
  return storedFileName ? `/api/images/${encodeURIComponent(storedFileName)}` : "";
}

export function imageUrlFromThumbnail(thumbnailFilename) {
  return thumbnailFilename ? `/api/images/${encodeURIComponent(thumbnailFilename)}` : "";
}

export function normalizeMember(member) {
  if (!member) return null;
  return {
    memberId: member.memberId ?? member.id ?? null,
    loginId: member.loginId ?? "",
    nickName: member.nickName ?? member.nickname ?? "",
  };
}

export function normalizeItem(item, fallbackId) {
  const itemImages = item.itemImages ?? (item.uploadFileDto ? [item.uploadFileDto] : []);

  return {
    itemId: item.itemId ?? item.id ?? fallbackId ?? null,
    memberId: item.memberId ?? item.sellerId ?? null,
    name: item.name ?? item.title ?? item.itemName ?? "이름 없는 상품",
    description: item.description ?? "",
    price: Number(item.price ?? 0),
    status: item.status ?? item.itemStatus ?? "SELLING",
    nickName: item.nickName ?? item.nickname ?? item.sellerNickName ?? "",
    category: item.category ?? "ETC",
    itemImages,
    imageUrl: item.imageUrl || imageUrlFromUploadFile(item.uploadFileDto) || imageUrlFromItemImages(itemImages) || imageUrlFromThumbnail(item.thumbnailFilename) || defaultImage(),
  };
}

export function normalizeMemberInfo(data) {
  if (!data) return null;
  return {
    memberId: data.memberId ?? data.id ?? null,
    loginId: data.loginId ?? "",
    nickName: data.nickName ?? data.nickname ?? "",
    name: data.name ?? "",
    address: {
      city: data.address?.city ?? "",
      street: data.address?.street ?? "",
      zipcode: data.address?.zipcode ?? "",
    },
  };
}

export function normalizeShop(shop) {
  if (!shop) return null;
  return {
    memberId: shop.memberId ?? shop.id ?? null,
    loginId: shop.loginId ?? "",
    nickName: shop.nickName ?? shop.nickname ?? "",
    name: shop.name ?? "",
    items: (shop.itemList ?? shop.items ?? []).map((item, index) => normalizeItem(item, index + 1)),
  };
}

export function normalizeChatRoom(raw) {
  if (!raw) return null;
  return {
    roomId: raw.roomId ?? raw.id ?? null,
    itemId: raw.itemId ?? null,
    buyerId: raw.buyerId ?? null,
    sellerId: raw.sellerId ?? null,
  };
}

export function normalizeChatMessage(raw) {
  if (!raw) return null;
  return {
    messageId: raw.messageId ?? null,
    roomId: raw.roomId ?? null,
    senderId: raw.senderId ?? null,
    senderNickname: raw.senderNickname ?? raw.senderNickName ?? "",
    content: raw.content ?? "",
    sentAt: raw.sentAt ?? null,
    type: raw.type ?? "TEXT",
    offeredPrice: raw.offeredPrice != null ? Number(raw.offeredPrice) : null,
    offerStatus: raw.offerStatus ?? null,
    orderId: raw.orderId ?? null,
  };
}

export function normalizePurchase(purchase) {
  if (!purchase) return null;
  return {
    orderId: purchase.orderId ?? null,
    orderStatus: purchase.orderStatus ?? "PAY_COMPLETED",
    agreedPrice: purchase.agreedPrice != null ? Number(purchase.agreedPrice) : null,
    purchaseDate: purchase.purchaseDate ?? null,
    sellerNickName: purchase.sellerNickName ?? "",
    item: {
      itemId: purchase.itemId ?? null,
      name: purchase.name ?? "이름 없는 상품",
      description: purchase.description ?? "",
      price: Number(purchase.price ?? 0),
      status: purchase.status ?? "SOLD",
      imageUrl: purchase.imageUrl || imageUrlFromThumbnail(purchase.thumbnailFilename) || defaultImage(),
    },
  };
}
