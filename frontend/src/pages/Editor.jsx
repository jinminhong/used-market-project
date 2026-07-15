import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { ImagePlus, X } from "lucide-react";
import { useSession } from "../context/SessionContext.jsx";
import { CATEGORIES, STATUSES } from "../api/constants.js";
import { normalizeItem } from "../api/normalize.js";

const emptyForm = { name: "", description: "", price: "", status: "SELLING", category: "ETC", imageUrl: "", imageFiles: [], imagePreview: "", itemImages: [], deletedFileIds: [] };

export default function Editor() {
  const { itemId } = useParams();
  const isEdit = Boolean(itemId);
  const navigate = useNavigate();
  const { api, member, run, loading, setNotice } = useSession();
  const [form, setForm] = useState(emptyForm);
  const [ready, setReady] = useState(!isEdit);

  useEffect(() => {
    if (!isEdit) {
      setForm(emptyForm);
      setReady(true);
      return;
    }

    let cancelled = false;

    (async () => {
      try {
        const detail = await api.findItem(Number(itemId));
        const item = normalizeItem(detail, Number(itemId));
        if (cancelled) return;
        if (item.memberId !== member?.memberId) {
          navigate(`/items/${itemId}`, { replace: true });
          return;
        }
        setForm({
          name: item.name,
          description: item.description,
          price: String(item.price),
          status: item.status,
          category: item.category,
          imageUrl: item.imageUrl,
          imageFiles: [],
          imagePreview: "",
          itemImages: item.itemImages ?? [],
          deletedFileIds: [],
        });
        setReady(true);
      } catch (error) {
        if (cancelled) return;
        setNotice(error.message || "상품을 불러오지 못했습니다.");
        navigate("/", { replace: true });
      }
    })();

    return () => { cancelled = true; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isEdit, itemId, api]);

  function change(event) {
    const { name, value, files } = event.target;
    if (name === "imageFiles") {
      const imageFiles = Array.from(files ?? []);
      setForm((current) => {
        if (current.imagePreview?.startsWith("blob:")) URL.revokeObjectURL(current.imagePreview);
        return { ...current, imageFiles, imagePreview: imageFiles[0] ? URL.createObjectURL(imageFiles[0]) : "" };
      });
      return;
    }
    setForm((current) => ({ ...current, [name]: value }));
  }

  function toggleDeleteImage(fileId) {
    setForm((current) => ({
      ...current,
      deletedFileIds: current.deletedFileIds.includes(fileId)
        ? current.deletedFileIds.filter((id) => id !== fileId)
        : [...current.deletedFileIds, fileId],
    }));
  }

  async function submit(event) {
    event.preventDefault();
    if (isEdit) {
      await run(async () => {
        const updated = normalizeItem(await api.updateItem(Number(itemId), { ...form, price: Number(form.price) }), Number(itemId));
        navigate(`/items/${updated.itemId}`);
      }, "상품이 수정되었습니다.");
    } else {
      await run(async () => {
        if (!form.name.trim() || !form.price) throw new Error("상품명과 가격을 입력해주세요.");
        if (!form.imageFiles.length) throw new Error("상품 이미지를 1장 이상 선택해주세요.");
        const created = normalizeItem(await api.createItem({ ...form, price: Number(form.price) }));
        navigate(`/items/${created.itemId}`);
      }, "상품이 등록되었습니다.");
    }
  }

  if (!ready) return null;

  return (
    <main className="form-page">
      <section className="form-panel wide-panel">
        <p>Seller studio</p>
        <h1>{isEdit ? "상품 수정" : "상품 등록"}</h1>
        <form onSubmit={submit}>
          <input name="name" value={form.name} onChange={change} placeholder="상품명" />
          <div className="split-fields">
            <input name="price" type="number" min="0" value={form.price} onChange={change} placeholder="가격" />
            <select name="category" value={form.category} onChange={change}>
              {CATEGORIES.filter((name) => name !== "All").map((name) => <option key={name} value={name}>{name}</option>)}
            </select>
          </div>
          {form.itemImages.length > 0 && (
            <div className="existing-images">
              {form.itemImages.map((image) => {
                const fileId = image.itemImageId;
                const marked = form.deletedFileIds.includes(fileId);
                return (
                  <div key={fileId ?? image.storedFileName} className={`existing-image${marked ? " marked-delete" : ""}`}>
                    <img src={`/api/images/${encodeURIComponent(image.storedFileName ?? image.storedFilename)}`} alt="" />
                    <span>{image.originalFilename}</span>
                    <button type="button" className="existing-image-remove" onClick={() => toggleDeleteImage(fileId)} aria-label="이미지 삭제 표시">
                      <X size={14} />
                    </button>
                  </div>
                );
              })}
            </div>
          )}
          <label className="file-drop">
            <input name="imageFiles" type="file" accept="image/*" multiple onChange={change} />
            <ImagePlus size={20} />
            <span>{form.imageFiles.length ? `${form.imageFiles.length}개 이미지 선택됨` : isEdit ? "새 이미지 추가" : "상품 이미지 선택"}</span>
            {form.imagePreview && <img src={form.imagePreview} alt="" />}
          </label>
          <textarea name="description" value={form.description} onChange={change} placeholder="상품 설명" />
          <div className="split-fields">
            <select name="status" value={form.status} onChange={change}>
              {STATUSES.map((status) => <option key={status} value={status}>{status}</option>)}
            </select>
            <button disabled={loading}>저장</button>
          </div>
        </form>
        <button className="text-button" type="button" onClick={() => navigate(isEdit ? `/items/${itemId}` : "/")}>취소</button>
      </section>
    </main>
  );
}
