import { Badge } from "./ui/badge.jsx";
import { STATUS_LABELS } from "../api/constants.js";

const STATUS_STYLES = {
  SELLING: "bg-white text-neutral-900 border border-neutral-200",
  RESERVED: "bg-neutral-900 text-white border border-transparent",
  SOLD: "bg-neutral-400 text-white border border-transparent",
};

export default function StatusPill({ status }) {
  return (
    <Badge
      variant="outline"
      className={`status-pill ${status.toLowerCase()} ${STATUS_STYLES[status] ?? STATUS_STYLES.SELLING}`}
    >
      {STATUS_LABELS[status] ?? status}
    </Badge>
  );
}
