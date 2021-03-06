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
package com.linkedin.pinot.core.query.aggregation.groupby.utils;

import com.linkedin.pinot.common.data.FieldSpec;


/**
 * Factory for various implementations for {@link ValueToIdMap}
 */
public class ValueToIdMapFactory {
  // Private constructor to prevent instantiating the class.
  private ValueToIdMapFactory() {
  }

  public static ValueToIdMap get(FieldSpec.DataType dataType) {
    switch (dataType) {
      case INT:
        return new IntToIdMap();

      case LONG:
        return new LongToIdMap();

      case FLOAT:
        return new FloatToIdMap();

      case DOUBLE:
        return new DoubleToIdMap();

      case STRING:
        return new StringToIdMap();

      default:
        throw new IllegalArgumentException("Illegal data type for ValueToIdMapFactory: " + dataType);
    }
  }
}
