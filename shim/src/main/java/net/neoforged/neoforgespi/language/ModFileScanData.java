/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforgespi.language;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.objectweb.asm.Type;

/**
 * Mekanism shim: Annotation scan data of a mod file, provided by the loader.
 */
public class ModFileScanData {

    private final Set<AnnotationData> annotations = new LinkedHashSet<>();
    private final List<IModFileInfo> modFiles = new ArrayList<>();

    public Set<AnnotationData> getAnnotations() {
        return annotations;
    }

    public Stream<AnnotationData> getAnnotatedBy(Class<? extends Annotation> type, ElementType elementType) {
        Type annotationType = Type.getType(type);
        return annotations.stream().filter(data -> data.targetType() == elementType && data.annotationType().equals(annotationType));
    }

    public void addModFileInfo(IModFileInfo info) {
        modFiles.add(info);
    }

    public List<IModFileInfo> getIModInfoData() {
        return modFiles;
    }

    public record AnnotationData(Type annotationType, ElementType targetType, Type clazz, String memberName, Map<String, Object> annotationData) {
    }
}
