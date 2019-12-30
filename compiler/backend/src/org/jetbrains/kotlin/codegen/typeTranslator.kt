/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.resolve.AnnotationChecker.Companion.applicableTargetSet
import org.jetbrains.kotlin.types.*
import org.jetbrains.org.objectweb.asm.TypePath


class TypePathInfo(
    val path: TypePath?,
    val annotations: List<AnnotationDescriptor>
)

private class State(val type: Int, var pathLength: Int, val path: MutableList<String>) {

    val results = arrayListOf<TypePathInfo>()


    fun addStep(step: String) {
        pathLength++
        path.add(step)
    }

    fun removeStep(step: String) {
        pathLength--
        path.removeAt(path.lastIndex)
    }

    fun rememberAnnotations(annotations: List<AnnotationDescriptor>) {
        results.add(TypePathInfo(TypePath.fromString(path.joinToString("")), annotations))
    }
}

class Translator {

    private lateinit var state: State

    fun collectTypeAnnotations(kotlinType: KotlinType, annotationType: Int): ArrayList<TypePathInfo> {
        state = State(annotationType, 0, arrayListOf())
        kotlinType.collectTypeAnnotations()
        return state.results
    }

    private fun KotlinType.collectTypeAnnotations() {
        if (isFlexible()) {
            return upperIfFlexible().collectTypeAnnotations()
        } else if (this.constructor is AbstractClassTypeConstructor) {
            //skip inner classes for now
            if ((this.constructor.declarationDescriptor as? ClassDescriptor)?.isInner == true) return
        }

        typeAnnotations.takeIf { it.isNotEmpty() }?.let { state.rememberAnnotations(it) }

        arguments.forEachIndexed { index, type ->
            //skip in/out variance for now
            if (type.projectionKind == Variance.INVARIANT) {
                when {
                    KotlinBuiltIns.isArray(this) -> type.type.process("[")
                    else -> type.type.process("$index;")
                }
            }
        }
    }

    fun KotlinType.process(step: String) {
        state.addStep(step)
        this.collectTypeAnnotations()
        state.removeStep(step)
    }


    private val KotlinType.typeAnnotations
        get() = annotations.filter {
            applicableTargetSet(it).contains(KotlinTarget.TYPE) &&
                    it.fqName != KotlinBuiltIns.FQ_NAMES.extensionFunctionType
        }

}