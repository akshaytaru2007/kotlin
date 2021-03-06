/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.library.lite

import org.jetbrains.kotlin.konan.CompilerVersion
import java.io.File

class LiteKonanDistribution(
    val distributionHome: File,
    val konanVersion: CompilerVersion,
    val konanVersionString: String
)
