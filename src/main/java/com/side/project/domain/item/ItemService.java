package com.side.project.domain.item;

import com.side.project.domain.item.itemdto.*;
import com.side.project.domain.itemimage.ItemImage;
import com.side.project.domain.itemimage.ItemImageRepository;
import com.side.project.domain.itemimage.file.FileStore;
import com.side.project.domain.itemimage.file.UploadFile;
import com.side.project.domain.member.Member;
import com.side.project.domain.member.MemberRepository;
import com.side.project.web.exception.login.UnauthorizedException;
import com.side.project.web.exception.member.DuplicateMemberException;
import com.side.project.web.exception.item.ItemException;
import com.side.project.web.exception.member.MemberException;
import com.side.project.web.login.LoginMember;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
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

        if (multipartFiles != null && !multipartFiles.isEmpty()) {
            List<UploadFile> uploadFiles = fileStore.storeFiles(multipartFiles);

            List<ItemImage> itemImageList = uploadFiles.stream()
                    .map(uploadFile -> new ItemImage(uploadFile.getOriginalFilename(), uploadFile.getStoredFileName()))
                    .collect(Collectors.toList());

            for (ItemImage itemImage : itemImageList) {
                item.addItemImage(itemImage);
            }
            itemSaveDto.setUploadFiles(uploadFiles);
        }

        itemRepository.save(item);


        return item.getId();
    }

    public Optional<Item> findById(Long itemId) {
        return itemRepository.findById(itemId);
    }

    public Optional<Item> findByIdWithMember(Long itemId){
        return itemRepository.findByIdWithMember(itemId);
    }

    public ItemDto findByIdToDto(Long itemId) {
         return findByIdWithMember(itemId).map(ItemDto::new).orElseThrow(() -> new ItemException("상품을 찾을 수 없습니다"));
    }

    public List<ItemDto> findAll() {
        List<Item> result = itemRepository.findAll();
        return result.stream().map(
                        ItemDto::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long itemId , LoginMember loginMember) {
        Item item = itemRepository.findByIdWithMember(itemId).orElseThrow(() -> new ItemException("상품을 찾을 수 없습니다"));
        if (!item.getMember().getLoginId().equals(loginMember.getLoginId())) {
            throw new UnauthorizedException("상품을 삭제할 권한이 없습니다");
        }
        itemRepository.delete(item);
    }

    @Transactional
    public ItemDto update(Long itemId , ItemUpdateDto itemUpdateDto ,List<MultipartFile> multipartFiles, String loginId) throws IOException {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ItemException("상품을 찾을 수 없습니다. id=" + itemId));

        if (!item.getMember().getLoginId().equals(loginId)) {
            throw new ItemException("상품을 수정할 권한이 없습니다");
        }
        List<Long> deletedFileIds = itemUpdateDto.getDeletedFileIds();

        if (deletedFileIds != null && !deletedFileIds.isEmpty()) {
            List<ItemImage> itemImageToDelete = item.getItemImages().stream()
                    .filter(itemImage -> deletedFileIds.contains(itemImage.getId()))
                    .toList();
            for (ItemImage itemImage : itemImageToDelete) {
                item.removeItemImage(itemImage);
            }
        }

        if (multipartFiles != null && !multipartFiles.isEmpty()) {
            List<UploadFile> uploadFiles = fileStore.storeFiles(multipartFiles);

            List<ItemImage> itemImageList = uploadFiles.stream()
                    .map(uploadFile -> new ItemImage(uploadFile.getOriginalFilename(), uploadFile.getStoredFileName()))
                    .toList();

            for (ItemImage itemImage : itemImageList) {
                item.addItemImage(itemImage);
            }
        }

        item.updateItem(itemUpdateDto);
        return new ItemDto(item);
    }

    public PageResponseDto findItemSlice(int page , int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Slice<Item> items = itemRepository.findAllSlice(pageRequest);
        boolean hasNext = items.hasNext();
        List<ItemResponseDto> list = items.getContent()
                .stream()
                .map(ItemResponseDto::new)
                .toList();
        return new PageResponseDto(list,hasNext);
    }
}
