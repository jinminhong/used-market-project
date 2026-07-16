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
    name: item.name ?? item.title ?? "이름 없는 상품",
    description: item.description ?? "",
    price: Number(item.price ?? 0),
    status: item.status ?? "SELLING",
    nickName: item.nickName ?? item.nickname ?? "",
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
