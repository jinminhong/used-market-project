import { Link } from "react-router-dom";
import StatusPill from "./StatusPill.jsx";

export default function ItemCard({ item }) {
  return (
    <Link to={`/items/${item.itemId}`} className="item-card" data-status={item.status}>
      <span className="item-card-image">
        <img src={item.imageUrl} alt={item.name} loading="lazy" />
        <StatusPill status={item.status} />
      </span>
      <span className="item-card-body">
        <strong className="item-card-name">{item.name}</strong>
        <span className="item-card-price">{item.price.toLocaleString()}원</span>
        <em className="item-card-seller">@{item.nickName}</em>
      </span>
    </Link>
  );
}
