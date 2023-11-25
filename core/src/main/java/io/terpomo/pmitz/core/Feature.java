/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.terpomo.pmitz.core;

import io.terpomo.pmitz.core.limits.UsageLimit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Feature {

    private String featureId;

    private Product product;

    private List<UsageLimit> limits;

    private Feature() {}

    public Feature(Product product, String featureId) {

        this.product = product;
        this.featureId = featureId;
        this.limits = new ArrayList<>();
    }

    public String getFeatureId() {
        return featureId;
    }

    public Product getProduct() {
        return product;
    }

    public List<UsageLimit> getLimits() {
        return limits;
    }

    public List<String> getLimitsIds() {
        return limits == null ? Collections.emptyList() :
                limits.stream().map(UsageLimit::getId).collect(Collectors.toList());
    }
}
