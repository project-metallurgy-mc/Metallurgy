package projectmetallurgy.metallurgy.api.multiblock;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import projectmetallurgy.metallurgy.Metallurgy;
import projectmetallurgy.metallurgy.api.utils.DirectionUtils;
import projectmetallurgy.metallurgy.api.utils.SetRestrictedField;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class TemplateMultiBlock implements MultiBlockHandler.IMultiBlock {
    private static final SetRestrictedField<Function<BlockState, ItemStack>> PICK_BLOCK = SetRestrictedField.common();
    private static final SetRestrictedField<BiFunction<ResourceLocation, MinecraftServer, StructureTemplate>>
            LOAD_TEMPLATE = SetRestrictedField.common();
    private static final SetRestrictedField<Function<StructureTemplate, List<StructureTemplate.Palette>>>
            GET_PALETTES = SetRestrictedField.common();
    private static final Logger LOGGER = Metallurgy.LOGGER;

    private final ResourceLocation loc;
    protected final BlockPos masterFromOrigin;
    protected final BlockPos triggerFromOrigin;
    protected final BlockPos size;
    protected final List<BlockMatcher.MatcherPredicate> additionalPredicates;
    @Nullable
    private StructureTemplate template;
    private BlockState trigger = Blocks.AIR.defaultBlockState();

    public TemplateMultiBlock(ResourceLocation loc, BlockPos masterFromOrigin, BlockPos triggerFromOrigin, BlockPos size,
                              List<BlockMatcher.MatcherPredicate> additionalPredicates) {
        this.loc = loc;
        this.masterFromOrigin = masterFromOrigin;
        this.triggerFromOrigin = triggerFromOrigin;
        this.size = size;
        this.additionalPredicates = additionalPredicates;
    }

    public TemplateMultiBlock(ResourceLocation loc, BlockPos masterFromOrigin, BlockPos triggerFromOrigin, BlockPos size) {
        this(loc, masterFromOrigin, triggerFromOrigin, size, ImmutableMap.of());
    }

    public TemplateMultiBlock(ResourceLocation loc, BlockPos masterFromOrigin, BlockPos triggerFromOrigin, BlockPos size, Map<Block, TagKey<Block>> tags) {
        this(loc, masterFromOrigin, triggerFromOrigin, size, ImmutableList.of(
                (expected, found, world, pos) -> {
                    TagKey<Block> tag = tags.get(expected.getBlock());
                    if (tag != null) {
                        if (found.is(tag))
                            return BlockMatcher.Result.allow(2);
                        else
                            return BlockMatcher.Result.deny(2);
                    } else
                        return BlockMatcher.Result.DEFAULT;
                }
        ));
    }

    @Nonnull
    protected StructureTemplate getTemplate(@Nullable Level world) {
        return getTemplate(world == null ? null : world.getServer());
    }

    public ResourceLocation getTemplateLocation() {
        return loc;
    }

    @Nonnull
    public StructureTemplate getTemplate(@Nullable MinecraftServer server) {
        if (template == null)//TODO reset on resource reload
        {
            template = LOAD_TEMPLATE.getValue().apply(loc, server);
            List<StructureTemplate.StructureBlockInfo> blocks = getStructureFromTemplate(template);
            for (int i = 0; i < blocks.size(); i++) {
                StructureTemplate.StructureBlockInfo info = blocks.get(i);
                if (info.pos.equals(triggerFromOrigin))
                    trigger = info.state;
                if (info.state == Blocks.AIR.defaultBlockState()) {
                    blocks.remove(i);
                    i--;
                } else if (info.state.isAir())
                    // Usually means it contains a block that has been renamed
                    LOGGER.error("Found non-default air block in template {}", loc);
            }
        }
        return Objects.requireNonNull(template);
    }

    public void reset() {
        template = null;
    }

    @Override
    public ResourceLocation getUniqueName() {
        return loc;
    }

    @Override
    public boolean isBlockTrigger(BlockState state, Direction d, @Nullable Level world) {
        getTemplate(world);
        Rotation rot = DirectionUtils.getRotationBetweenFacings(Direction.NORTH, d.getOpposite());
        if (rot == null)
            return false;
        for (Mirror mirror : getPossibleMirrorStates()) {
            BlockState modifiedTrigger = applyToState(trigger, mirror, rot);
            if (BlockMatcher.matches(modifiedTrigger, state, null, null, additionalPredicates).isAllow())
                return true;
        }
        return false;
    }

    @Override
    public boolean createStructure(Level world, BlockPos pos, Direction side, Player player) {
        Rotation rot = DirectionUtils.getRotationBetweenFacings(Direction.NORTH, side.getOpposite());
        if (rot == null)
            return false;
        List<StructureTemplate.StructureBlockInfo> structure = getStructure(world);
        mirrorLoop:
        for (Mirror mirror : getPossibleMirrorStates()) {
            StructurePlaceSettings placeSet = new StructurePlaceSettings().setMirror(mirror).setRotation(rot);
            BlockPos origin = pos.subtract(StructureTemplate.calculateRelativePosition(placeSet, triggerFromOrigin));
            for (StructureTemplate.StructureBlockInfo info : structure) {
                BlockPos realRelPos = StructureTemplate.calculateRelativePosition(placeSet, info.pos);
                BlockPos here = origin.offset(realRelPos);

                BlockState expected = applyToState(info.state, mirror, rot);
                BlockState inWorld = world.getBlockState(here);
                if (!BlockMatcher.matches(expected, inWorld, world, here, additionalPredicates).isAllow())
                    continue mirrorLoop;
            }
            form(world, origin, rot, mirror, side);
            return true;
        }
        return false;
    }

    private BlockState applyToState(BlockState in, Mirror m, Rotation r) {
        return in.mirror(m).rotate(r);
    }

    private List<Mirror> getPossibleMirrorStates() {
        if (canBeMirrored())
            return ImmutableList.of(Mirror.NONE, Mirror.FRONT_BACK);
        else
            return ImmutableList.of(Mirror.NONE);
    }

    protected void form(Level world, BlockPos pos, Rotation rot, Mirror mirror, Direction sideHit) {
        BlockPos masterPos = withSettingsAndOffset(pos, masterFromOrigin, mirror, rot);
        for (StructureTemplate.StructureBlockInfo block : getStructure(world)) {
            BlockPos actualPos = withSettingsAndOffset(pos, block.pos, mirror, rot);
            replaceStructureBlock(block, world, actualPos, mirror != Mirror.NONE, sideHit,
                    actualPos.subtract(masterPos));
        }
    }

    public BlockPos getMasterFromOriginOffset() {
        return masterFromOrigin;
    }

    protected abstract void replaceStructureBlock(StructureTemplate.StructureBlockInfo info, Level world, BlockPos actualPos, boolean mirrored, Direction clickDirection, Vec3i offsetFromMaster);

    @Override
    public List<StructureTemplate.StructureBlockInfo> getStructure(@Nullable Level world) {
        return getStructureFromTemplate(getTemplate(world));
    }

    private static List<StructureTemplate.StructureBlockInfo> getStructureFromTemplate(StructureTemplate template) {
        return GET_PALETTES.getValue().apply(template).get(0).blocks();
    }

    @Override
    public Vec3i getSize(@Nullable Level world) {
        return getTemplate(world).getSize();
    }

    public static BlockPos withSettingsAndOffset(BlockPos origin, BlockPos relative, Mirror mirror, Rotation rot) {
        StructurePlaceSettings settings = new StructurePlaceSettings().setMirror(mirror).setRotation(rot);
        return origin.offset(StructureTemplate.calculateRelativePosition(settings, relative));
    }

    public static BlockPos withSettingsAndOffset(BlockPos origin, BlockPos relative, boolean mirrored, Direction facing) {
        Rotation rot = DirectionUtils.getRotationBetweenFacings(Direction.NORTH, facing);
        if (rot == null)
            return origin;
        return withSettingsAndOffset(origin, relative, mirrored ? Mirror.FRONT_BACK : Mirror.NONE,
                rot);
    }

    @Override
    public void disassemble(Level world, BlockPos origin, boolean mirrored, Direction clickDirectionAtCreation) {
        Mirror mirror = mirrored ? Mirror.FRONT_BACK : Mirror.NONE;
        Rotation rot = DirectionUtils.getRotationBetweenFacings(Direction.NORTH, clickDirectionAtCreation);
        Preconditions.checkNotNull(rot);
        for (StructureTemplate.StructureBlockInfo block : getStructure(world)) {
            BlockPos actualPos = withSettingsAndOffset(origin, block.pos, mirror, rot);
            prepareBlockForDisassembly(world, actualPos);
            world.setBlockAndUpdate(actualPos, block.state.mirror(mirror).rotate(rot));
        }
    }

    protected void prepareBlockForDisassembly(Level world, BlockPos pos) {
    }

    @Override
    public BlockPos getTriggerOffset() {
        return triggerFromOrigin;
    }

    public boolean canBeMirrored() {
        return true;
    }

    public static void setCallbacks(
            Function<BlockState, ItemStack> pickBlock,
            BiFunction<ResourceLocation, MinecraftServer, StructureTemplate> loadTemplate,
            Function<StructureTemplate, List<StructureTemplate.Palette>> getPalettes
    ) {
        PICK_BLOCK.setValue(pickBlock);
        LOAD_TEMPLATE.setValue(loadTemplate);
        GET_PALETTES.setValue(getPalettes);
    }
}
