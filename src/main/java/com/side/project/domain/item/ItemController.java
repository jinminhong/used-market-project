package com.side.project.domain.item;

import com.side.project.domain.member.MemberService;
import com.side.project.web.SessionConst;
import com.side.project.web.login.LoginMember;
import com.side.project.web.login.LoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
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

    @PostMapping("/items")
    public ResponseEntity<?> addItem(@RequestBody ItemSaveForm itemSaveForm ,
                                                HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        LoginMember loginMember = (LoginMember) request.getSession().getAttribute(SessionConst.LOGIN_MEMBER);

        Long itemId = itemService.save(itemSaveForm, loginMember.getLoginId());
        itemSaveForm.setItemId(itemId);

        return ResponseEntity.status(HttpStatus.CREATED).body(itemSaveForm);
    }

    //상품 상세
    @GetMapping("/items/{itemId}")
    public ResponseEntity<ItemDto> item(@PathVariable Long itemId) {
        // TODO: Call itemService.findById(itemId) and add it as "item".
        ItemDto itemDto = itemService.findById(itemId).map(item ->
                new ItemDto(item.getName(), item.getDescription(), item.getPrice(), item.getStatus(), item.getMember())).get();
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
