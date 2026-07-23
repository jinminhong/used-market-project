import { useEffect, useState } from "react";
import { Button } from "./ui/button.jsx";
import { Input } from "./ui/input.jsx";

export default function PriceOfferDialog({ open, currentPrice, onCancel, onSubmit }) {
  const [price, setPrice] = useState("");

  useEffect(() => {
    if (open) setPrice(currentPrice != null ? String(currentPrice) : "");
  }, [open, currentPrice]);

  if (!open) return null;

  function handleSubmit(event) {
    event.preventDefault();
    const value = Number(price);
    if (!value || value <= 0) return;
    onSubmit(value);
  }

  return (
    <div className="price-offer-overlay" role="dialog" aria-modal="true">
      <form className="price-offer-dialog" onSubmit={handleSubmit}>
        <h2>가격 제안하기</h2>
        {currentPrice != null && (
          <p className="price-offer-current">현재 가격: {currentPrice.toLocaleString()}원</p>
        )}
        <Input
          type="number"
          min="0"
          autoFocus
          value={price}
          onChange={(event) => setPrice(event.target.value)}
          placeholder="제안 가격을 입력하세요"
        />
        <div className="price-offer-actions">
          <Button type="button" variant="ghost" onClick={onCancel}>취소</Button>
          <Button type="submit" disabled={!price}>제안하기</Button>
        </div>
      </form>
    </div>
  );
}
