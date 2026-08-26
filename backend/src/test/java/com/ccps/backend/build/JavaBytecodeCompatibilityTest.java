package com.ccps.backend.build;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class JavaBytecodeCompatibilityTest {

    private static final int JAVA_17_CLASS_VERSION = 61;

    @Test
    void compiledMainClassesRemainJava17Compatible() throws IOException {
        Path classesRoot = Path.of(System.getProperty("user.dir"), "target", "classes");
        assertTrue(Files.isDirectory(classesRoot), "Maven main classes directory must exist");

        List<String> incompatibleClasses = new ArrayList<>();
        try (Stream<Path> classFiles = Files.walk(classesRoot)) {
            classFiles
                    .filter(path -> path.toString().endsWith(".class"))
                    .forEach(path -> collectIfIncompatible(classesRoot, path, incompatibleClasses));
        }

        assertTrue(
                incompatibleClasses.isEmpty(),
                () -> "Classes newer than Java 17 were found: " + String.join(", ", incompatibleClasses));
    }

    private void collectIfIncompatible(Path classesRoot, Path classFile, List<String> incompatibleClasses) {
        try (DataInputStream input = new DataInputStream(Files.newInputStream(classFile))) {
            input.readInt();
            input.readUnsignedShort();
            int majorVersion = input.readUnsignedShort();
            if (majorVersion > JAVA_17_CLASS_VERSION) {
                incompatibleClasses.add(classesRoot.relativize(classFile) + " (major " + majorVersion + ")");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to inspect class file " + classFile, exception);
        }
    }
}
