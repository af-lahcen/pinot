/**
 * Copyright (C) 2014-2018 LinkedIn Corp. (pinot-core@linkedin.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.linkedin.thirdeye.rootcause.impl;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.testng.Assert;
import org.testng.annotations.Test;


public class DimensionsEntityTest {
  @Test
  public void testWithoutDimensions() {
    DimensionsEntity e = DimensionsEntity.fromDimensions(1.0, ArrayListMultimap.<String, String>create());
    Assert.assertEquals(e.getUrn(), "thirdeye:dimensions:");
    Assert.assertTrue(e.getDimensions().isEmpty());
  }

  @Test
  public void testWithoutDimensionsUrn() {
    DimensionsEntity e = DimensionsEntity.fromURN("thirdeye:dimensions:", 1.0);
    Assert.assertTrue(e.getDimensions().isEmpty());
  }

  @Test
  public void testEncode() {
    Multimap<String, String> dimensions = ArrayListMultimap.create();
    dimensions.put("key", "value!");
    dimensions.put("key", "other=Value");
    dimensions.put("otherKey", "another:Value");

    DimensionsEntity e = DimensionsEntity.fromDimensions(1.0, dimensions);

    Assert.assertEquals(e.getUrn(), "thirdeye:dimensions:key%3Dother%3DValue:key%3Dvalue!:otherKey%3Danother%3AValue");
  }

  @Test
  public void testDecode() {
    final String urn = "thirdeye:dimensions:key%3Dother%3DValue:key%3Dvalue!:otherKey%3Danother%3AValue";

    DimensionsEntity e = DimensionsEntity.fromURN(urn, 1.0);

    Assert.assertEquals(e.getDimensions().size(), 3);
    Assert.assertEquals(e.getDimensions().get("key").size(), 2);
    Assert.assertTrue(e.getDimensions().get("key").contains("value!"));
    Assert.assertTrue(e.getDimensions().get("key").contains("other=Value"));
    Assert.assertEquals(e.getDimensions().get("otherKey").size(), 1);
    Assert.assertTrue(e.getDimensions().get("otherKey").contains("another:Value"));
  }

  @Test
  public void testDecodePlain() {
    final String urn = "thirdeye:dimensions:key=other=Value:key=value!:otherKey=another/Value";

    DimensionsEntity e = DimensionsEntity.fromURN(urn, 1.0);

    Assert.assertEquals(e.getDimensions().size(), 3);
    Assert.assertEquals(e.getDimensions().get("key").size(), 2);
    Assert.assertTrue(e.getDimensions().get("key").contains("value!"));
    Assert.assertTrue(e.getDimensions().get("key").contains("other=Value"));
    Assert.assertEquals(e.getDimensions().get("otherKey").size(), 1);
    Assert.assertTrue(e.getDimensions().get("otherKey").contains("another/Value"));
  }

  @Test
  public void testDuplicateDimensionsUrn() {
    final String urn = "thirdeye:dimensions:key=value:key=value";

    DimensionsEntity e = DimensionsEntity.fromURN(urn, 1.0);

    Assert.assertEquals(e.getDimensions().size(), 1);
    Assert.assertTrue(e.getDimensions().get("key").contains("value"));
  }
}
