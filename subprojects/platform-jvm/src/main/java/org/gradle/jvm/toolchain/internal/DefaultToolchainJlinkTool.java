/*
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.jvm.toolchain.internal;

import com.google.common.base.Preconditions;
import org.gradle.api.file.RegularFile;
import org.gradle.jvm.toolchain.JavaInstallationMetadata;
import org.gradle.jvm.toolchain.JlinkTool;
import org.gradle.jvm.toolchain.JvmTool;

public final class DefaultToolchainJlinkTool implements JlinkTool {

    private final JvmTool jvmTool;

    public DefaultToolchainJlinkTool(JavaToolchain javaToolchain) {
        Preconditions.checkArgument(javaToolchain.getLanguageVersion().asInt() >= 9, "jlink is only available on JDK 9 and above");
        this.jvmTool = new DefaultToolchainJvmTool(javaToolchain, "jlink");

    }

    @Override
    public JavaInstallationMetadata getMetadata() {
        return jvmTool.getMetadata();
    }

    @Override
    public RegularFile getExecutablePath() {
        return jvmTool.getExecutablePath();
    }
}