package com.side.project.domain.chat.chatroom;

import com.side.project.domain.chat.chatmessage.dto.PageMessageResponseDto;
import com.side.project.domain.chat.chatmessage.repository.ChatMessageRepository;
import com.side.project.domain.chat.chatmessage.dto.ChatMessageResponse;
import com.side.project.domain.chat.chatroom.dto.ChatRoomRequest;
import com.side.project.domain.chat.chatroom.dto.ChatRoomResponse;
import com.side.project.domain.chat.chatroom.dto.PageChatRoomResponse;
import com.side.project.web.argumentresolver.Login;
import com.side.project.web.login.LoginMember;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chatRooms")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @PostMapping("")
    public ResponseEntity<ChatRoomResponse> createChatRoom(@RequestBody ChatRoomRequest request,
                                                              @Login LoginMember loginMember) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatRoomService.createChatRoom(request.itemId(), loginMember.getMemberId()));
    }

    @GetMapping("/me")
    public ResponseEntity<PageChatRoomResponse> getChatRooms(@Login LoginMember loginMember,
                                          Pageable pageable) {
        PageChatRoomResponse chatRooms = chatRoomService.getRooms(loginMember.getMemberId(), pageable);
        return ResponseEntity.ok(chatRooms);
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<PageMessageResponseDto> getMessages(
            @PathVariable Long roomId,
            Pageable pageable,
            @Login LoginMember loginMember) {
        PageMessageResponseDto messages = chatRoomService.getMessages(roomId, loginMember.getMemberId(), pageable);
        return ResponseEntity.ok(messages);
    }
}
