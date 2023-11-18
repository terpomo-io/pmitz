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

package io.terpomo.pmitz.core.repository.feature;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.Product;
import io.terpomo.pmitz.core.limits.UsageLimit;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;

public interface FeatureRepository {

    List<Feature> getFeatures (Product product);

    Optional<Feature> getFeature (Product product, String featureId);

    Optional<UsageLimit> getGlobalLimit (Feature feature, String usageLimitId);

    void addFeature (Feature feature);

    void updateFeature (Feature feature);

    void removeFeature (Feature feature);

    void clear();

    void load(InputStream inputStream) throws IOException;

    void store(OutputStream outputStream) throws IOException;

}
