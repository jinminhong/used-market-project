package com.side.project.domain.item;

import com.side.project.domain.item.itemdto.ItemDto;
import com.side.project.domain.item.itemdto.ItemResponseDto;
import com.side.project.domain.item.itemdto.ItemSaveDto;
import com.side.project.domain.item.itemdto.ItemUpdateDto;
import com.side.project.domain.member.MemberService;
import com.side.project.web.SessionConst;
import com.side.project.web.argumentresolver.Login;
import com.side.project.web.login.LoginMember;
import com.side.project.web.login.LoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;
    private final MemberService memberService;
    private final LoginService loginService;

    @GetMapping("/items")
    public ResponseEntity<List<ItemResponseDto>> items() {
        List<ItemResponseDto> items = itemService.findItemSlice();
        return ResponseEntity.status(HttpStatus.OK).body(items);
    }

    @PostMapping(value = "/items" ,consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> saveItem(@RequestPart("itemSaveDto") ItemSaveDto itemSaveDto,
                                      @RequestPart("multipartFiles") List<MultipartFile> multipartFiles,
                                      @Login LoginMember loginMember,
                                                HttpServletRequest request) throws IOException {
        Long itemId = itemService.save(itemSaveDto, loginMember.getLoginId() ,multipartFiles);
        itemSaveDto.setItemId(itemId);


        return ResponseEntity.status(HttpStatus.CREATED).body(itemSaveDto);
    }

    //상품 상세
    @GetMapping("/items/{itemId}")
    public ResponseEntity<ItemDto> item(@PathVariable Long itemId) {
        // TODO: Call itemService.findById(itemId) and add it as "item".
        ItemDto itemDto = itemService.findByIdToDto(itemId);
        return ResponseEntity.status(HttpStatus.OK).body(itemDto);
    }

    //상세 상품 수정
    @PatchMapping(value = "/items/{itemId}" ,consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> patchItem(@PathVariable Long itemId ,
                                             @Login LoginMember loginMember,
                                             @RequestPart("itemUpdateDto") ItemUpdateDto itemUpdateDto,
                                             @RequestPart(value = "multipartFiles", required = false) List<MultipartFile> multipartFiles,
                                             HttpServletRequest request) throws IOException {

        ItemDto updatedItemDto = itemService.update(itemId, itemUpdateDto, multipartFiles, loginMember.getLoginId());
        return ResponseEntity.status(HttpStatus.OK).body(updatedItemDto);
    }


    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<String> delete(@PathVariable Long itemId,
                                         @Login LoginMember loginMember,
                                         HttpServletRequest request) {
        itemService.delete(itemId , loginMember);
        return ResponseEntity.ok("삭제 완료");
    }
}
