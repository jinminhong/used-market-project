import { sampleItems } from "./constants.js";
import { defaultImage } from "./normalize.js";

export function buildItemFormData(data, dtoPartName, includeEmptyFilePart = false) {
  const itemDto = {
    name: data.name,
    description: data.description,
    price: Number(data.price),
    category: data.category === "All" ? "ETC" : data.category,
  };
  if (dtoPartName === "itemUpdateDto") {
    itemDto.status = data.status;
    itemDto.deletedFileIds = data.deletedFileIds ?? [];
  }
  const formData = new FormData();
  formData.append(dtoPartName, new Blob([JSON.stringify(itemDto)], { type: "application/json" }));
  if (data.imageFiles.length) {
    data.imageFiles.forEach((file) => formData.append("multipartFiles", file));
  } else if (includeEmptyFilePart) {
    formData.append("multipartFiles", new Blob([]), "");
  }
  return formData;
}

export function createApi(useMock) {
  let mockItems = [...sampleItems];
  let mockMember = null;

  async function request(path, options = {}) {
    const method = options.method || "GET";
    const isFormData = typeof FormData !== "undefined" && options.body instanceof FormData;
    const body = options.body && !isFormData ? JSON.parse(options.body) : null;

    if (useMock) return mockRequest(path, method, body);

    const { headers: optionHeaders, ...fetchOptions } = options;
    const headers = isFormData ? optionHeaders : { "Content-Type": "application/json", ...optionHeaders };
    const response = await fetch(`/api${path}`, {
      credentials: "include",
      ...fetchOptions,
      headers,
    });
    if (!response.ok) throw new Error((await response.text()) || `요청 실패 (${response.status})`);
    if (response.status === 204) return null;
    const text = await response.text();
    if (!text) return null;
    try { return JSON.parse(text); } catch { return text; }
  }

  async function mockRequest(path, method, body) {
    await new Promise((resolve) => setTimeout(resolve, 140));
    if (path === "/login" && method === "POST") {
      mockMember = { memberId: body.loginId === "lee" ? 2 : 1, loginId: body.loginId, nickName: body.loginId === "lee" ? "lee" : "hong" };
      return mockMember;
    }
    if (path === "/members" && method === "POST") return body;
    const shopMemberId = Number(path.match(/\/members\/(\d+)\/shop/)?.[1]);
    if (path.startsWith("/members/") && path.endsWith("/shop") && method === "GET") {
      const shopItems = mockItems.filter((item) => item.memberId === shopMemberId);
      const firstItem = shopItems[0];
      return {
        memberId: shopMemberId,
        loginId: `user${shopMemberId}`,
        nickName: firstItem?.nickName ?? `seller${shopMemberId}`,
        name: firstItem?.nickName ?? `seller${shopMemberId}`,
        itemList: shopItems,
      };
    }
    if (path.startsWith("/items") && method === "GET") {
      const params = new URLSearchParams(path.split("?")[1] ?? "");
      const page = Number(params.get("page") ?? 0);
      const size = Number(params.get("size") ?? 10);
      const keyword = (params.get("keyword") ?? "").trim().toLowerCase();
      const category = params.get("category") ?? "";
      const status = params.get("status") ?? "";
      const priceGoe = params.get("priceGoe") ? Number(params.get("priceGoe")) : null;
      const priceLoe = params.get("priceLoe") ? Number(params.get("priceLoe")) : null;

      const filtered = mockItems.filter((item) => {
        if (category && item.category !== category) return false;
        if (status && item.status !== status) return false;
        if (priceGoe != null && item.price < priceGoe) return false;
        if (priceLoe != null && item.price > priceLoe) return false;
        if (keyword && !`${item.name} ${item.description} ${item.nickName}`.toLowerCase().includes(keyword)) return false;
        return true;
      });

      const start = page * size;
      const list = filtered.slice(start, start + size);
      const hasNext = start + size < filtered.length;
      return { list, hasNext, page, size, nextPage: hasNext ? page + 1 : null };
    }
    if (path === "/items" && method === "POST") {
      if (!mockMember) throw new Error("로그인이 필요합니다.");
      const item = { ...body, itemId: Date.now(), price: Number(body.price), status: "SELLING", nickName: mockMember.nickName, imageUrl: body.imageUrl || defaultImage() };
      mockItems = [item, ...mockItems];
      return item;
    }
    if (path === "/members/me" && method === "GET") {
      if (!mockMember) throw new Error("로그인이 필요합니다.");
      return {
        memberId: mockMember.memberId,
        loginId: mockMember.loginId,
        nickName: mockMember.nickName,
        name: mockMember.name ?? mockMember.nickName,
        address: mockMember.address ?? { city: "", street: "", zipcode: "" },
      };
    }
    if (path === "/members/me" && method === "PATCH") {
      if (!mockMember) throw new Error("로그인이 필요합니다.");
      mockMember = {
        ...mockMember,
        nickName: body.nickname ?? mockMember.nickName,
        name: body.name ?? mockMember.name,
        address: body.address ?? mockMember.address,
      };
      return { name: mockMember.name, nickname: mockMember.nickName, address: mockMember.address };
    }
    const itemId = Number(path.match(/\/items\/(\d+)/)?.[1]);
    if (path.startsWith("/items/") && method === "GET") return mockItems.find((item) => item.itemId === itemId) ?? null;
    if (path.startsWith("/items/") && method === "PATCH") {
      mockItems = mockItems.map((item) => item.itemId === itemId ? { ...item, ...body, price: Number(body.price) } : item);
      return mockItems.find((item) => item.itemId === itemId);
    }
    if (path.startsWith("/items/") && method === "DELETE") {
      mockItems = mockItems.filter((item) => item.itemId !== itemId);
      return null;
    }
    throw new Error("정의되지 않은 요청입니다.");
  }

  return {
    login: (data) => request("/login", { method: "POST", body: JSON.stringify(data) }),
    signup: (data) => request("/members", { method: "POST", body: JSON.stringify(data) }),
    findShop: (memberId) => request(`/members/${memberId}/shop`),
    getMyInfo: () => request("/members/me"),
    updateMyInfo: (data) => request("/members/me", { method: "PATCH", body: JSON.stringify(data) }),
    listItems: (page = 0, size = 10, condition = {}) => {
      const params = new URLSearchParams({ page, size });
      const { keyword, category, status, priceGoe, priceLoe } = condition;
      if (keyword) params.set("keyword", keyword);
      if (category && category !== "All") params.set("category", category);
      if (status) params.set("status", status);
      if (priceGoe != null) params.set("priceGoe", priceGoe);
      if (priceLoe != null) params.set("priceLoe", priceLoe);
      return request(`/items?${params.toString()}`);
    },
    findItem: (id) => request(`/items/${id}`),
    createItem: (data) => useMock
      ? request("/items", { method: "POST", body: JSON.stringify({ ...data, imageUrl: data.imagePreview || data.imageUrl }) })
      : request("/items", { method: "POST", body: buildItemFormData(data, "itemSaveDto") }),
    updateItem: (id, data) => useMock
      ? request(`/items/${id}`, { method: "PATCH", body: JSON.stringify({ ...data, imageUrl: data.imagePreview || data.imageUrl }) })
      : request(`/items/${id}`, { method: "PATCH", body: buildItemFormData(data, "itemUpdateDto", true) }),
    deleteItem: (id) => request(`/items/${id}`, { method: "DELETE" }),
  };
}
