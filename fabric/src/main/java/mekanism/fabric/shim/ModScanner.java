package mekanism.fabric.shim;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.annotation.ElementType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.neoforged.fml.loading.modscan.ModAnnotation;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Scans the classes of a mod for annotations, equivalent to the scan data FML provides for NeoForge mods.
 */
public final class ModScanner {

    private ModScanner() {
    }

    public static void scan(List<Path> rootPaths, ModFileScanData scanData) {
        for (Path root : rootPaths) {
            try (Stream<Path> files = Files.walk(root)) {
                files.filter(file -> file.toString().endsWith(".class") && Files.isRegularFile(file)).forEach(file -> {
                    try (InputStream stream = Files.newInputStream(file)) {
                        new ClassReader(stream).accept(new ScanningClassVisitor(scanData), ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    } catch (RuntimeException ignored) {
                        //Skip classes that ASM can't read
                    }
                });
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private static final class ScanningClassVisitor extends ClassVisitor {

        private final ModFileScanData scanData;
        private Type clazz = Type.VOID_TYPE;

        private ScanningClassVisitor(ModFileScanData scanData) {
            super(Opcodes.ASM9);
            this.scanData = scanData;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            clazz = Type.getObjectType(name);
        }

        private AnnotationVisitor record(String descriptor, ElementType type, String member) {
            Map<String, Object> values = new HashMap<>();
            return new ValueCollector(values, map -> scanData.getAnnotations().add(new ModFileScanData.AnnotationData(Type.getType(descriptor), type, clazz, member, map)));
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            return record(descriptor, ElementType.TYPE, clazz.getClassName());
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            return new FieldVisitor(Opcodes.ASM9) {
                @Override
                public AnnotationVisitor visitAnnotation(String annotationDescriptor, boolean visible) {
                    return record(annotationDescriptor, ElementType.FIELD, name);
                }
            };
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            return new MethodVisitor(Opcodes.ASM9) {
                @Override
                public AnnotationVisitor visitAnnotation(String annotationDescriptor, boolean visible) {
                    return record(annotationDescriptor, ElementType.METHOD, name + descriptor);
                }
            };
        }
    }

    private static final class ValueCollector extends AnnotationVisitor {

        private final Map<String, Object> values;
        private final Consumer<Map<String, Object>> onEnd;

        private ValueCollector(Map<String, Object> values, Consumer<Map<String, Object>> onEnd) {
            super(Opcodes.ASM9);
            this.values = values;
            this.onEnd = onEnd;
        }

        @Override
        public void visit(String name, Object value) {
            values.put(name, value);
        }

        @Override
        public void visitEnum(String name, String descriptor, String value) {
            values.put(name, new ModAnnotation.EnumHolder(descriptor, value));
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String descriptor) {
            Map<String, Object> nested = new HashMap<>();
            return new ValueCollector(nested, map -> values.put(name, map));
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            List<Object> array = new ArrayList<>();
            return new AnnotationVisitor(Opcodes.ASM9) {
                @Override
                public void visit(String ignored, Object value) {
                    array.add(value);
                }

                @Override
                public void visitEnum(String ignored, String descriptor, String value) {
                    array.add(new ModAnnotation.EnumHolder(descriptor, value));
                }

                @Override
                public AnnotationVisitor visitAnnotation(String ignored, String descriptor) {
                    Map<String, Object> nested = new HashMap<>();
                    array.add(nested);
                    return new ValueCollector(nested, map -> {
                    });
                }

                @Override
                public void visitEnd() {
                    values.put(name, array);
                }
            };
        }

        @Override
        public void visitEnd() {
            onEnd.accept(values);
        }
    }
}
