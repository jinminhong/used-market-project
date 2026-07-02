package com.side.project.domain.item;

import com.side.project.domain.item.itemdto.ItemDto;
import com.side.project.domain.item.itemdto.ItemSaveDto;
import com.side.project.domain.member.MemberService;
import com.side.project.web.SessionConst;
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
    public List<ItemDto> items() {
        List<ItemDto> items = itemService.findAll();
        return items;
    }

    @PostMapping(value = "/items" ,consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addItem(@RequestPart("itemSaveDto") ItemSaveDto itemSaveDto,
                                     @RequestPart("multipartFiles") List<MultipartFile> multipartFiles,
                                                HttpServletRequest request) throws IOException {
        HttpSession session = request.getSession(false);

        if (session == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        LoginMember loginMember = (LoginMember) request.getSession().getAttribute(SessionConst.LOGIN_MEMBER);

        Long itemId = itemService.save(itemSaveDto, loginMember.getLoginId() ,multipartFiles);
        itemSaveDto.setItemId(itemId);

        return ResponseEntity.status(HttpStatus.CREATED).body(itemSaveDto);
    }

    //상품 상세
    @GetMapping("/items/{itemId}")
    public ResponseEntity<ItemDto> item(@PathVariable Long itemId) {
        // TODO: Call itemService.findById(itemId) and add it as "item".
        ItemDto itemDto = itemService.findById(itemId).map(item ->
                new ItemDto(item.getName(), item.getDescription(), item.getPrice(), item.getStatus(), item.getCategory(), item.getMember())).get();
        return ResponseEntity.status(HttpStatus.OK).body(itemDto);
    }


    //상세 상품 수정
    @PatchMapping("/items/{itemId}")
    public ResponseEntity<ItemDto> patchItem(@PathVariable Long itemId ,
                                             @RequestBody ItemDto itemDto) {
        ItemDto updatedItemDto = itemService.update(itemId, itemDto);
        return ResponseEntity.status(HttpStatus.OK).body(updatedItemDto);
    }


    @PostMapping("/items/{itemId}/delete")
    public String delete(@PathVariable Long itemId) {
        // TODO: Call itemService.delete() and redirect to item list.
        return "redirect:/items";
    }
}
