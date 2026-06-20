import { useEffect, useMemo, useState } from "react";

const API_BASE_URL = "/api";

const sampleItems = [
  {
    id: 1,
    title: "MacBook Pro 14",
    description: "Good condition, normal operation.",
    price: 1200000,
    status: "SELLING",
    sellerId: 1,
  },
  {
    id: 2,
    title: "Wireless Keyboard",
    description: "Almost new, box included.",
    price: 45000,
    status: "RESERVED",
    sellerId: 1,
  },
];

function createItemApi({ useMock, setLastRequest }) {
  let mockItems = [...sampleItems];

  async function request(path, options = {}) {
    const method = options.method || "GET";
    const body = options.body ? JSON.parse(options.body) : null;
    const url = `${API_BASE_URL}${path}`;

    setLastRequest({ method, url, body });

    if (useMock) {
      return mockRequest(path, method, body);
    }

    const response = await fetch(url, {
      headers: {
        "Content-Type": "application/json",
        ...options.headers,
      },
      ...options,
    });

    if (!response.ok) {
      const errorBody = await response.json().catch(() => ({
        message: "API request failed.",
      }));
      throw new Error(errorBody.message || "API request failed.");
    }

    if (response.status === 204) {
      return null;
    }

    return response.json();
  }

  async function mockRequest(path, method, body) {
    await new Promise((resolve) => setTimeout(resolve, 200));

    if (path === "/items" && method === "GET") {
      return { items: mockItems, total: mockItems.length };
    }

    if (path === "/items" && method === "POST") {
      const item = {
        id: Date.now(),
        title: body.title,
        description: body.description,
        price: Number(body.price),
        status: "SELLING",
        sellerId: Number(body.sellerId),
      };
      mockItems = [item, ...mockItems];
      return item;
    }

    const itemId = Number(path.replace("/items/", ""));

    if (path.startsWith("/items/") && method === "PATCH") {
      mockItems = mockItems.map((item) =>
        item.id === itemId ? { ...item, ...body, price: Number(body.price) } : item
      );
      return mockItems.find((item) => item.id === itemId);
    }

    if (path.startsWith("/items/") && method === "DELETE") {
      mockItems = mockItems.filter((item) => item.id !== itemId);
      return null;
    }

    throw new Error("Mock API route is not defined.");
  }

  return {
    listItems: () => request("/items"),
    createItem: (data) =>
      request("/items", {
        method: "POST",
        body: JSON.stringify(data),
      }),
    updateItem: (id, data) =>
      request(`/items/${id}`, {
        method: "PATCH",
        body: JSON.stringify(data),
      }),
    deleteItem: (id) =>
      request(`/items/${id}`, {
        method: "DELETE",
      }),
  };
}

export default function App() {
  const [useMock, setUseMock] = useState(true);
  const [items, setItems] = useState([]);
  const [form, setForm] = useState({
    title: "",
    description: "",
    price: "",
    sellerId: 1,
  });
  const [lastRequest, setLastRequest] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const api = useMemo(
    () => createItemApi({ useMock, setLastRequest }),
    [useMock]
  );

  async function run(action) {
    setLoading(true);
    setError("");
    try {
      await action();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function loadItems() {
    await run(async () => {
      const data = await api.listItems();
      setItems(data.items);
    });
  }

  async function submitItem(event) {
    event.preventDefault();

    if (!form.title.trim() || !form.price) {
      setError("Title and price are required.");
      return;
    }

    await run(async () => {
      const created = await api.createItem(form);
      setItems((current) => [created, ...current]);
      setForm({ title: "", description: "", price: "", sellerId: 1 });
    });
  }

  async function toggleReserved(item) {
    await run(async () => {
      const updated = await api.updateItem(item.id, {
        title: item.title,
        description: item.description,
        price: item.price,
        status: item.status === "RESERVED" ? "SELLING" : "RESERVED",
      });
      setItems((current) =>
        current.map((target) => (target.id === updated.id ? updated : target))
      );
    });
  }

  async function deleteItem(itemId) {
    await run(async () => {
      await api.deleteItem(itemId);
      setItems((current) => current.filter((item) => item.id !== itemId));
    });
  }

  function changeForm(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  }

  useEffect(() => {
    loadItems();
  }, [api]);

  return (
    <main className="page">
      <header className="topbar">
        <div>
          <p className="eyebrow">Used Market</p>
          <h1>React REST API Frontend</h1>
        </div>

        <label className="toggle">
          <input
            type="checkbox"
            checked={useMock}
            onChange={(event) => setUseMock(event.target.checked)}
          />
          Mock API
        </label>
      </header>

      <section className="layout">
        <div className="main-panel">
          <form className="item-form" onSubmit={submitItem}>
            <input
              name="title"
              value={form.title}
              onChange={changeForm}
              placeholder="Item title"
            />
            <input
              name="price"
              type="number"
              value={form.price}
              onChange={changeForm}
              placeholder="Price"
            />
            <textarea
              name="description"
              value={form.description}
              onChange={changeForm}
              placeholder="Description"
            />
            <button disabled={loading}>Create Item</button>
          </form>

          {error && <p className="error">{error}</p>}

          <ul className="items">
            {items.map((item) => (
              <li key={item.id}>
                <div>
                  <strong>{item.title}</strong>
                  <p>{item.description}</p>
                  <span>{item.price.toLocaleString()} KRW</span>
                </div>

                <div className="actions">
                  <span className={`badge ${item.status.toLowerCase()}`}>
                    {item.status}
                  </span>
                  <button type="button" onClick={() => toggleReserved(item)}>
                    Toggle Reserved
                  </button>
                  <button
                    type="button"
                    className="ghost"
                    onClick={() => deleteItem(item.id)}
                  >
                    Delete
                  </button>
                </div>
              </li>
            ))}
          </ul>
        </div>

        <aside className="api-panel">
          <h2>Last API Request</h2>
          {lastRequest ? (
            <pre>{JSON.stringify(lastRequest, null, 2)}</pre>
          ) : (
            <p>No request yet.</p>
          )}

          <h2>Development Proxy</h2>
          <code>React 5173 /api -&gt; Spring 8080 /api</code>
        </aside>
      </section>
    </main>
  );
}
