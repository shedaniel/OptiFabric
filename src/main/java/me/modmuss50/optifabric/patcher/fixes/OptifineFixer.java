package me.modmuss50.optifabric.patcher.fixes;

import me.modmuss50.optifabric.util.RemappingUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class OptifineFixer {

	public static final OptifineFixer INSTANCE = new OptifineFixer();

	private HashMap<String, List<ClassFixer>> classFixes = new HashMap<>();
	private List<String> skippedClass = new ArrayList<>();

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
