package someassemblyrequired.client.jei;

import com.mojang.blaze3d.matrix.MatrixStack;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
import someassemblyrequired.SomeAssemblyRequired;
import someassemblyrequired.common.init.Blocks;

import java.util.Arrays;
import java.util.List;

// there's probs a better way to do this...
@SuppressWarnings("rawtypes")
public class ToastingRecipeCategory implements IRecipeCategory<IRecipe> {

    public static final ResourceLocation ID = new ResourceLocation(SomeAssemblyRequired.MODID, "toasting");

    private final IDrawable background;
    private final IDrawable arrow;
    private final IDrawable icon;
    private final String title;

    public ToastingRecipeCategory(IGuiHelper guiHelper) {
        ResourceLocation texture = new ResourceLocation(SomeAssemblyRequired.MODID, "textures/gui/toasting_recipe.png");
        background = guiHelper.createDrawable(texture, 0, 0, 111, 41);
        icon = guiHelper.createDrawableIngredient(new ItemStack(Blocks.REDSTONE_TOASTER.get()));
        arrow = guiHelper.drawableBuilder(texture, 111, 0, 22, 16).buildAnimated(240, IDrawableAnimated.StartDirection.LEFT, false);
        title = I18n.format("recipecategory." + SomeAssemblyRequired.MODID + ".toasting");
    }

    @Override
    public ResourceLocation getUid() {
        return ID;
    }

    @Override
    public Class<? extends IRecipe> getRecipeClass() {
        return IRecipe.class;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setIngredients(IRecipe recipe, IIngredients ingredients) {
        //noinspection unchecked
        ingredients.setInputIngredients((List<Ingredient>) recipe.getIngredients());
        ingredients.setOutput(VanillaTypes.ITEM, recipe.getRecipeOutput());
    }

    @Override
    public void setRecipe(IRecipeLayout layout, IRecipe recipe, IIngredients ingredients) {
        IGuiItemStackGroup stacks = layout.getItemStacks();

        stacks.init(0, true, 11, 7);
        //noinspection unchecked
        stacks.set(0, Arrays.asList(((NonNullList<Ingredient>) recipe.getIngredients()).get(0).getMatchingStacks()));

        stacks.init(2, true, 79, 7);
        stacks.set(2, recipe.getRecipeOutput());
    }

    @Override
    public void draw(IRecipe recipe, MatrixStack matrixStack, double mouseX, double mouseY) {
        arrow.draw(matrixStack, 44, 12);

        TranslationTextComponent timeString = new TranslationTextComponent("gui.jei.category.smelting.time.seconds", 12);
        FontRenderer fontRenderer = Minecraft.getInstance().fontRenderer;
        int stringWidth = fontRenderer.getStringPropertyWidth(timeString);
        fontRenderer.func_243248_b(matrixStack, timeString, background.getWidth() - stringWidth - 8, 33, 0xFF808080);
    }
}
