import { LayoutGrid, Shirt, Layers, ShoppingBag, Footprints, Gem, Package } from "lucide-react";
import { Tabs, TabsList, TabsTrigger } from "./ui/tabs.jsx";
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
    <Tabs value={value} onValueChange={onChange}>
      <TabsList className="h-auto w-full justify-start gap-2 overflow-x-auto bg-transparent p-0">
        {CATEGORIES.map((name) => {
          const Icon = ICONS[name];
          return (
            <TabsTrigger
              key={name}
              value={name}
              className="gap-1.5 rounded-full border border-neutral-200 px-4 py-2 data-[state=active]:border-transparent data-[state=active]:bg-neutral-900 data-[state=active]:text-white data-[state=active]:shadow-none"
            >
              <Icon size={16} strokeWidth={2.25} />
              {CATEGORY_LABELS[name]}
            </TabsTrigger>
          );
        })}
      </TabsList>
    </Tabs>
  );
}
