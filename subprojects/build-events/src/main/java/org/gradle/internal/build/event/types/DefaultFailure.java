/*
 * Copyright 2019 the original author or authors.
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
package org.gradle.internal.build.event.types;

import org.gradle.internal.exceptions.MultiCauseException;
import org.gradle.tooling.internal.protocol.InternalFailure;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class DefaultFailure implements Serializable, InternalFailure {

    private final String message;
    private final String description;
    private final List<InternalFailure> causes;

    DefaultFailure(String message, String description, List<InternalFailure> causes) {
        this.message = message;
        this.description = description;
        this.causes = causes;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public List<? extends InternalFailure> getCauses() {
        return causes;
    }

    public static InternalFailure fromThrowable(Throwable t) {
        return fromThrowable(t, (throwable, failure) -> {});
    }

    public static InternalFailure fromThrowable(Throwable t, BiConsumer<Throwable, InternalFailure> consumer) {
        StringWriter out = new StringWriter();
        PrintWriter wrt = new PrintWriter(out);
        t.printStackTrace(wrt);
        Throwable cause = t.getCause();
        List<InternalFailure> causeFailure;
        if (cause == null) {
            causeFailure = Collections.emptyList();
        } else if (cause instanceof MultiCauseException) {
            causeFailure = ((MultiCauseException) cause).getCauses().stream().map(c -> fromThrowable(c, consumer)).collect(Collectors.toList());
        } else {
            causeFailure = Collections.singletonList(fromThrowable(cause, consumer));
        }
        DefaultFailure result = new DefaultFailure(t.getMessage(), out.toString(), causeFailure);
        consumer.accept(t, result);
        return result;
    }
}
