/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hop.pipeline.transforms.metastructure;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.junit.rules.RestoreHopEngineEnvironment;
import org.apache.hop.pipeline.transforms.loadsave.LoadSaveTester;
import org.junit.ClassRule;
import org.junit.Test;

public class TransformMetaStructureMetaTest {
  @ClassRule public static RestoreHopEngineEnvironment env = new RestoreHopEngineEnvironment();

  @Test
  public void testTransformMeta() throws HopException {
    List<String> attributes = Arrays.asList("outputRowcount", "rowcountField");
    // At present, none of the other fields in TransformMetaStructureMeta are being persisted or
    // read
    // All the other fields in the class are set from message bundles and not persisted. See
    // default() method.
    // MB - 5/2016
    Map<String, String> getterMap = new HashMap<>();
    Map<String, String> setterMap = new HashMap<>();

    LoadSaveTester loadSaveTester =
        new LoadSaveTester(TransformMetaStructureMeta.class, attributes, getterMap, setterMap);
    loadSaveTester.testSerialization();
  }
}
