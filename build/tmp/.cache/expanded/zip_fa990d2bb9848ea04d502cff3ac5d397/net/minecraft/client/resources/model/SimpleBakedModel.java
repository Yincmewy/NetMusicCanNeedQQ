package net.minecraft.client.resources.model;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SimpleBakedModel implements BakedModel {
   protected final List<BakedQuad> unculledFaces;
   protected final Map<Direction, List<BakedQuad>> culledFaces;
   protected final boolean hasAmbientOcclusion;
   protected final boolean isGui3d;
   protected final boolean usesBlockLight;
   protected final TextureAtlasSprite particleIcon;
   protected final ItemTransforms transforms;
   protected final ItemOverrides overrides;
   /** Forge: Block render types to be used with {@linkplain net.minecraft.client.GraphicsStatus#FANCY fancy graphics} */
   protected final net.minecraftforge.client.ChunkRenderTypeSet blockRenderTypes;
   /** Forge: Block render types to be used with {@linkplain net.minecraft.client.GraphicsStatus#FAST fast graphics} */
   protected final net.minecraftforge.client.ChunkRenderTypeSet blockRenderTypesFast;
   /** Forge: Item render types to be used with {@linkplain net.minecraft.client.GraphicsStatus#FANCY fancy graphics} */
   protected final List<net.minecraft.client.renderer.RenderType> itemRenderTypes;
   /** Forge: Item render types to be used with {@linkplain net.minecraft.client.GraphicsStatus#FAST fast graphics} */
   protected final List<net.minecraft.client.renderer.RenderType> itemRenderTypesFast;
   /** Forge: Item render types to be used with {@linkplain net.minecraft.client.GraphicsStatus#FABULOUS fabulous graphics} */
   protected final List<net.minecraft.client.renderer.RenderType> fabulousItemRenderTypes;
   /** Forge: If this model's {@linkplain #blockRenderTypes block render types} are rendering cutout, to account for older leaves model JSONs */
   protected final boolean isRenderingCutout;

   /** @deprecated Forge: Use {@linkplain #SimpleBakedModel(List, Map, boolean, boolean, boolean, TextureAtlasSprite, ItemTransforms, ItemOverrides, net.minecraftforge.client.RenderTypeGroup, net.minecraftforge.client.RenderTypeGroup) variant with RenderTypeGroup} **/
   @Deprecated
   public SimpleBakedModel(List<BakedQuad> pUnculledFaces, Map<Direction, List<BakedQuad>> pCulledFaces, boolean pHasAmbientOcclusion, boolean pUsesBlockLight, boolean pIsGui3d, TextureAtlasSprite pParticleIcon, ItemTransforms pTransforms, ItemOverrides pOverrides) {
      this(pUnculledFaces, pCulledFaces, pHasAmbientOcclusion, pUsesBlockLight, pIsGui3d, pParticleIcon, pTransforms, pOverrides, net.minecraftforge.client.RenderTypeGroup.EMPTY);
   }

   /** @deprecated Forge: Use {@linkplain #SimpleBakedModel(List, Map, boolean, boolean, boolean, TextureAtlasSprite, ItemTransforms, ItemOverrides, net.minecraftforge.client.RenderTypeGroup, net.minecraftforge.client.RenderTypeGroup) variant with RenderTypeGroup for fast graphics} **/
   @Deprecated(forRemoval = true, since = "1.21.4")
   public SimpleBakedModel(List<BakedQuad> pUnculledFaces, Map<Direction, List<BakedQuad>> pCulledFaces, boolean pHasAmbientOcclusion, boolean pUsesBlockLight, boolean pIsGui3d, TextureAtlasSprite pParticleIcon, ItemTransforms pTransforms, ItemOverrides pOverrides, net.minecraftforge.client.RenderTypeGroup renderTypes) {
      this(pUnculledFaces, pCulledFaces, pHasAmbientOcclusion, pUsesBlockLight, pIsGui3d, pParticleIcon, pTransforms, pOverrides, renderTypes, net.minecraftforge.client.RenderTypeGroup.EMPTY);
   }

   /** Constructor with {@link net.minecraftforge.client.RenderTypeGroup RenderTypeGroup} for fancy and fast graphics. Preferred over {@link net.minecraft.client.renderer.ItemBlockRenderTypes#setRenderLayer(net.minecraft.world.level.block.Block, net.minecraft.client.renderer.RenderType) ItemBlockRenderTypes.setRenderLayer(Block, RenderType)}. */
   public SimpleBakedModel(
      List<BakedQuad> pUnculledFaces,
      Map<Direction, List<BakedQuad>> pCulledFaces,
      boolean pHasAmbientOcclusion,
      boolean pUsesBlockLight,
      boolean pIsGui3d,
      TextureAtlasSprite pParticleIcon,
      ItemTransforms pTransforms,
      ItemOverrides pOverrides,
      net.minecraftforge.client.RenderTypeGroup renderTypes,
      net.minecraftforge.client.RenderTypeGroup renderTypesFast
   ) {
      this.unculledFaces = pUnculledFaces;
      this.culledFaces = pCulledFaces;
      this.hasAmbientOcclusion = pHasAmbientOcclusion;
      this.isGui3d = pIsGui3d;
      this.usesBlockLight = pUsesBlockLight;
      this.particleIcon = pParticleIcon;
      this.transforms = pTransforms;
      this.overrides = pOverrides;

      boolean hasRenderTypes = renderTypes != null && !renderTypes.isEmpty();
      boolean hasRenderTypesFast = renderTypesFast != null && !renderTypesFast.isEmpty();
      this.blockRenderTypes = hasRenderTypes ? net.minecraftforge.client.ChunkRenderTypeSet.of(renderTypes.block()) : null;
      this.blockRenderTypesFast = hasRenderTypesFast ? net.minecraftforge.client.ChunkRenderTypeSet.of(renderTypesFast.block()) : null;
      this.itemRenderTypes = hasRenderTypes ? List.of(renderTypes.entity()) : null;
      this.itemRenderTypesFast = hasRenderTypesFast ? List.of(renderTypesFast.entity()) : null;
      this.fabulousItemRenderTypes = hasRenderTypes ? List.of(renderTypes.entityFabulous()) : null;
      this.isRenderingCutout = hasRenderTypes && (renderTypes.block() == net.minecraft.client.renderer.RenderType.cutout() || renderTypes.block() == net.minecraft.client.renderer.RenderType.cutoutMipped());
   }

   public List<BakedQuad> getQuads(@Nullable BlockState pState, @Nullable Direction pDirection, RandomSource pRandom) {
      return pDirection == null ? this.unculledFaces : this.culledFaces.get(pDirection);
   }

   public boolean useAmbientOcclusion() {
      return this.hasAmbientOcclusion;
   }

   public boolean isGui3d() {
      return this.isGui3d;
   }

   public boolean usesBlockLight() {
      return this.usesBlockLight;
   }

   public boolean isCustomRenderer() {
      return false;
   }

   public TextureAtlasSprite getParticleIcon() {
      return this.particleIcon;
   }

   public ItemTransforms getTransforms() {
      return this.transforms;
   }

   public ItemOverrides getOverrides() {
      return this.overrides;
   }

   private static final net.minecraftforge.client.ChunkRenderTypeSet SOLID_BLOCK = net.minecraftforge.client.ChunkRenderTypeSet.of(net.minecraft.client.renderer.RenderType.solid());
   private static final List<net.minecraft.client.renderer.RenderType> SOLID_BLOCK_ITEM = List.of(net.minecraft.client.renderer.RenderType.solid());

   @Override
   public net.minecraftforge.client.ChunkRenderTypeSet getRenderTypes(@org.jetbrains.annotations.NotNull BlockState state, @org.jetbrains.annotations.NotNull RandomSource rand, @org.jetbrains.annotations.NotNull net.minecraftforge.client.model.data.ModelData data) {
      if (!net.minecraft.client.renderer.ItemBlockRenderTypes.isFancy()) {
         if (blockRenderTypesFast != null)
            return blockRenderTypesFast;
         if (isRenderingCutout && state.getBlock() instanceof net.minecraft.world.level.block.LeavesBlock)
            return SOLID_BLOCK;
      }
      if (blockRenderTypes != null)
         return blockRenderTypes;
      return BakedModel.super.getRenderTypes(state, rand, data);
   }

   @Override
   public List<net.minecraft.client.renderer.RenderType> getRenderTypes(net.minecraft.world.item.ItemStack itemStack, boolean fabulous) {
      if (!fabulous) {
         if (!net.minecraft.client.renderer.ItemBlockRenderTypes.isFancy()) {
            if (itemRenderTypesFast != null)
               return itemRenderTypesFast;
            if (isRenderingCutout && itemStack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem && blockItem.getBlock() instanceof net.minecraft.world.level.block.LeavesBlock)
               return SOLID_BLOCK_ITEM;
         }
         if (itemRenderTypes != null)
            return itemRenderTypes;
      } else {
         if (fabulousItemRenderTypes != null)
            return fabulousItemRenderTypes;
      }
      return BakedModel.super.getRenderTypes(itemStack, fabulous);
   }

   @OnlyIn(Dist.CLIENT)
   public static class Builder {
      private final List<BakedQuad> unculledFaces = Lists.newArrayList();
      private final Map<Direction, List<BakedQuad>> culledFaces = Maps.newEnumMap(Direction.class);
      private final ItemOverrides overrides;
      private final boolean hasAmbientOcclusion;
      private TextureAtlasSprite particleIcon;
      private final boolean usesBlockLight;
      private final boolean isGui3d;
      private final ItemTransforms transforms;

      public Builder(BlockModel pBlockModel, ItemOverrides pOverrides, boolean pIsGui3d) {
         this(pBlockModel.hasAmbientOcclusion(), pBlockModel.getGuiLight().lightLikeBlock(), pIsGui3d, pBlockModel.getTransforms(), pOverrides);
      }

      public Builder(boolean pHasAmbientOcclusion, boolean pUsesBlockLight, boolean pIsGui3d, ItemTransforms pTransforms, ItemOverrides pOverrides) {
         for(Direction direction : Direction.values()) {
            this.culledFaces.put(direction, Lists.newArrayList());
         }

         this.overrides = pOverrides;
         this.hasAmbientOcclusion = pHasAmbientOcclusion;
         this.usesBlockLight = pUsesBlockLight;
         this.isGui3d = pIsGui3d;
         this.transforms = pTransforms;
      }

      public SimpleBakedModel.Builder addCulledFace(Direction pFacing, BakedQuad pQuad) {
         this.culledFaces.get(pFacing).add(pQuad);
         return this;
      }

      public SimpleBakedModel.Builder addUnculledFace(BakedQuad pQuad) {
         this.unculledFaces.add(pQuad);
         return this;
      }

      public SimpleBakedModel.Builder particle(TextureAtlasSprite pParticleIcon) {
         this.particleIcon = pParticleIcon;
         return this;
      }

      public SimpleBakedModel.Builder item() {
         return this;
      }

      /** @deprecated Forge: Use {@linkplain #build(net.minecraftforge.client.RenderTypeGroup) variant with RenderTypeGroup} **/
      @Deprecated
      public BakedModel build() {
         return build(net.minecraftforge.client.RenderTypeGroup.EMPTY, net.minecraftforge.client.RenderTypeGroup.EMPTY);
      }

      /**
       * Builds with the render type to be used for this model, which will be used for any graphics setting.
       * <p>
       * If you need to set a specific render type for
       * {@linkplain net.minecraft.client.GraphicsStatus#FAST fast graphics}, consider using
       * {@link #build(net.minecraftforge.client.RenderTypeGroup, net.minecraftforge.client.RenderTypeGroup)
       * renderTypes(RenderTypeGroup, RenderTypeGroup)} instead which allows choosing render types for both fancy and
       * fast graphics.
       *
       * @apiNote If this model is for {@linkplain net.minecraft.world.level.block.LeavesBlock leaves} and if the
       * given render type is either {@linkplain net.minecraft.client.renderer.RenderType#cutout() cutout} or
       * {@linkplain net.minecraft.client.renderer.RenderType#cutoutMipped() cutout mipped}, it will be overridden
       * with {@linkplain net.minecraft.client.renderer.RenderType#solid() solid} when fast graphics is enabled.
       * @see #build(net.minecraftforge.client.RenderTypeGroup, net.minecraftforge.client.RenderTypeGroup)
       * renderTypes(RenderTypeGroup, RenderTypeGroup)
       */
      public BakedModel build(net.minecraftforge.client.RenderTypeGroup renderTypes) {
         return this.build(renderTypes, net.minecraftforge.client.RenderTypeGroup.EMPTY);
      }

      /**
       * Builds with the render types to be used for this model: one for
       * {@linkplain net.minecraft.client.GraphicsStatus#FANCY fancy graphics} and one for
       * {@linkplain net.minecraft.client.GraphicsStatus#FAST fast graphics}.
       *
       * @see #build(net.minecraftforge.client.RenderTypeGroup)
       */
      public BakedModel build(net.minecraftforge.client.RenderTypeGroup renderTypes, net.minecraftforge.client.RenderTypeGroup renderTypesFast) {
         if (this.particleIcon == null) {
            throw new RuntimeException("Missing particle!");
         } else {
            // Forge: Account for render types in model JSONs
            return new SimpleBakedModel(this.unculledFaces, this.culledFaces, this.hasAmbientOcclusion, this.usesBlockLight, this.isGui3d, this.particleIcon, this.transforms, this.overrides, renderTypes, renderTypesFast);
         }
      }
   }
}
