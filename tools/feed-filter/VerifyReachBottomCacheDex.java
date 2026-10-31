import com.android.tools.smali.dexlib2.AccessFlags;
import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;

import java.io.File;
import java.util.*;

/** Bytecode regression check for the reach-bottom cache IllegalAccessError.
 * Run against an actual patched APK or a dex containing the callback and model:
 * java -cp <compiled-tools>;<morphe-cli.jar> VerifyReachBottomCacheDex <apk-or-dex>
 */
public final class VerifyReachBottomCacheDex {
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static Object reference(Instruction instruction) {
        return instruction instanceof ReferenceInstruction r ? r.getReference() : null;
    }

    public static void main(String[] args) throws Exception {
        require(args.length == 1, "usage: VerifyReachBottomCacheDex <apk-or-dex>");
        var container = DexFileFactory.loadDexContainer(new File(args[0]), Opcodes.forApi(35));
        Map<String, ClassDef> classes = new HashMap<>();
        Map<String, String> classEntries = new HashMap<>();
        List<Method> matches = new ArrayList<>();
        for (String entry : container.getDexEntryNames()) {
            for (ClassDef owner : container.getEntry(entry).getDexFile().getClasses()) {
                classes.put(owner.getType(), owner);
                classEntries.put(owner.getType(), entry);
                for (Method method : owner.getMethods()) {
                    if (method.getParameterTypes().size() != 1 || !method.getReturnType().equals("V")
                            || method.getImplementation() == null) continue;
                    Set<String> strings = new HashSet<>();
                    for (Instruction instruction : method.getImplementation().getInstructions()) {
                        if (reference(instruction) instanceof StringReference s) strings.add(s.getString());
                    }
                    if (strings.containsAll(List.of("golden_house", "offline_video_useout", "golden_insert"))) {
                        matches.add(method);
                    }
                }
            }
        }
        require(matches.size() == 1, "Expected exactly one semantic reach-bottom callback: " + matches.size());
        Method callback = matches.get(0);
        String resultType = callback.getParameterTypes().get(0).toString();
        ClassDef model = classes.get(resultType);
        require(model != null, "Result model missing from artifact");
        Map<String, Integer> fields = new HashMap<>();
        model.getFields().forEach(f -> {
            fields.put(f.getName(), f.getAccessFlags());
            require(AccessFlags.STATIC.isSet(f.getAccessFlags()) || AccessFlags.FINAL.isSet(f.getAccessFlags()),
                    "Result immutability must not be weakened: " + f);
        });
        List<Instruction> instructions = new ArrayList<>();
        callback.getImplementation().getInstructions().forEach(instructions::add);
        for (Instruction instruction : instructions) {
            if (instruction.getOpcode().name.startsWith("iput") && reference(instruction) instanceof FieldReference f
                    && f.getDefiningClass().equals(resultType)) {
                require(!AccessFlags.FINAL.isSet(fields.getOrDefault(f.getName(), 0)),
                        "Illegal final-field write from " + callback.getDefiningClass() + " to " + f);
            }
        }
        int nativeStart = -1;
        int constructorIndex = -1;
        int filterIndex = -1;
        int address = 0;
        List<Integer> addresses = new ArrayList<>();
        for (int i = 0; i < instructions.size(); i++) {
            Instruction instruction = instructions.get(i);
            addresses.add(address);
            address += instruction.getCodeUnits();
            if (nativeStart < 0 && instruction.getOpcode() == Opcode.SGET_OBJECT
                    && reference(instruction) instanceof FieldReference f
                    && f.getDefiningClass().endsWith("/ReachBottomCacheComponent;")) nativeStart = i;
            if (nativeStart >= 0) continue;
            require(instruction.getOpcode() != Opcode.RETURN_VOID, "Hook bypasses native cleanup");
            if (reference(instruction) instanceof MethodReference m) {
                if (m.getName().equals("shouldKeepCachedAweme")) filterIndex = i;
                if (m.getDefiningClass().equals(resultType) && m.getName().equals("<init>")) {
                    constructorIndex = i;
                    require(m.getParameterTypes().size() == 4 && m.getParameterTypes().get(0).equals("Z")
                                    && m.getParameterTypes().get(2).equals("Ljava/lang/String;")
                                    && m.getParameterTypes().get(3).equals("Ljava/util/List;"),
                            "Wrong cache-result constructor");
                }
            }
        }
        require(nativeStart > 0 && filterIndex >= 0 && constructorIndex > filterIndex,
                "Replacement cache-result hook missing");
        var invoke = (FiveRegisterInstruction) instructions.get(constructorIndex);
        require(invoke.getRegisterCount() == 5 && invoke.getRegisterC() == 0 && invoke.getRegisterD() == 1
                        && invoke.getRegisterE() == 2 && invoke.getRegisterF() == 3 && invoke.getRegisterG() == 4,
                "Constructor must receive replacement, success, payload, source, list");
        Instruction success = instructions.get(constructorIndex - 1);
        require(success.getOpcode() == Opcode.CONST_4 && ((OneRegisterInstruction) success).getRegisterA() == 1
                        && ((NarrowLiteralInstruction) success).getNarrowLiteral() == 0,
                "Rejected result must set success=false");
        var assignment = (TwoRegisterInstruction) instructions.get(constructorIndex + 1);
        require(instructions.get(constructorIndex + 1).getOpcode() == Opcode.MOVE_OBJECT_16
                        && assignment.getRegisterA() == callback.getImplementation().getRegisterCount() - 1
                        && assignment.getRegisterB() == 0,
                "Replacement must become the native callback argument");
        Map<Integer, FieldReference> copied = new HashMap<>();
        Set<Integer> keepTargets = new HashSet<>();
        int guards = 0;
        for (int i = 0; i < constructorIndex; i++) {
            Instruction instruction = instructions.get(i);
            if (instruction.getOpcode() == Opcode.IGET_OBJECT && reference(instruction) instanceof FieldReference f
                    && f.getDefiningClass().equals(resultType)) {
                var read = (TwoRegisterInstruction) instruction;
                require(read.getRegisterB() == 0, "Metadata must be read from original result");
                copied.put(read.getRegisterA(), f);
            }
            if (instruction.getOpcode() == Opcode.IF_EQZ || instruction.getOpcode() == Opcode.IF_NEZ) {
                guards++;
                keepTargets.add(addresses.get(i) + ((OffsetInstruction) instruction).getCodeOffset());
            }
        }
        require(copied.size() == 3 && copied.containsKey(2)
                        && copied.get(3).getType().equals("Ljava/lang/String;")
                        && copied.get(4).getType().equals("Ljava/util/List;"), "Original metadata not preserved");
        require(guards == 3 && keepTargets.size() == 1 && keepTargets.iterator().next()
                        == addresses.get(nativeStart - 1), "Failed/null/accepted results must skip replacement");
        System.out.println("PASS: no illegal final-field writes; rejection copies metadata with success=false; "
                + "failed/null/accepted results retain native cleanup. " + callback.getDefiningClass());
        System.out.println("Immutable result model: " + resultType + " in " + classEntries.get(resultType));
    }
}
