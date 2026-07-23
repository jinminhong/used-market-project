package com.side.project.domain.chat.chatroom;

import com.side.project.domain.chat.chatmessage.dto.PageMessageResponseDto;
import com.side.project.domain.chat.chatmessage.repository.ChatMessageRepository;
import com.side.project.domain.chat.chatmessage.dto.ChatMessageResponse;
import com.side.project.domain.chat.chatroom.dto.ChatRoomAndMessageDto;
import com.side.project.domain.chat.chatroom.dto.ChatRoomRequest;
import com.side.project.domain.chat.chatroom.dto.ChatRoomResponse;
import com.side.project.domain.chat.chatroom.dto.PageChatRoomResponse;
import com.side.project.web.argumentresolver.Login;
import com.side.project.web.login.LoginMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/rooms")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final SimpMessagingTemplate messagingTemplate;

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

    @PostMapping("/offer")
    public ResponseEntity<ChatRoomAndMessageDto> createOffer(@Valid @RequestBody ChatRoomRequest request,
                                                             @Login LoginMember loginMember) {

        ChatRoomAndMessageDto response = chatRoomService.createOffer(request.itemId(), loginMember.getMemberId(), request);

        messagingTemplate.convertAndSend(
                "/topic/chat/rooms/" + response.room().roomId(),
                response
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{roomId}/offer/{messageId}/reject")
    public ResponseEntity<ChatRoomAndMessageDto> rejectOffer(@Login LoginMember loginMember,
                                                             @PathVariable Long roomId,
                                                             @PathVariable Long messageId) {
        ChatRoomAndMessageDto response = chatRoomService.rejectOffer(roomId, loginMember.getMemberId(), messageId);

        messagingTemplate.convertAndSend(
                "/topic/chat/rooms/" + response.room().roomId(),
                response
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{roomId}/offer/{messageId}/accept")
    public ResponseEntity<ChatRoomAndMessageDto> acceptOffer(@Login LoginMember loginMember,
                                                             @PathVariable Long roomId,
                                                             @PathVariable Long messageId) {
        ChatRoomAndMessageDto response = chatRoomService.acceptOffer(roomId, loginMember.getMemberId(), messageId);

        messagingTemplate.convertAndSend(
                "/topic/chat/rooms/" + response.room().roomId(),
                response
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
