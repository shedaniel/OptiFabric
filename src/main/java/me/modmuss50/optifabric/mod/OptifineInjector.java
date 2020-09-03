package me.modmuss50.optifabric.mod;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.MethodNode;

import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

import net.fabricmc.loader.api.FabricLoader;

import com.chocohead.mm.api.ClassTinkerers;

import me.modmuss50.optifabric.patcher.ASMUtils;
import me.modmuss50.optifabric.patcher.ClassCache;
import me.modmuss50.optifabric.patcher.fixes.OptifineFixer;

public class OptifineInjector {
	private static Set<String> patched = new HashSet<>();
	private final ClassCache classCache;

	public OptifineInjector(ClassCache classCache) {
		this.classCache = classCache;
	}

	public Optional<ClassNode> predictFuture(String className) {
		byte[] bytes = classCache.getClass(className);
		return bytes != null ? Optional.of(ASMUtils.readClassFromBytes(bytes)) : Optional.empty();
	}

	public void setup() {
		for (String name : classCache.getClasses()) {
			ClassTinkerers.addReplacement(name, target -> {
				//Avoid double patching things, not that this should happen
				if (!patched.add(target.name)) {
					System.out.println("Already patched " + target.name);
					return;
				}

				//Skip applying class patches we veto
				if (OptifineFixer.INSTANCE.shouldSkip(target.name)) {
					return;
				}

				//Remember the access we started with
				Object2IntMap<String> memberToAccess = new Object2IntArrayMap<>(target.methods.size());
				memberToAccess.defaultReturnValue(-1);
				for (MethodNode method : target.methods) {
					memberToAccess.put(method.name + method.desc, method.access);
				}
				for (FieldNode field : target.fields) {
					memberToAccess.put(field.name + ' ' + field.desc, field.access);
				}

				//I cannot imagine this being very good at all
				ClassNode source = getSourceClassNode(target);

				//Patch the class if required
				OptifineFixer.INSTANCE.getFixers(target.name).forEach(classFixer -> classFixer.fix(source, target));

				target.methods = source.methods;
				target.fields = source.fields;
				target.interfaces = source.interfaces;
				target.superName = source.superName;

				//Classes should be read with frames expanded (as Mixin itself does it), in which case this should all be fine
				for (MethodNode methodNode : target.methods) {
					for (AbstractInsnNode insnNode : methodNode.instructions.toArray()) {
						if (insnNode instanceof FrameNode) {
							FrameNode frameNode = (FrameNode) insnNode;
							if (frameNode.local == null) {
								throw new IllegalStateException("Null locals in " + frameNode.type + " frame @ " + source.name + "#" + methodNode.name + methodNode.desc);
							}
						}
					}
				}

				// Lets make every class we touch match the access it used to have
				target.access = widerAccess(target.access, source.access);
				for (MethodNode method : target.methods) {
					int access = memberToAccess.getInt(method.name + method.desc);
					if (access != -1) method.access = widerAccess(access, method.access);
				}
				for (FieldNode field : target.fields) {
					int access = memberToAccess.getInt(field.name + ' ' + field.desc);
					if (access != -1) field.access = widerAccess(access, field.access);
				}
			});
		}
	}

	private static int widerAccess(int origin, int target) {
		target = unfinal(origin, target);
		switch (target & 0x7) {
		case Modifier.PUBLIC:
			return target;

		case Modifier.PROTECTED:
			return Modifier.isPublic(origin) ? (target & (~0x7)) | Modifier.PUBLIC : target;

		case 0:
			return Modifier.isPrivate(origin) ? target : (target & (~0x7)) | (origin & 0x7);

		case Modifier.PRIVATE:
			return (target & (~0x7)) | (origin & 0x7);

		default:
			if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
				throw new AssertionError("Unexpected access: " + target + " (transformed from " + origin + ')');
			}

			return target;
		}
	}
	
	private static int unfinal(int origin, int target) {
	    if (!Modifier.isFinal(origin))
	        return target & (~Modifier.FINAL);
	    return target;
    }

	private ClassNode getSourceClassNode(ClassNode classNode) {
		byte[] bytes = classCache.popClass(classNode.name);
		if(bytes == null) {
			throw new RuntimeException("Failed to find patched class for: " + classNode.name);
		}
		return ASMUtils.readClassFromBytes(bytes);
	}
}
