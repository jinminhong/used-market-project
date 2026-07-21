import { Button } from "./ui/button.jsx";

const OFFER_STATUS_LABELS = {
  PENDING: "제안중",
  ACCEPTED: "수락됨",
  REJECTED: "거절됨",
};

function formatTime(iso) {
  if (!iso) return "";
  const date = new Date(iso);
  return date.toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" });
}

export default function ChatBubble({ message, isMine, isSeller, onAcceptOffer, onRejectOffer }) {
  if (message.type === "OFFER") {
    const canRespond = isSeller && message.offerStatus === "PENDING";
    return (
      <div className={`chat-bubble-row ${isMine ? "mine" : "theirs"}`}>
        {!isMine && <em className="chat-bubble-sender">{message.senderNickname}</em>}
        <div className="chat-offer-card">
          <span className="chat-offer-label">가격 제안</span>
          <strong className="chat-offer-price">{(message.offeredPrice ?? 0).toLocaleString()}원</strong>
          <span className={`chat-offer-status ${(message.offerStatus ?? "PENDING").toLowerCase()}`}>
            {OFFER_STATUS_LABELS[message.offerStatus] ?? OFFER_STATUS_LABELS.PENDING}
          </span>
          {canRespond && (
            <div className="chat-offer-actions">
              <Button size="sm" onClick={() => onAcceptOffer?.(message.messageId)}>수락</Button>
              <Button size="sm" variant="outline" onClick={() => onRejectOffer?.(message.messageId)}>거절</Button>
            </div>
          )}
        </div>
        <span className="chat-bubble-meta">{formatTime(message.sentAt)}</span>
      </div>
    );
  }

  return (
    <div className={`chat-bubble-row ${isMine ? "mine" : "theirs"}`}>
      {!isMine && <em className="chat-bubble-sender">{message.senderNickname}</em>}
      <div className={`chat-bubble ${message.pending ? "pending" : ""} ${message.failed ? "failed" : ""}`}>
        <p>{message.content}</p>
      </div>
      <span className="chat-bubble-meta">
        {message.failed ? "전송 실패" : message.pending ? "전송 중..." : formatTime(message.sentAt)}
      </span>
    </div>
  );
}
