package me.modmuss50.optifabric.mod;

import com.chocohead.mm.api.ClassTinkerers;
import com.google.common.collect.Lists;
import me.modmuss50.optifabric.patcher.ASMUtils;
import me.modmuss50.optifabric.patcher.ClassCache;
import me.modmuss50.optifabric.patcher.fixes.OptifineFixer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Consumer;

public class OptifineInjector {

	ClassCache classCache;

	private static Set<String> patched = new HashSet<>();
	private final OptifineFixer optifineFixer = OptifineFixer.INSTANCE;

	public OptifineInjector(ClassCache classCache) {
		this.classCache = classCache;
	}

	public void setup() {
		classCache.getClasses().forEach(s -> ClassTinkerers.addReplacement(s.replaceAll("/", ".").substring(0, s.length() - 6), transformer));
	}

	//I have no idea why and how this works, if you know better please let me know
	public final Consumer<ClassNode> transformer = target -> {

		if(patched.contains(target.name)) {
			System.out.println("Already patched " + target.name);
			return;
		}
		patched.add(target.name);

		//I cannot imagine this being very good at all
		ClassNode source = getSourceClassNode(target);


		//Skip applying classes
		if (optifineFixer.shouldSkip(target.name)) {
			return;
		}

		//Patch the class if required
		optifineFixer.getFixers(target.name)
				.forEach(classFixer -> classFixer.fix(source, target));

		List<MethodNode> previousMethods = Lists.newArrayList(target.methods);

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

		// Lets make every class we touch public
		target.access = modAccess(target.access);
		target.methods.forEach(methodNode -> {
			previousMethods.stream().filter(previousMethod -> Objects.equals(previousMethod.name, methodNode.name) && Objects.equals(previousMethod.desc, methodNode.desc)).findFirst().ifPresent(previousMethod -> {
				if ((previousMethod.access & Opcodes.ACC_PUBLIC) != 0) {
					methodNode.access = modAccess(methodNode.access);
				}
			});
		});
		target.fields.forEach(fieldNode -> fieldNode.access = modAccess(fieldNode.access));
	};

	private static int modAccess(int access) {
		if ((access & 0x7) != 0) {
			return (access & (~0x7)) | Opcodes.ACC_PUBLIC;
		} else {
			return access;
		}
	}

	private ClassNode getSourceClassNode(ClassNode classNode) {
		String name = classNode.name.replaceAll("\\.", "/") + ".class";
		byte[] bytes = classCache.getAndRemove(name);
		if (bytes == null) {
			throw new RuntimeException("Failed to find patched class for: " + name);
		}
		return ASMUtils.readClassFromBytes(bytes);
	}

	public String toString(InputStream inputStream, Charset charset) throws IOException {
		StringBuilder stringBuilder = new StringBuilder();
		String line;
		try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, charset))) {
			while ((line = bufferedReader.readLine()) != null) {
				stringBuilder.append(line);
			}
		}
		return stringBuilder.toString();
	}

}
