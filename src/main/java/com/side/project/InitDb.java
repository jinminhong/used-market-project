package com.side.project;

import com.side.project.domain.item.Category;
import com.side.project.domain.item.Item;
import com.side.project.domain.item.ItemStatus;
import com.side.project.domain.itemimage.ItemImage;
import com.side.project.domain.member.Address;
import com.side.project.domain.member.Member;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
public class InitDb implements ApplicationRunner {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Member> members = new ArrayList<>();

        for (int i = 1; i <= 5; i++) {
            Member member = new Member(
                    "user" + i,
                    "member" + i,
                    "1234",
                    "nickname" + i,
                    new Address("city" + i, "street" + i, String.format("%05d", i))
            );

            em.persist(member);
            members.add(member);
        }

        em.flush();

        Category[] categories = Category.values();
        ItemStatus[] statuses = ItemStatus.values();

        for (int i = 1; i <= 50; i++) {
            Member seller = members.get((i - 1) / 10);
            Item item = new Item(
                    "item" + i,
                    "description" + i,
                    1000 + (i * 100),
                    statuses[(i - 1) % statuses.length],
                    categories[(i - 1) % categories.length],
                    seller
            );

            item.addItemImage(new ItemImage("item" + i + "-image1.jpg", "stored-item" + i + "-image1.jpg"));
            item.addItemImage(new ItemImage("item" + i + "-image2.jpg", "stored-item" + i + "-image2.jpg"));

            em.persist(item);
        }
    }
}
