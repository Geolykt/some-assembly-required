package someassemblyrequired.common.block.tileentity;

import net.minecraft.block.BlockState;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.world.Explosion;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import someassemblyrequired.common.block.RedstoneToasterBlock;
import someassemblyrequired.common.init.Items;
import someassemblyrequired.common.init.RecipeTypes;
import someassemblyrequired.common.init.Tags;
import someassemblyrequired.common.init.TileEntityTypes;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public class RedstoneToasterTileEntity extends ItemHandlerTileEntity implements ITickableTileEntity {

    private static final int TOASTING_TIME = 240;
    private static final int SMOKE_PARTICLES_TIME = 80;

    private int toastingProgress;
    private int smokeParticlesProgress;
    private boolean isEjectionToaster;
    @Nullable
    private UUID playerToasting;

    public RedstoneToasterTileEntity() {
        // noinspection unchecked
        super((TileEntityType<RedstoneToasterTileEntity>) TileEntityTypes.REDSTONE_TOASTER.get(), 2);
    }

    public void setAutoEject(boolean isEjectionToaster) {
        this.isEjectionToaster = isEjectionToaster;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        // for items, only allow inserting/extracting from the bottom
        // return super.getCapability for any other capability
        if (side == null || side == Direction.DOWN || capability != CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return super.getCapability(capability, side);
        }
        return LazyOptional.empty();
    }

    private boolean hasMetalInside() {
        return getItems().stream().anyMatch(stack -> Tags.TOASTER_METALS.contains(stack.getItem()));
    }

    public void explode() {
        if (world != null && !world.isRemote()) {
            PlayerEntity player = playerToasting == null ? null : world.getPlayerByUuid(playerToasting);
            NonNullList<ItemStack> items = removeItems();
            world.removeBlock(pos, true);
            world.createExplosion(player, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2.2F, true, Explosion.Mode.DESTROY);
            for (ItemStack stack : items) {
                ItemEntity entity = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
                entity.setDefaultPickupDelay();
                entity.setMotion(entity.getMotion().scale(2.5));
                world.addEntity(entity);
            }
        }
    }

    public ItemStack removeItem() {
        int slot = !getInventory().getStackInSlot(0).isEmpty() ? 0 : 1;
        return getInventory().extractItem(slot, 1, false);
    }

    /**
     * Insert 1 unit of the specified stack in the toaster
     *
     * @return whether the ingredient successfully got added
     */
    public boolean addItem(ItemStack stack) {
        int slot;
        if (getInventory().getStackInSlot(0).isEmpty()) {
            if (!getInventory().getStackInSlot(1).isEmpty()) {
                // fully empty the toaster first
                return false;
            }
            slot = 0;
        } else {
            slot = 1;
        }

        if (stack.getCount() != getInventory().insertItem(slot, stack.copy(), false).getCount()) {
            stack.shrink(1);
            return true;
        }
        return false;
    }

    public ItemStack getItem(int slot) {
        return getInventory().getStackInSlot(slot);
    }

    private boolean hasToastingResult(ItemStack ingredient) {
        return !getToastingResult(ingredient).isEmpty();
    }

    private ItemStack getToastingResult(ItemStack ingredient) {
        if (ingredient.isEmpty() || ingredient.getItem() == Items.CHARRED_MORSEL.get() || world == null) {
            return ItemStack.EMPTY;
        }

        IInventory ingredientWrapper = new Inventory(ingredient);

        Optional<? extends IRecipe<IInventory>> toastingRecipe = world.getRecipeManager().getRecipe(RecipeTypes.TOASTING, ingredientWrapper, world);

        IRecipe<IInventory> recipe = null;
        if (toastingRecipe.isPresent()) {
            recipe = toastingRecipe.get();
        } else {
            Optional<? extends IRecipe<IInventory>> smokingRecipe = world.getRecipeManager().getRecipe(IRecipeType.SMOKING, ingredientWrapper, world);
            if (smokingRecipe.isPresent()) {
                recipe = smokingRecipe.get();
            }
        }

        if (recipe == null) {
            if (ingredient.getItem().isFood()) {
                if (Tags.SMALL_FOODS.contains(ingredient.getItem())) {
                    return new ItemStack(Items.CHARRED_MORSEL.get());
                } else {
                    return new ItemStack(Items.CHARRED_FOOD.get());
                }
            } else {
                return ItemStack.EMPTY;
            }
        } else {
            return recipe.getCraftingResult(ingredientWrapper);
        }
    }

    private void toastItems() {
        if (world == null) {
            return;
        }

        for (int slot = 0; slot < getInventory().getSlots(); slot++) {
            ItemStack result = getToastingResult(getInventory().getStackInSlot(slot));
            if (!result.isEmpty()) {
                getInventory().setStackInSlot(slot, result);
            }
        }
    }

    public boolean startToasting(PlayerEntity entity) {
        playerToasting = entity.getUniqueID();
        if (world != null && world.getBlockState(pos).get(BlockStateProperties.WATERLOGGED)) {
            explode();
        }
        return startToasting();
    }

    public boolean startToasting() {
        if (world == null || isToasting()) {
            return false;
        }

        world.playSound(null, pos, SoundEvents.BLOCK_WOODEN_BUTTON_CLICK_ON, SoundCategory.BLOCKS, 0.5F, 0.8F);

        if (hasMetalInside()) {
            explode();
            return true;
        }

        if (world.getBlockState(pos).hasProperty(RedstoneToasterBlock.TOASTING)) {
            world.setBlockState(pos, world.getBlockState(pos).with(RedstoneToasterBlock.TOASTING, true));
        }

        toastingProgress = TOASTING_TIME;
        if (smokeParticlesProgress > 0) {
            smokeParticlesProgress = TOASTING_TIME + SMOKE_PARTICLES_TIME;
        }

        return true;
    }

    private void stopToasting() {
        toastItems();

        playerToasting = null;

        if (world == null) {
            return;
        }

        if (world.getBlockState(pos).hasProperty(RedstoneToasterBlock.TOASTING)) {
            world.setBlockState(pos, world.getBlockState(pos).with(RedstoneToasterBlock.TOASTING, false));
        }

        if (isEjectionToaster) {
            world.playSound(null, pos, SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.BLOCKS, 0.5F, world.rand.nextFloat() * 0.25F + 0.8F);

            for (ItemStack ingredient : removeItems()) {
                ItemEntity item = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.75, pos.getZ() + 0.5, ingredient);
                item.setDefaultPickupDelay();
                item.setMotion(item.getMotion().mul(0.5, 2, 0.5));
                world.addEntity(item);
            }
        }

        world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_BELL, SoundCategory.BLOCKS, 0.8F, 4);
    }

    public boolean isToasting() {
        return toastingProgress > 0;
    }

    @Override
    protected boolean canModifyItems() {
        return super.canModifyItems() && !isToasting();
    }

    @Override
    public void read(BlockState state, CompoundNBT compoundNBT) {
        super.read(state, compoundNBT);
        toastingProgress = compoundNBT.getInt("ToastingProgress");
        smokeParticlesProgress = compoundNBT.getInt("SmokeParticlesProgress");
        isEjectionToaster = compoundNBT.getBoolean("IsEjectionToaster");
        if (compoundNBT.hasUniqueId("PlayerToasting")) {
            playerToasting = compoundNBT.getUniqueId("PlayerToasting");
        }
    }

    @Override
    public CompoundNBT write(CompoundNBT compoundNBT) {
        compoundNBT.putInt("ToastingProgress", toastingProgress);
        compoundNBT.putInt("SmokeParticlesProgress", smokeParticlesProgress);
        compoundNBT.putBoolean("IsEjectionToaster", isEjectionToaster);
        if (playerToasting != null) {
            compoundNBT.putUniqueId("PlayerToasting", playerToasting);
        }
        return super.write(compoundNBT);
    }

    @Override
    public void tick() {
        if (world == null) {
            return;
        }

        if (isToasting()) {
            if (--toastingProgress <= 0) {
                stopToasting();
            } else {
                if (toastingProgress % 4 == 0) {
                    world.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.05F, toastingProgress % 8 == 0 ? 2 : 1.9F);
                }
            }
        }

        if (toastingProgress == TOASTING_TIME / 3) {
            smokeParticlesProgress = TOASTING_TIME / 3 + SMOKE_PARTICLES_TIME;
        }

        if (smokeParticlesProgress > 0) {
            if (smokeParticlesProgress-- % 3 == 0) {
                world.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 0, 0.03, 0);
            }
        }
    }

    @Override
    protected TileEntityItemHandler createItemHandler(int size) {
        return new TileEntityItemHandler(size) {

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return super.isItemValid(slot, stack) && stack.getItem() != Items.SANDWICH.get() && (stack.getItem().isFood() || !(stack.getItem() instanceof BlockItem) || hasToastingResult(stack));
            }
        };
    }
}
