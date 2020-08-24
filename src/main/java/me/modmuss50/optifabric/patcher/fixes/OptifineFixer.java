package me.modmuss50.optifabric.patcher.fixes;

import me.modmuss50.optifabric.util.RemappingUtils;
import net.fabricmc.loader.api.FabricLoader;

import java.util.*;

public class OptifineFixer {

	public static final OptifineFixer INSTANCE = new OptifineFixer();

	private HashMap<String, List<ClassFixer>> classFixes = new HashMap<>();
	private Set<String> skippedClass = new HashSet<>();

	private OptifineFixer() {
		//net/minecraft/client/render/chunk/ChunkBuilder$BuiltChunk
		registerFix("class_851", new ChunkRendererFix());

		//net/minecraft/client/render/block/BlockModelRenderer
		registerFix("class_778", new BlockModelRendererFix());

		//net/minecraft/client/Keyboard
		registerFix("class_309", new KeyboardFix());

		//net/minecraft/client/render/item/HeldItemRenderer
		registerFix("class_759", new HeldItemRendererFix());

		//net/minecraft/client/texture/SpriteAtlasTexture
		registerFix("class_1059", new SpriteAtlasTextureFix());

		//net/minecraft/server/world/ThreadedAnvilChunkStorage
		registerFix("class_3898", new ThreadedAnvilChunkStorageFix());

		//net/minecraft/client/particle/ParticleManager
		skipClass("class_702");

		//net/minecraft/client/render/item/HeldItemRenderer$1
		skipClass("class_759$1");
		
		//net/minecraft/entity/mob/MobEntity
		registerFix("class_1308", new MobEntityFix());
		
		if (FabricLoader.getInstance().isModLoaded("patchouli")) {
			System.out.println("We give up fixing this for now, no more custom item textures.");
			//net/minecraft/client/render/model/json/ModelOverrideList
			registerFix("class_806", (optifine, minecraft) -> {
				optifine.methods = minecraft.methods;
			});
		}
	}

	private void registerFix(String className, ClassFixer classFixer) {
		classFixes.computeIfAbsent(RemappingUtils.getClassName(className), s -> new ArrayList<>()).add(classFixer);
	}

	private void skipClass(String className) {
		skippedClass.add(RemappingUtils.getClassName(className));
	}

	public boolean shouldSkip(String className) {
		return skippedClass.contains(className);
	}

	public List<ClassFixer> getFixers(String className) {
		return classFixes.getOrDefault(className, Collections.emptyList());
	}
}
