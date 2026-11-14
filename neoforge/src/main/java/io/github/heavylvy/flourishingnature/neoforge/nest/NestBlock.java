package io.github.heavylvy.flourishingnature.neoforge.nest;

import io.github.heavylvy.flourishingnature.core.nest.NestEgg;
import io.github.heavylvy.flourishingnature.neoforge.boundary.NestWorld;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A low ring of sticks and grass holding up to four eggs. The egg count and whether any are fertile
 * are block states, so they show; the eggs themselves and the owner live in {@link NestBlockEntity}.
 * Using the nest takes the newest egg (fertile eggs come out as ordinary eggs).
 */
public final class NestBlock extends Block implements EntityBlock {
    public static final IntegerProperty EGGS = IntegerProperty.create("eggs", 0, 4);
    public static final BooleanProperty FERTILE = BooleanProperty.create("fertile");
    /** Built by a hen: decays when abandoned and breaks into sticks. Crafted nests are not wild. */
    public static final BooleanProperty WILD = BooleanProperty.create("wild");

    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 3, 15);

    public NestBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(EGGS, 0).setValue(FERTILE, false).setValue(WILD, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(EGGS, FERTILE, WILD);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (state.getValue(EGGS) == 0) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        Optional<NestEgg> taken = level.getBlockEntity(pos, NestRegistry.NEST_ENTITY.get()).flatMap(NestBlockEntity::takeEgg);
        if (taken.isEmpty()) {
            return InteractionResult.PASS;
        }
        // Fertile or not, a taken egg is an ordinary egg: hatching happens only in the nest.
        ItemStack egg = new ItemStack(Items.EGG);
        if (!player.getInventory().add(egg)) {
            Block.popResource(level, pos, egg);
        }
        NestWorld.playSound(level, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public NestBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NestBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return (tickLevel, pos, tickState, entity) -> {
            if (tickLevel instanceof ServerLevel server && entity instanceof NestBlockEntity nest) {
                nest.serverTick(server);
            }
        };
    }
}
