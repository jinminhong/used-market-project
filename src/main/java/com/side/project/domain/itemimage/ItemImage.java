package com.side.project.domain.itemimage;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.side.project.domain.item.Item;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class ItemImage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String originalFilename;

    private String storedFilename;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    public ItemImage(String originalFilename, String storedFilename) {
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
    }

    public void changeItem(Item item) {
        this.item = item;
    }
}
