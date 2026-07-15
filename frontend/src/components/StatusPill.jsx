import { STATUS_LABELS } from "../api/constants.js";

export default function StatusPill({ status }) {
  return <span className={`status-pill ${status.toLowerCase()}`}>{STATUS_LABELS[status] ?? status}</span>;
}
