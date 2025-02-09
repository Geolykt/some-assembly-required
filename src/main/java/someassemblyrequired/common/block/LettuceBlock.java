package someassemblyrequired.common.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropsBlock;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import someassemblyrequired.common.init.Blocks;
import someassemblyrequired.common.init.Items;

public class LettuceBlock extends CropsBlock {

    private static final VoxelShape[] SHAPE_BY_AGE = new VoxelShape[]{
            Block.makeCuboidShape(5, -1, 5, 11, 3, 11),
            Block.makeCuboidShape(3, -1, 3, 13, 4, 13),
            Block.makeCuboidShape(1, -1, 1, 15, 6, 15),
            Block.makeCuboidShape(0, -1, 0, 16, 8, 16)
    };

    public LettuceBlock(Properties builder) {
        super(builder);
    }

    @Override
    public BlockState getPlant(IBlockReader world, BlockPos pos) {
        return Blocks.LETTUCE.get().getDefaultState();
    }

    @Override
    protected IItemProvider getSeedsItem() {
        return Items.LETTUCE_SEEDS.get();
    }

    @Override
    public int getMaxAge() {
        return 3;
    }

    @Override
    public IntegerProperty getAgeProperty() {
        return BlockStateProperties.AGE_0_3;
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(getAgeProperty());
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
        return SHAPE_BY_AGE[getAge(state)];
    }
}
