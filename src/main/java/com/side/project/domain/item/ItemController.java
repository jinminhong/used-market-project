package com.side.project.domain.item;

import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping("/items")
    public String items(Model model) {
        // TODO: Call itemService.findAll() and add it as "items".
        return "items/items";
    }

    @PostMapping("/items")
    public ItemSaveForm addItem(@RequestBody ItemSaveForm itemSaveForm) {


        return itemSaveForm;
    }

    @GetMapping("/items/{itemId}")
    public String item(@PathVariable Long itemId, Model model) {
        // TODO: Call itemService.findById(itemId) and add it as "item".
        return "items/item";
    }

    @GetMapping("/items/add")
    public String addForm(@ModelAttribute("itemSaveForm") ItemSaveForm form) {
        // TODO: Prepare the itemSaveForm object for the item form page.
        return "items/addItemForm";
    }

    @PostMapping("/items/add")
    public String save(@ModelAttribute("itemSaveForm") ItemSaveForm form) {
        // TODO:
        // 1. Move form values to Item.
        // 2. Call itemService.save().
        // 3. Redirect to item detail or list page.
        return "redirect:/items";
    }

    @GetMapping("/items/{itemId}/edit")
    public String editForm(@PathVariable Long itemId, Model model) {
        // TODO: Find the item and copy its values to itemUpdateForm.
        model.addAttribute("itemId", itemId);
        model.addAttribute("itemUpdateForm", new ItemUpdateForm());
        return "items/editItemForm";
    }

    @PostMapping("/items/{itemId}/edit")
    public String edit(@PathVariable Long itemId,
                       @ModelAttribute("itemUpdateForm") ItemUpdateForm form) {
        // TODO:
        // 1. Move form values to ItemUpdateParam.
        // 2. Call itemService.update().
        // 3. Redirect to item detail page.
        return "redirect:/items/" + itemId;
    }

    @PostMapping("/items/{itemId}/delete")
    public String delete(@PathVariable Long itemId) {
        // TODO: Call itemService.delete() and redirect to item list.
        return "redirect:/items";
    }
}
