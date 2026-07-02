package com.side.project.domain.item;

import com.side.project.domain.item.itemdto.ItemDto;
import com.side.project.domain.item.itemdto.ItemSaveDto;
import com.side.project.domain.itemimage.ItemImage;
import com.side.project.domain.itemimage.ItemImageRepository;
import com.side.project.domain.itemimage.file.FileStore;
import com.side.project.domain.member.Member;
import com.side.project.domain.member.MemberRepository;
import com.side.project.web.exception.member.DuplicateMemberException;
import com.side.project.web.exception.item.ItemException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemService {

    private final ItemRepository itemRepository;
    private final MemberRepository memberRepository;
    private final FileStore fileStore;
    private final ItemImageRepository itemImageRepository;

    @Transactional
    public Long save(ItemSaveDto itemSaveDto, String loginId ,List<MultipartFile> multipartFiles) throws IOException {

        Member findMember = memberRepository.findByLoginId(loginId).orElseThrow(() -> new DuplicateMemberException("존재하지 않는 회원입니다."));

        Item item = new Item(itemSaveDto.getName(), itemSaveDto.getDescription(), itemSaveDto.getPrice(), ItemStatus.SELLING, itemSaveDto.getCategory(), findMember);

        for (MultipartFile multipartFile : multipartFiles) {
            ItemImage itemImage = new ItemImage(multipartFile.getOriginalFilename(), fileStore.storeFile(multipartFile));
            item.addItemImage(itemImage);
        }

        itemRepository.save(item);

        return item.getId();
    }

    public Optional<Item> findById(Long itemId) {
        return itemRepository.findById(itemId);
    }

    public List<ItemDto> findAll() {
        List<Item> result = itemRepository.findAll();
        List<ItemDto> itemDtoResult = result.stream().map(
                item -> new ItemDto(
                        item.getName(), item.getDescription(),item.getPrice(), item.getStatus(), item.getCategory(), item.getMember()))
                .collect(Collectors.toList());
        return itemDtoResult;
    }

    @Transactional
    public void delete(Long itemId) {
        itemRepository.deleteById(itemId);
    }

    @Transactional
    public ItemDto update(Long itemId , ItemDto itemDto) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ItemException("상품을 찾을 수 없습니다. id=" + itemId));
        item.updateItem(itemDto);
        return new ItemDto(item);
    }
}
