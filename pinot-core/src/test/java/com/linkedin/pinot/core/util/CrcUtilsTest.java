/**
 * Copyright (C) 2014-2015 LinkedIn Corp. (pinot-core@linkedin.com)
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
package com.linkedin.pinot.core.util;

import com.linkedin.pinot.util.TestUtils;
import java.io.File;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.linkedin.pinot.common.segment.ReadMode;
import com.linkedin.pinot.common.segment.SegmentMetadata;
import com.linkedin.pinot.core.chunk.creator.impl.ChunkIndexCreationDriverImplTest;
import com.linkedin.pinot.core.indexsegment.generator.SegmentGeneratorConfig;
import com.linkedin.pinot.core.segment.creator.SegmentIndexCreationDriver;
import com.linkedin.pinot.core.segment.creator.impl.SegmentCreationDriverFactory;
import com.linkedin.pinot.core.segment.index.loader.Loaders;
import com.linkedin.pinot.segments.v1.creator.SegmentTestUtils;


/**
 * Dec 4, 2014
 */

public class CrcUtilsTest {

  private static final String AVRO_DATA = "data/test_data-mv.avro";
  private static File INDEX_DIR = new File("/tmp/testingCrc");

  @Test
  public void test1() throws Exception {
    if (INDEX_DIR.exists()) {
      FileUtils.deleteQuietly(INDEX_DIR);
    }

    final CrcUtils u1 = CrcUtils.forAllFilesInFolder(new File(makeSegmentAndReturnPath()));
    final long crc1 = u1.computeCrc();
    final String md51 = u1.computeMD5();

    FileUtils.deleteQuietly(INDEX_DIR);

    final CrcUtils u2 = CrcUtils.forAllFilesInFolder(new File(makeSegmentAndReturnPath()));
    final long crc2 = u2.computeCrc();
    final String md52 = u2.computeMD5();

    Assert.assertEquals(crc1, crc2);
    Assert.assertEquals(md51, md52);

    FileUtils.deleteQuietly(INDEX_DIR);

    final com.linkedin.pinot.core.indexsegment.IndexSegment segment = Loaders.IndexSegment.load(new File(makeSegmentAndReturnPath()), ReadMode.mmap);
    final SegmentMetadata m = segment.getSegmentMetadata();

    System.out.println(m.getCrc());
    System.out.println(m.getIndexCreationTime());

    FileUtils.deleteQuietly(INDEX_DIR);

  }

  private String makeSegmentAndReturnPath() throws Exception {
    final String filePath = TestUtils
        .getFileFromResourceUrl(ChunkIndexCreationDriverImplTest.class.getClassLoader().getResource(AVRO_DATA));

    final SegmentGeneratorConfig config =
        SegmentTestUtils.getSegmentGenSpecWithSchemAndProjectedColumns(new File(filePath), INDEX_DIR,
            "daysSinceEpoch", TimeUnit.DAYS, "testTable");
    config.setSegmentNamePostfix("1");
    config.setTimeColumnName("daysSinceEpoch");
    final SegmentIndexCreationDriver driver = SegmentCreationDriverFactory.get(null);
    driver.init(config);
    driver.build();

    return new File(INDEX_DIR, driver.getSegmentName()).getAbsolutePath();
  }
}
