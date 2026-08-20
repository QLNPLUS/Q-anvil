package com.qanvil.block;

import com.qanvil.menu.QAnvilMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class QAnvilBlock extends AnvilBlock {
    private static final Component TITLE = Component.translatable("block.qanvil.q_anvil");
    private static final VoxelShape NORTH_SHAPE = createShape(net.minecraft.core.Direction.NORTH);
    private static final VoxelShape EAST_SHAPE = createShape(net.minecraft.core.Direction.EAST);
    private static final VoxelShape SOUTH_SHAPE = createShape(net.minecraft.core.Direction.SOUTH);
    private static final VoxelShape WEST_SHAPE = createShape(net.minecraft.core.Direction.WEST);

    public QAnvilBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        player.openMenu(state.getMenuProvider(level, pos));
        player.awardStat(Stats.INTERACT_WITH_ANVIL);
        return InteractionResult.CONSUME;
    }

    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider(
                (containerId, inventory, player) -> new QAnvilMenu(
                        containerId, inventory, ContainerLevelAccess.create(level, pos)),
                TITLE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return super.getStateForPlacement(context);
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos,
                               CollisionContext context) {
        return shapeFor(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos,
                                        CollisionContext context) {
        return shapeFor(state);
    }

    private static VoxelShape shapeFor(BlockState state) {
        return switch (state.getValue(HorizontalDirectionalBlock.FACING)) {
            case EAST -> EAST_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            default -> NORTH_SHAPE;
        };
    }

    private static VoxelShape createShape(net.minecraft.core.Direction facing) {
        VoxelShape shape = Shapes.empty();

        // Main body and the top platform follow the Blockbench model dimensions.
        shape = addBox(shape, facing, 4, 0, 2, 12, 2, 14);
        shape = addBox(shape, facing, 5, 2, 3, 11, 3, 13);
        shape = addBox(shape, facing, 6, 3, 4, 10, 6, 12);
        shape = addBox(shape, facing, 4.5, 6, 4, 11.5, 8, 12);
        shape = addBox(shape, facing, 4.5, 9, 0, 11.5, 12, 16);
        shape = addBox(shape, facing, 5, 12, 2, 11, 13, 14);

        // Four thin edges around the upper body.
        shape = addBox(shape, facing, 3.5, 9, 13, 12.5, 10, 14);
        shape = addBox(shape, facing, 3.5, 7, 11, 12.5, 8, 12);
        shape = addBox(shape, facing, 3.5, 7, 4, 12.5, 8, 5);
        shape = addBox(shape, facing, 3.5, 9, 2, 12.5, 10, 3);

        // The two diagonal supports use simple axis-aligned cuboids for collision.
        shape = addBox(shape, facing, 4, 4.5, 0, 12, 11.5, 7);
        shape = addBox(shape, facing, 4, 4.8, 9, 12, 11.5, 16);

        return shape;
    }

    private static VoxelShape addBox(VoxelShape shape, net.minecraft.core.Direction facing,
                                     double minX, double minY, double minZ,
                                     double maxX, double maxY, double maxZ) {
        double rotatedMinX;
        double rotatedMaxX;
        double rotatedMinZ;
        double rotatedMaxZ;
        switch (facing) {
            case EAST -> {
                rotatedMinX = 16 - maxZ;
                rotatedMaxX = 16 - minZ;
                rotatedMinZ = minX;
                rotatedMaxZ = maxX;
            }
            case SOUTH -> {
                rotatedMinX = 16 - maxX;
                rotatedMaxX = 16 - minX;
                rotatedMinZ = 16 - maxZ;
                rotatedMaxZ = 16 - minZ;
            }
            case WEST -> {
                rotatedMinX = minZ;
                rotatedMaxX = maxZ;
                rotatedMinZ = 16 - maxX;
                rotatedMaxZ = 16 - minX;
            }
            default -> {
                rotatedMinX = minX;
                rotatedMaxX = maxX;
                rotatedMinZ = minZ;
                rotatedMaxZ = maxZ;
            }
        }
        return Shapes.or(shape, Block.box(rotatedMinX, minY, rotatedMinZ,
                rotatedMaxX, maxY, rotatedMaxZ));
    }
}
