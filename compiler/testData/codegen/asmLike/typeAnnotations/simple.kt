// KOTLIN_CONFIGURATION_FLAGS: +JVM.EMIT_JVM_TYPE_ANNOTATIONS
// TYPE_ANNOTATIONS
// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_RUNTIME
// WITH_REFLECT
// FULL_JDK
package foo

import java.lang.reflect.AnnotatedType
import kotlin.reflect.jvm.javaMethod
import kotlin.test.fail

@Target(AnnotationTarget.TYPE)
annotation class TypeAnn

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.BINARY)
annotation class TypeAnnBinary

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class TypeAnnSource

class Kotlin {

    fun foo(s: @TypeAnn @TypeAnnBinary @TypeAnnSource String) {
    }

    fun foo2(): @TypeAnn @TypeAnnBinary @TypeAnnSource String {
        return "OK"
    }

    fun fooArray(s: Array<@TypeAnn @TypeAnnBinary @TypeAnnSource String>) {
    }

    fun fooArray2(): Array<@TypeAnn @TypeAnnBinary @TypeAnnSource String>? {
        return null
    }

}
