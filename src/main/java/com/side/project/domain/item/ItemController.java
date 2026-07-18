package com.side.project.domain.item;

import com.side.project.domain.item.itemdto.*;
import com.side.project.web.argumentresolver.Login;
import com.side.project.web.login.LoginMember;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping("/items")
    public ResponseEntity<PageResponseDto> items(@RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "10") int size,
                                                 @ModelAttribute ItemSearchCondition condition) {
        PageResponseDto itemSlice = itemService.searchItems(condition, page, size);
        return ResponseEntity.status(HttpStatus.OK).body(itemSlice);
    }

    @PostMapping(value = "/items" ,consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> saveItem(@RequestPart("itemSaveDto") ItemSaveDto itemSaveDto,
                                      @RequestPart("multipartFiles") List<MultipartFile> multipartFiles,
                                      @Login LoginMember loginMember) throws IOException {
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
                                             @RequestPart(value = "multipartFiles", required = false) List<MultipartFile> multipartFiles
                                       ) throws IOException {

        ItemDto updatedItemDto = itemService.update(itemId, itemUpdateDto, multipartFiles, loginMember.getLoginId());
        return ResponseEntity.status(HttpStatus.OK).body(updatedItemDto);
    }


    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<String> delete(@PathVariable Long itemId,
                                         @Login LoginMember loginMember) {
        itemService.delete(itemId , loginMember);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("삭제 완료");
    }
}
