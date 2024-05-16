/*
 * Copyright 2024 the original author or authors.
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

package org.gradle.internal.declarativedsl.software

import org.gradle.api.internal.project.ProjectInternal
import org.gradle.declarative.dsl.schema.ConfigureAccessor
import org.gradle.declarative.dsl.schema.DataConstructor
import org.gradle.declarative.dsl.schema.SchemaMemberFunction
import org.gradle.internal.declarativedsl.analysis.ConfigureAccessorInternal
import org.gradle.internal.declarativedsl.analysis.DefaultDataMemberFunction
import org.gradle.internal.declarativedsl.analysis.FunctionSemanticsInternal
import org.gradle.internal.declarativedsl.conventions.ConventionsConfiguringBlock
import org.gradle.internal.declarativedsl.conventions.ConventionsTopLevelReceiver
import org.gradle.internal.declarativedsl.evaluationSchema.EvaluationSchemaComponent
import org.gradle.internal.declarativedsl.mappingToJvm.RuntimeCustomAccessors
import org.gradle.internal.declarativedsl.evaluationSchema.FixedTypeDiscovery
import org.gradle.internal.declarativedsl.schemaBuilder.DataSchemaBuilder
import org.gradle.internal.declarativedsl.schemaBuilder.FunctionExtractor
import org.gradle.internal.declarativedsl.schemaBuilder.TypeDiscovery
import org.gradle.internal.declarativedsl.schemaBuilder.toDataTypeRef
import org.gradle.plugin.software.internal.SoftwareTypeImplementation
import org.gradle.plugin.software.internal.SoftwareTypeRegistry
import kotlin.reflect.KClass
import kotlin.reflect.KFunction


internal
class SoftwareTypeComponent(
    private val schemaTypeToExtend: KClass<*>,
    private val accessorIdPrefix: String,
    softwareTypeRegistry: SoftwareTypeRegistry
) : EvaluationSchemaComponent {
    private
    val softwareTypeImplementations = softwareTypeRegistry.getSoftwareTypeImplementations().map {
        SoftwareTypeInfo(it, schemaTypeToExtend, accessorIdPrefix) { receiverObject ->
            require(receiverObject is ProjectInternal) { "unexpected receiver, expected a ProjectInternal instance, got $receiverObject" }
            receiverObject.pluginManager.apply(it.pluginClass)
            receiverObject.extensions.getByName(it.softwareType)
        }
    }

    override fun typeDiscovery(): List<TypeDiscovery> = listOf(
        FixedTypeDiscovery(schemaTypeToExtend, softwareTypeImplementations.map { it.modelPublicType.kotlin })
    )

    override fun functionExtractors(): List<FunctionExtractor> = listOf(
        softwareTypeConfiguringFunctions(schemaTypeToExtend, softwareTypeImplementations)
    )

    override fun runtimeCustomAccessors(): List<RuntimeCustomAccessors> = listOf(
        RuntimeModelTypeAccessors(softwareTypeImplementations)
    )
}


private
val conventionsFunction = conventionsFunction()


internal
class SoftwareTypeConventionComponent(
    private val schemaTypeToExtend: KClass<*>,
    private val accessorIdPrefix: String,
    softwareTypeRegistry: SoftwareTypeRegistry
) : EvaluationSchemaComponent {
    private
    val softwareTypeImplementations = softwareTypeRegistry.getSoftwareTypeImplementations().map {
        SoftwareTypeInfo(it, schemaTypeToExtend, accessorIdPrefix) { receiverObject ->
            require(receiverObject is ConventionsConfiguringBlock) { "unexpected receiver, expected a ConventionsConfiguringBlock instance, got $receiverObject" }
            null
        }
    }

    override fun typeDiscovery(): List<TypeDiscovery> = listOf(
        FixedTypeDiscovery(ConventionsTopLevelReceiver::class, listOf(ConventionsConfiguringBlock::class)),
        FixedTypeDiscovery(schemaTypeToExtend, softwareTypeImplementations.map { it.modelPublicType.kotlin })
    )

    override fun functionExtractors(): List<FunctionExtractor> = listOf(
        conventionsFunction,
        softwareTypeConfiguringFunctions(schemaTypeToExtend, softwareTypeImplementations),
    )

    override fun runtimeCustomAccessors(): List<RuntimeCustomAccessors> = emptyList()
}


private
data class SoftwareTypeInfo<T>(
    val delegate: SoftwareTypeImplementation<T>,
    val schemaTypeToExtend: KClass<*>,
    val accessorIdPrefix: String,
    val extensionProvider: (receiverObject: Any) -> Any?
) : SoftwareTypeImplementation<T> by delegate {
    val customAccessorId = "$accessorIdPrefix:${delegate.softwareType}"

    val schemaFunction = DefaultDataMemberFunction(
        schemaTypeToExtend.toDataTypeRef(),
        delegate.softwareType,
        emptyList(),
        isDirectAccessOnly = true,
        semantics = FunctionSemanticsInternal.DefaultAccessAndConfigure(
            accessor = ConfigureAccessorInternal.DefaultCustom(delegate.modelPublicType.kotlin.toDataTypeRef(), customAccessorId),
            FunctionSemanticsInternal.DefaultAccessAndConfigure.DefaultReturnType.DefaultUnit,
            FunctionSemanticsInternal.DefaultConfigureBlockRequirement.DefaultRequired
        )
    )
}


private
fun softwareTypeConfiguringFunctions(typeToExtend: KClass<*>, softwareTypeImplementations: Iterable<SoftwareTypeInfo<*>>): FunctionExtractor = object : FunctionExtractor {
    override fun memberFunctions(kClass: KClass<*>, preIndex: DataSchemaBuilder.PreIndex): Iterable<SchemaMemberFunction> =
        if (kClass == typeToExtend) softwareTypeImplementations.map(SoftwareTypeInfo<*>::schemaFunction) else emptyList()

    override fun constructors(kClass: KClass<*>, preIndex: DataSchemaBuilder.PreIndex): Iterable<DataConstructor> = emptyList()

    override fun topLevelFunction(function: KFunction<*>, preIndex: DataSchemaBuilder.PreIndex) = null
}


private
fun conventionsFunction(): FunctionExtractor = object : FunctionExtractor {
    override fun memberFunctions(kClass: KClass<*>, preIndex: DataSchemaBuilder.PreIndex): Iterable<SchemaMemberFunction> =
        if (kClass == ConventionsTopLevelReceiver::class) listOf(
            DefaultDataMemberFunction(
                ConventionsTopLevelReceiver::class.toDataTypeRef(),
                "conventions",
                emptyList(),
                isDirectAccessOnly = true,
                semantics = FunctionSemanticsInternal.DefaultAccessAndConfigure(
                    accessor = ConfigureAccessorInternal.DefaultCustom(ConventionsConfiguringBlock::class.toDataTypeRef(), "conventions"),
                    FunctionSemanticsInternal.DefaultAccessAndConfigure.DefaultReturnType.DefaultUnit,
                    FunctionSemanticsInternal.DefaultConfigureBlockRequirement.DefaultRequired
                )
            )
        ) else emptyList()

    override fun constructors(kClass: KClass<*>, preIndex: DataSchemaBuilder.PreIndex): Iterable<DataConstructor> = emptyList()

    override fun topLevelFunction(function: KFunction<*>, preIndex: DataSchemaBuilder.PreIndex) = null
}


private
class RuntimeModelTypeAccessors(info: List<SoftwareTypeInfo<*>>) : RuntimeCustomAccessors {

    val modelTypeById = info.associate { it.customAccessorId to it.extensionProvider }

    override fun getObjectFromCustomAccessor(receiverObject: Any, accessor: ConfigureAccessor.Custom): Any? =
        modelTypeById[accessor.customAccessorIdentifier]?.invoke(receiverObject)
}
