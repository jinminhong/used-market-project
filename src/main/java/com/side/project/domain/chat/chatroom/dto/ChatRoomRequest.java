package com.side.project.domain.chat.chatroom.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChatRoomRequest(
        Long itemId,
        @NotBlank(message = "메세지 내용을 입력해야 합니다.") String content,
        @NotNull(message = "제안 가격을 입력해야 합니다.")
        @Min(value = 1000, message = "네고 가격제안은 1000원 이상부터 입니다") Integer offeredPrice
) {
}
