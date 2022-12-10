/*
 * Copyright 2022 the original author or authors.
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

package org.gradle.api.internal.tasks.schema;

import org.gradle.api.services.ServiceReference;
import org.gradle.internal.execution.schema.AbstractWorkPropertySchema;
import org.gradle.internal.properties.annotations.PropertyMetadata;

public class DefaultServiceReferencePropertySchema extends AbstractWorkPropertySchema implements ServiceReferencePropertySchema {
    public DefaultServiceReferencePropertySchema(String qualifiedName, PropertyMetadata metadata, Object parent) {
        super(qualifiedName, metadata, parent);
    }

    @Override
    public String getServiceName() {
        String serviceName = ((ServiceReference) getMetadata().getPropertyAnnotation()).value();
        return serviceName.isEmpty()
            ? null
            : serviceName;
    }
}
