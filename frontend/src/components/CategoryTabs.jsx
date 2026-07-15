import { LayoutGrid, Shirt, Layers, ShoppingBag, Footprints, Gem, Package } from "lucide-react";
import { CATEGORIES, CATEGORY_LABELS } from "../api/constants.js";

const ICONS = {
  All: LayoutGrid,
  OUTER: Shirt,
  TOP: Shirt,
  BOTTOM: Layers,
  BAG: ShoppingBag,
  SHOES: Footprints,
  ACCESSORY: Gem,
  ETC: Package,
};

export default function CategoryTabs({ value, onChange }) {
  return (
    <div className="category-tabs">
      {CATEGORIES.map((name) => {
        const Icon = ICONS[name];
        const active = value === name;
        return (
          <button key={name} type="button" className={active ? "active" : ""} onClick={() => onChange(name)}>
            <Icon size={16} strokeWidth={2.25} />
            {CATEGORY_LABELS[name]}
          </button>
        );
      })}
    </div>
  );
}
