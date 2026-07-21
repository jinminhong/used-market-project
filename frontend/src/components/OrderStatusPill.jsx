import { Badge } from "./ui/badge.jsx";
import { ORDER_STATUS_LABELS } from "../api/constants.js";

const ORDER_STATUS_STYLES = {
  REQUESTED: "bg-white text-neutral-900 border border-neutral-200",
  ACCEPTED: "bg-white text-neutral-900 border border-neutral-200",
  PAY_COMPLETED: "bg-neutral-900 text-white border border-transparent",
  SHIPPING: "bg-neutral-900 text-white border border-transparent",
  COMPLETED: "bg-neutral-400 text-white border border-transparent",
  CANCELED: "bg-neutral-100 text-neutral-500 border border-neutral-200",
};

export default function OrderStatusPill({ status }) {
  return (
    <Badge
      variant="outline"
      className={`status-pill order-status-pill ${status.toLowerCase()} ${ORDER_STATUS_STYLES[status] ?? ORDER_STATUS_STYLES.PAY_COMPLETED}`}
    >
      {ORDER_STATUS_LABELS[status] ?? status}
    </Badge>
  );
}
