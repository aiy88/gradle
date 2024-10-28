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

package org.gradle.tooling.events.problems.internal;

import com.google.common.collect.ImmutableList;
import org.gradle.api.NonNullApi;
import org.gradle.tooling.Failure;
import org.gradle.tooling.events.OperationDescriptor;
import org.gradle.tooling.events.internal.BaseProgressEvent;
import org.gradle.tooling.events.problems.AdditionalData;
import org.gradle.tooling.events.problems.ContextualLabel;
import org.gradle.tooling.events.problems.Details;
import org.gradle.tooling.events.problems.Location;
import org.gradle.tooling.events.problems.ProblemDefinition;
import org.gradle.tooling.events.problems.SingleProblemEvent;
import org.gradle.tooling.events.problems.Solution;

import javax.annotation.Nullable;
import java.util.List;

@NonNullApi
public class DefaultSingleProblemEvent extends BaseProgressEvent implements SingleProblemEvent {
    private final ProblemDefinition problemDefinition;
    private final ContextualLabel contextualLabel;
    private final Details details;
    private final List<Location> originLocation;
    private final List<Location> attributedLocations;
    private final List<Solution> solutions;
    private final AdditionalData additionalData;
    private final Failure failure;

    public DefaultSingleProblemEvent(
        long eventTime,
        @Nullable OperationDescriptor problemDescriptor,
        ProblemDefinition problemDefinition,
        @Nullable ContextualLabel contextualLabel,
        @Nullable Details details,
        List<Location> originLocations,
        List<Location> attributedLocations,
        List<Solution> solutions,
        @Nullable AdditionalData additionalData,
        @Nullable Failure failure) {
        super(eventTime, problemDescriptor == null ? "<null>" : problemDescriptor.getDisplayName(), problemDescriptor);
        this.problemDefinition = problemDefinition;
        this.contextualLabel = contextualLabel;
        this.details = details;
        this.originLocation = originLocations;
        this.attributedLocations = attributedLocations;
        this.solutions = solutions;
        this.additionalData = additionalData;
        this.failure = failure;
    }

    @Override
    public ProblemDefinition getDefinition() {
        return problemDefinition;
    }

    @Override
    public ContextualLabel getContextualLabel() {
        return contextualLabel;
    }

    @Override
    public Details getDetails() {
        return details;
    }

    @Override
    public List<Location> getLocations() {
        return ImmutableList
            .<Location>builder()
            .addAll(originLocation)
            .addAll(attributedLocations)
            .build();
    }

    public List<Location> getOriginLocation() {
        return originLocation;
    }

    public List<Location> getAttributedLocations() {
        return attributedLocations;
    }

    @Override
    public List<Solution> getSolutions() {
        return solutions;
    }

    @Nullable
    @Override
    public Failure getFailure() {
        return failure;
    }

    @Override
    public AdditionalData getAdditionalData() {
        return additionalData;
    }
}
