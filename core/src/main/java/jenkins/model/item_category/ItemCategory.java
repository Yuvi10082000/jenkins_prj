package jenkins.model.item_category;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.TopLevelItemDescriptor;
import jenkins.model.Messages;

import javax.annotation.Nonnull;

/**
 * A category for {@link hudson.model.Item}s.
 *
 * @since TODO
 */
public abstract class ItemCategory implements ExtensionPoint {

    public static int MIN_TOSHOW = 1;

    /**
     * Helpful to set the order.
     */
    private int order = 1;

    /**
     * Identifier, e.g. "standaloneprojects", etc.
     *
     * @return the identifier
     */
    public abstract String getId();

    /**
     * The description in plain text
     *
     * @return the description
     */
    public abstract String getDescription();

    /**
     * A human readable name.
     *
     * @return the display name
     */
    public abstract String getDisplayName();

    /**
     * Minimum number of items required to show the category.
     *
     * @return the minimum items required
     */
    public abstract int getMinToShow();

    private void setOrder(int order) {
        this.order = order;
    }

    public int getOrder() {
        return order;
    }

    /**
     * A {@link ItemCategory} associated to this {@link TopLevelItemDescriptor}.
     *
     * @return A {@link ItemCategory}, if not found, {@link ItemCategory.UncategorizedCategory} is returned
     */
    @Nonnull
    public static ItemCategory getCategory(TopLevelItemDescriptor descriptor) {
        int order = 0;
        ExtensionList<ItemCategory> categories = ExtensionList.lookup(ItemCategory.class);
        for (ItemCategory category : categories) {
            if (category.getId().equals(descriptor.getCategoryId())) {
                category.setOrder(++order);
                return category;
            }
            order++;
        }
        return new UncategorizedCategory();
    }

    /**
     * The default {@link ItemCategory}, if an item doesn't belong anywhere else, this is where it goes by default.
     */
    @Extension(ordinal = Integer.MIN_VALUE)
    public static final class UncategorizedCategory extends ItemCategory {

        public static final String ID = "uncategorized";

        @Override
        public String getId() {
            return ID;
        }

        @Override
        public String getDescription() {
            return Messages.ItemCategory_Uncategorized_Description();
        }

        @Override
        public String getDisplayName() {
            return Messages.ItemCategory_Uncategorized_DisplayName();
        }

        @Override
        public int getMinToShow() {
            return ItemCategory.MIN_TOSHOW;
        }

    }

}
