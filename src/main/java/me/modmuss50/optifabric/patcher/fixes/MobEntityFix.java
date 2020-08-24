package me.modmuss50.optifabric.patcher.fixes;

import me.modmuss50.optifabric.util.RemappingUtils;
import org.apache.commons.lang3.Validate;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class MobEntityFix implements ClassFixer {
	private String getPreferredEquipmentSlot = RemappingUtils.getMethodName("class_1308", "method_5953", "(Lnet/minecraft/class_1799;)Lnet/minecraft/class_1304;");

	@Override
	public void fix(ClassNode optifine, ClassNode minecraft) {
		// Restore the vanilla getPreferredEquipmentSlot as optifine's replacement doesn't do anything
		Validate.notNull(getPreferredEquipmentSlot, "Failed to find name");

		replaceOrCopyMethod(optifine, minecraft, getPreferredEquipmentSlot);
	}

	private void replaceOrCopyMethod(ClassNode optifine, ClassNode minecraft, String name) {
		MethodNode vanillaNode = null;

		for (MethodNode method : minecraft.methods) {
			if (method.name.equals(name)) {
				vanillaNode = method;
				break;
			}
		}

		optifine.methods.removeIf((m) -> m.name.equals(name));
		optifine.methods.add(vanillaNode);
	}
}
