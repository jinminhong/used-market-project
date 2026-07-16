import { useEffect, useRef, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { Search } from "lucide-react";
import { useSession } from "../context/SessionContext.jsx";
import { normalizeItem } from "../api/normalize.js";
import ItemCard from "../components/ItemCard.jsx";
import CategoryTabs from "../components/CategoryTabs.jsx";

function readItemSlice(data, page, size) {
  const list = Array.isArray(data) ? data : data?.list ?? data?.items ?? data?.content ?? [];
  return {
    list,
    hasNext: Array.isArray(data) ? list.length >= size : Boolean(data?.hasNext),
    nextPage: Array.isArray(data) ? page + 1 : data?.nextPage ?? page + 1,
  };
}

export default function Home() {
  const { api, setNotice, loading, setLoading } = useSession();
  const [searchParams, setSearchParams] = useSearchParams();
  const category = searchParams.get("category") ?? "All";
  const search = searchParams.get("q") ?? "";

  const [items, setItems] = useState([]);
  const [itemPage, setItemPage] = useState(0);
  const [hasNextItems, setHasNextItems] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);

  const apiRef = useRef(null);
  const inFlightItemRequestRef = useRef("");
  const loadedItemPagesRef = useRef(new Set());
  const searchInputRef = useRef(null);
  const isFirstFilterRunRef = useRef(true);

  async function loadItems(page = 0, append = false) {
    const size = 10;
    const requestKey = `${append ? "append" : "replace"}:${page}:${size}`;

    if (append && !hasNextItems) return;
    if (loadedItemPagesRef.current.has(page)) return;
    if (inFlightItemRequestRef.current === requestKey) return;
    inFlightItemRequestRef.current = requestKey;

    if (append) setLoadingMore(true);
    else setLoading(true);

    try {
      const data = await api.listItems(page, size, { keyword: search, category });
      const { list, hasNext, nextPage } = readItemSlice(data, page, size);
      const normalizedItems = list.map((item, index) => normalizeItem(item, page * size + index + 1));
      let addedCount = normalizedItems.length;

      loadedItemPagesRef.current.add(page);
      setItems((current) => {
        if (!append) return normalizedItems;
        const currentIds = new Set(current.map((item) => item.itemId));
        const nextItems = normalizedItems.filter((item) => !currentIds.has(item.itemId));
        addedCount = nextItems.length;
        return [...current, ...nextItems];
      });
      setHasNextItems(append && addedCount === 0 ? false : hasNext);
      setItemPage(nextPage);
    } catch (error) {
      setNotice(error.message || "상품을 불러오지 못했습니다.");
    } finally {
      if (inFlightItemRequestRef.current === requestKey) inFlightItemRequestRef.current = "";
      if (append) setLoadingMore(false);
      else setLoading(false);
    }
  }

  useEffect(() => {
    if (apiRef.current === api) return;
    apiRef.current = api;
    inFlightItemRequestRef.current = "";
    loadedItemPagesRef.current = new Set();
    setItemPage(0);
    setHasNextItems(true);
    loadItems(0, false);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [api]);

  useEffect(() => {
    if (isFirstFilterRunRef.current) {
      isFirstFilterRunRef.current = false;
      return;
    }
    const timer = setTimeout(() => {
      inFlightItemRequestRef.current = "";
      loadedItemPagesRef.current = new Set();
      setItemPage(0);
      setHasNextItems(true);
      loadItems(0, false);
    }, 300);
    return () => clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [category, search]);

  useEffect(() => {
    function handleScroll() {
      const scrollBottom = window.innerHeight + window.scrollY;
      const documentHeight = document.documentElement.scrollHeight;
      const shouldLoadMore = documentHeight - scrollBottom < 500;

      if (shouldLoadMore && hasNextItems && !loadingMore && !loading) {
        loadItems(itemPage, true);
      }
    }

    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hasNextItems, loadingMore, loading, itemPage, api]);

  useEffect(() => {
    if (searchParams.get("focus") !== "search") return;
    searchInputRef.current?.focus();
    const next = new URLSearchParams(searchParams);
    next.delete("focus");
    setSearchParams(next, { replace: true });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function updateParam(key, value) {
    const next = new URLSearchParams(searchParams);
    if (value) next.set(key, value);
    else next.delete(key);
    setSearchParams(next, { replace: true });
  }

  return (
    <main className="page-shell">
      <section className="hero-strip">
        <div>
          <p>Latest vintage drops</p>
          <h1>개성 있는 셀러들의 상품을 둘러보세요</h1>
        </div>
        <div className="hero-count"><strong>{items.length}</strong><span>items</span></div>
      </section>
      <section className="shop-toolbar">
        <CategoryTabs value={category} onChange={(name) => updateParam("category", name === "All" ? "" : name)} />
        <div className="search-field">
          <Search size={16} />
          <input
            ref={searchInputRef}
            value={search}
            onChange={(event) => updateParam("q", event.target.value)}
            placeholder="상품, 설명, 셀러 검색"
          />
        </div>
      </section>
      <section className="product-grid">
        {items.map((item, index) => (
          <ItemCard key={`${item.itemId ?? item.name}-${index}`} item={item} />
        ))}
      </section>
      {items.length === 0 && <p className="quiet-message">조건에 맞는 상품이 없습니다.</p>}
      {loadingMore && <p className="quiet-message">상품을 더 불러오는 중입니다.</p>}
      {!hasNextItems && items.length > 0 && <p className="quiet-message">마지막 상품까지 모두 봤습니다.</p>}
    </main>
  );
}
