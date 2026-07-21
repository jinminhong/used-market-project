function storageKey(memberId) {
  return `chat-rooms:${memberId}`;
}

function readRooms(memberId) {
  if (!memberId) return [];
  try {
    const raw = window.localStorage.getItem(storageKey(memberId));
    const parsed = raw ? JSON.parse(raw) : [];
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

function writeRooms(memberId, rooms) {
  if (!memberId) return;
  try {
    window.localStorage.setItem(storageKey(memberId), JSON.stringify(rooms));
  } catch {
    // localStorage 사용 불가 환경에서는 조용히 무시(채팅 목록 저장만 실패, 앱 동작에는 영향 없음)
  }
}

function sortByLastMessage(rooms) {
  return [...rooms].sort((a, b) => {
    const aTime = a.lastMessageAt ? new Date(a.lastMessageAt).getTime() : 0;
    const bTime = b.lastMessageAt ? new Date(b.lastMessageAt).getTime() : 0;
    return bTime - aTime;
  });
}

export function getChatRooms(memberId) {
  return sortByLastMessage(readRooms(memberId));
}

export function getChatRoom(memberId, roomId) {
  const targetId = Number(roomId);
  return readRooms(memberId).find((room) => Number(room.roomId) === targetId) ?? null;
}

export function upsertChatRoom(memberId, roomSummary) {
  if (!memberId || !roomSummary?.roomId) return;
  const rooms = readRooms(memberId);
  const index = rooms.findIndex((room) => Number(room.roomId) === Number(roomSummary.roomId));
  if (index === -1) {
    writeRooms(memberId, [{ createdAt: new Date().toISOString(), lastMessage: null, lastMessageAt: null, ...roomSummary }, ...rooms]);
    return;
  }
  const merged = { ...rooms[index], ...roomSummary };
  const next = [...rooms];
  next[index] = merged;
  writeRooms(memberId, next);
}

export function updateLastMessage(memberId, roomId, { content, sentAt }) {
  if (!memberId) return;
  const rooms = readRooms(memberId);
  const index = rooms.findIndex((room) => Number(room.roomId) === Number(roomId));
  if (index === -1) return;
  const updated = { ...rooms[index], lastMessage: content, lastMessageAt: sentAt };
  const next = rooms.filter((_, i) => i !== index);
  next.unshift(updated);
  writeRooms(memberId, next);
}
