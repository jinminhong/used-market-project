package com.side.project.domain.member.memberdto;

import com.side.project.domain.item.itemdto.ItemResponseDto;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ShopInfoDto {

    private String nickName;

    private String name;

    private List<ItemResponseDto> itemList = new ArrayList<>();
}
