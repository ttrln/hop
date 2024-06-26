/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hop.beam.util;

import java.util.List;
import org.apache.hop.beam.metadata.FieldDefinition;
import org.apache.hop.beam.metadata.FileDefinition;
import org.apache.hop.beam.transform.PipelineTestBase;
import org.apache.hop.beam.transforms.io.BeamInputMeta;
import org.apache.hop.beam.transforms.io.BeamOutputMeta;
import org.apache.hop.core.Condition;
import org.apache.hop.core.row.IValueMeta;
import org.apache.hop.core.row.ValueMetaAndData;
import org.apache.hop.metadata.api.IHopMetadataProvider;
import org.apache.hop.metadata.api.IHopMetadataSerializer;
import org.apache.hop.pipeline.PipelineHopMeta;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.TransformMeta;
import org.apache.hop.pipeline.transforms.constant.ConstantField;
import org.apache.hop.pipeline.transforms.constant.ConstantMeta;
import org.apache.hop.pipeline.transforms.dummy.DummyMeta;
import org.apache.hop.pipeline.transforms.filterrows.FilterRowsMeta;
import org.apache.hop.pipeline.transforms.memgroupby.GAggregate;
import org.apache.hop.pipeline.transforms.memgroupby.GGroup;
import org.apache.hop.pipeline.transforms.memgroupby.MemoryGroupByMeta;
import org.apache.hop.pipeline.transforms.mergejoin.MergeJoinMeta;
import org.apache.hop.pipeline.transforms.streamlookup.StreamLookupMeta;
import org.apache.hop.pipeline.transforms.switchcase.SwitchCaseMeta;
import org.apache.hop.pipeline.transforms.switchcase.SwitchCaseTarget;

public class BeamPipelineMetaUtil {

  public static final PipelineMeta generateBeamInputOutputPipelineMeta(
      String pipelineName,
      String inputTransformName,
      String outputTransformName,
      IHopMetadataProvider metadataProvider)
      throws Exception {

    IHopMetadataSerializer<FileDefinition> serializer =
        metadataProvider.getSerializer(FileDefinition.class);
    FileDefinition customerFileDefinition = createCustomersInputFileDefinition();
    serializer.save(customerFileDefinition);

    PipelineMeta pipelineMeta = new PipelineMeta();
    pipelineMeta.setName(pipelineName);
    pipelineMeta.setMetadataProvider(metadataProvider);

    // Add the io transform
    //
    BeamInputMeta beamInputMeta = new BeamInputMeta();
    beamInputMeta.setInputLocation(PipelineTestBase.INPUT_CUSTOMERS_FILE);
    beamInputMeta.setFileDefinitionName(customerFileDefinition.getName());
    TransformMeta beamInputTransformMeta = new TransformMeta(inputTransformName, beamInputMeta);
    beamInputTransformMeta.setTransformPluginId("BeamInput");
    pipelineMeta.addTransform(beamInputTransformMeta);

    // Add a dummy in between to get started...
    //
    DummyMeta dummyPipelineMeta = new DummyMeta();
    TransformMeta dummyTransformMeta = new TransformMeta("Dummy", dummyPipelineMeta);
    pipelineMeta.addTransform(dummyTransformMeta);
    pipelineMeta.addPipelineHop(new PipelineHopMeta(beamInputTransformMeta, dummyTransformMeta));

    // Add the output transform
    //
    BeamOutputMeta beamOutputMeta = new BeamOutputMeta();
    beamOutputMeta.setOutputLocation("/tmp/customers/output/");
    beamOutputMeta.setFileDefinitionName(null);
    beamOutputMeta.setFilePrefix("customers");
    beamOutputMeta.setFileSuffix(".csv");
    beamOutputMeta.setWindowed(false); // Not yet supported
    TransformMeta beamOutputTransformMeta = new TransformMeta(outputTransformName, beamOutputMeta);
    beamOutputTransformMeta.setTransformPluginId("BeamOutput");
    pipelineMeta.addTransform(beamOutputTransformMeta);
    pipelineMeta.addPipelineHop(new PipelineHopMeta(dummyTransformMeta, beamOutputTransformMeta));

    return pipelineMeta;
  }

  public static final PipelineMeta generateBeamGroupByPipelineMeta(
      String transname,
      String inputTransformName,
      String outputTransformName,
      IHopMetadataProvider metadataProvider)
      throws Exception {

    IHopMetadataSerializer<FileDefinition> serializer =
        metadataProvider.getSerializer(FileDefinition.class);
    FileDefinition customerFileDefinition = createCustomersInputFileDefinition();
    serializer.save(customerFileDefinition);

    PipelineMeta pipelineMeta = new PipelineMeta();
    pipelineMeta.setName(transname);
    pipelineMeta.setMetadataProvider(metadataProvider);

    // Add the io transform
    //
    BeamInputMeta beamInputMeta = new BeamInputMeta();
    beamInputMeta.setInputLocation(PipelineTestBase.INPUT_CUSTOMERS_FILE);
    beamInputMeta.setFileDefinitionName(customerFileDefinition.getName());
    TransformMeta beamInputTransformMeta = new TransformMeta(inputTransformName, beamInputMeta);
    beamInputTransformMeta.setTransformPluginId(BeamConst.STRING_BEAM_INPUT_PLUGIN_ID);
    pipelineMeta.addTransform(beamInputTransformMeta);

    // Add a dummy in between to get started...
    //
    MemoryGroupByMeta memoryGroupByMeta = new MemoryGroupByMeta();
    memoryGroupByMeta.setGroups(List.of(new GGroup("state")));
    memoryGroupByMeta.setAggregates(
        List.of(
            new GAggregate("nrIds", "id", MemoryGroupByMeta.GroupType.CountAll, null),
            new GAggregate("sumIds", "id", MemoryGroupByMeta.GroupType.Sum, null)));

    TransformMeta memoryGroupByTransformMeta = new TransformMeta("Group By", memoryGroupByMeta);
    pipelineMeta.addTransform(memoryGroupByTransformMeta);
    pipelineMeta.addPipelineHop(
        new PipelineHopMeta(beamInputTransformMeta, memoryGroupByTransformMeta));

    // Add the output transform
    //
    BeamOutputMeta beamOutputMeta = new BeamOutputMeta();
    beamOutputMeta.setOutputLocation("/tmp/customers/output/");
    beamOutputMeta.setFileDefinitionName(null);
    beamOutputMeta.setFilePrefix("grouped");
    beamOutputMeta.setFileSuffix(".csv");
    beamOutputMeta.setWindowed(false); // Not yet supported
    TransformMeta beamOutputTransformMeta = new TransformMeta(outputTransformName, beamOutputMeta);
    beamOutputTransformMeta.setTransformPluginId("BeamOutput");
    pipelineMeta.addTransform(beamOutputTransformMeta);
    pipelineMeta.addPipelineHop(
        new PipelineHopMeta(memoryGroupByTransformMeta, beamOutputTransformMeta));

    return pipelineMeta;
  }

  public static final PipelineMeta generateFilterRowsPipelineMeta(
      String transname,
      String inputTransformName,
      String outputTransformName,
      IHopMetadataProvider metadataProvider)
      throws Exception {

    IHopMetadataSerializer<FileDefinition> serializer =
        metadataProvider.getSerializer(FileDefinition.class);
    FileDefinition customerFileDefinition = createCustomersInputFileDefinition();
    serializer.save(customerFileDefinition);

    PipelineMeta pipelineMeta = new PipelineMeta();
    pipelineMeta.setName(transname);
    pipelineMeta.setMetadataProvider(metadataProvider);

    // Add the io transform
    //
    BeamInputMeta beamInputMeta = new BeamInputMeta();
    beamInputMeta.setInputLocation(PipelineTestBase.INPUT_CUSTOMERS_FILE);
    beamInputMeta.setFileDefinitionName(customerFileDefinition.getName());
    TransformMeta beamInputTransformMeta = new TransformMeta(inputTransformName, beamInputMeta);
    beamInputTransformMeta.setTransformPluginId(BeamConst.STRING_BEAM_INPUT_PLUGIN_ID);
    pipelineMeta.addTransform(beamInputTransformMeta);

    // Add 2 add constants transforms A and B
    //
    ConstantMeta constantA = new ConstantMeta();
    ConstantField cf1 = new ConstantField("label", "String", "< 'k'");
    constantA.getFields().add(cf1);
    TransformMeta constantAMeta = new TransformMeta("A", constantA);
    pipelineMeta.addTransform(constantAMeta);

    ConstantMeta constantB = new ConstantMeta();
    ConstantField cf2 = new ConstantField("label", "String", ">= 'k'");
    constantB.getFields().add(cf2);
    TransformMeta constantBMeta = new TransformMeta("B", constantB);
    pipelineMeta.addTransform(constantBMeta);

    // Add Filter rows transform looking for customers name > "k"
    // Send rows to A (true) and B (false)
    //
    FilterRowsMeta filter = new FilterRowsMeta();
    filter.getCondition().setLeftValueName("name");
    filter.getCondition().setFunction(Condition.Function.SMALLER);
    filter.getCondition().setRightValue(new Condition.CValue(new ValueMetaAndData("value", "k")));
    filter.setTrueTransformName("A");
    filter.setFalseTransformName("B");
    TransformMeta filterMeta = new TransformMeta("Filter", filter);
    pipelineMeta.addTransform(filterMeta);
    pipelineMeta.addPipelineHop(new PipelineHopMeta(beamInputTransformMeta, filterMeta));
    pipelineMeta.addPipelineHop(new PipelineHopMeta(filterMeta, constantAMeta));
    pipelineMeta.addPipelineHop(new PipelineHopMeta(filterMeta, constantBMeta));

    // Add a dummy behind it all to flatten/merge the data again...
    //
    DummyMeta dummyPipelineMeta = new DummyMeta();
    TransformMeta dummyTransformMeta = new TransformMeta("Flatten", dummyPipelineMeta);
    pipelineMeta.addTransform(dummyTransformMeta);
    pipelineMeta.addPipelineHop(new PipelineHopMeta(constantAMeta, dummyTransformMeta));
    pipelineMeta.addPipelineHop(new PipelineHopMeta(constantBMeta, dummyTransformMeta));

    // Add the output transform
    //
    BeamOutputMeta beamOutputMeta = new BeamOutputMeta();
    beamOutputMeta.setOutputLocation("/tmp/customers/output/");
    beamOutputMeta.setFileDefinitionName(null);
    beamOutputMeta.setFilePrefix("filter-test");
    beamOutputMeta.setFileSuffix(".csv");
    beamOutputMeta.setWindowed(false); // Not yet supported
    TransformMeta beamOutputTransformMeta = new TransformMeta(outputTransformName, beamOutputMeta);
    beamOutputTransformMeta.setTransformPluginId("BeamOutput");
    pipelineMeta.addTransform(beamOutputTransformMeta);
    pipelineMeta.addPipelineHop(new PipelineHopMeta(dummyTransformMeta, beamOutputTransformMeta));

    return pipelineMeta;
  }

  public static final PipelineMeta generateSwitchCasePipelineMeta(
      String transname,
      String inputTransformName,
      String outputTransformName,
      IHopMetadataProvider metadataProvider)
      throws Exception {

    IHopMetadataSerializer<FileDefinition> serializer =
        metadataProvider.getSerializer(FileDefinition.class);
    FileDefinition customerFileDefinition = createCustomersInputFileDefinition();
    serializer.save(customerFileDefinition);

    PipelineMeta pipelineMeta = new PipelineMeta();
    pipelineMeta.setName(transname);
    pipelineMeta.setMetadataProvider(metadataProvider);

    // Add the io transform
    //
    BeamInputMeta beamInputMeta = new BeamInputMeta();
    beamInputMeta.setInputLocation(PipelineTestBase.INPUT_CUSTOMERS_FILE);
    beamInputMeta.setFileDefinitionName(customerFileDefinition.getName());
    TransformMeta beamInputTransformMeta = new TransformMeta(inputTransformName, beamInputMeta);
    beamInputTransformMeta.setTransformPluginId(BeamConst.STRING_BEAM_INPUT_PLUGIN_ID);
    pipelineMeta.addTransform(beamInputTransformMeta);

    // Add 4 add constants transforms CA and FL, NY, Default
    //
    String[] stateCodes = new String[] {"CA", "FL", "NY", "AR", "Default"};
    for (String stateCode : stateCodes) {
      ConstantMeta constant = new ConstantMeta();
      ConstantField cf = new ConstantField("Comment", "String", stateCode + " : some comment");
      constant.getFields().add(cf);
      TransformMeta constantMeta = new TransformMeta(stateCode, constant);
      pipelineMeta.addTransform(constantMeta);
    }

    // Add Switch / Case transform looking switching on stateCode field
    // Send rows to A (true) and B (false)
    //
    SwitchCaseMeta switchCaseMeta = new SwitchCaseMeta();
    switchCaseMeta.setFieldName("stateCode");
    switchCaseMeta.setCaseValueType("String");
    // Last one in the array is the Default target
    //
    for (int i = 0; i < stateCodes.length - 1; i++) {
      String stateCode = stateCodes[i];
      List<SwitchCaseTarget> caseTargets = switchCaseMeta.getCaseTargets();
      SwitchCaseTarget target = new SwitchCaseTarget();
      target.setCaseValue(stateCode);
      target.setCaseTargetTransformName(stateCode);
      caseTargets.add(target);
    }
    switchCaseMeta.setDefaultTargetTransformName(stateCodes[stateCodes.length - 1]);
    switchCaseMeta.searchInfoAndTargetTransforms(pipelineMeta.getTransforms());
    TransformMeta switchCaseTransformMeta = new TransformMeta("Switch/Case", switchCaseMeta);
    pipelineMeta.addTransform(switchCaseTransformMeta);
    pipelineMeta.addPipelineHop(
        new PipelineHopMeta(beamInputTransformMeta, switchCaseTransformMeta));

    for (String stateCode : stateCodes) {
      pipelineMeta.addPipelineHop(
          new PipelineHopMeta(switchCaseTransformMeta, pipelineMeta.findTransform(stateCode)));
    }

    // Add a dummy behind it all to flatten/merge the data again...
    //
    DummyMeta dummyPipelineMeta = new DummyMeta();
    TransformMeta dummyTransformMeta = new TransformMeta("Flatten", dummyPipelineMeta);
    pipelineMeta.addTransform(dummyTransformMeta);

    for (String stateCode : stateCodes) {
      pipelineMeta.addPipelineHop(
          new PipelineHopMeta(pipelineMeta.findTransform(stateCode), dummyTransformMeta));
    }

    // Add the output transform
    //
    BeamOutputMeta beamOutputMeta = new BeamOutputMeta();
    beamOutputMeta.setOutputLocation("/tmp/customers/output/");
    beamOutputMeta.setFileDefinitionName(null);
    beamOutputMeta.setFilePrefix("switch-case-test");
    beamOutputMeta.setFileSuffix(".csv");
    beamOutputMeta.setWindowed(false); // Not yet supported
    TransformMeta beamOutputTransformMeta = new TransformMeta(outputTransformName, beamOutputMeta);
    beamOutputTransformMeta.setTransformPluginId("BeamOutput");
    pipelineMeta.addTransform(beamOutputTransformMeta);
    pipelineMeta.addPipelineHop(new PipelineHopMeta(dummyTransformMeta, beamOutputTransformMeta));

    return pipelineMeta;
  }

  public static final PipelineMeta generateStreamLookupPipelineMeta(
      String transname,
      String inputTransformName,
      String outputTransformName,
      IHopMetadataProvider metadataProvider)
      throws Exception {

    IHopMetadataSerializer<FileDefinition> serializer =
        metadataProvider.getSerializer(FileDefinition.class);
    FileDefinition customerFileDefinition = createCustomersInputFileDefinition();
    serializer.save(customerFileDefinition);

    PipelineMeta pipelineMeta = new PipelineMeta();
    pipelineMeta.setName(transname);
    pipelineMeta.setMetadataProvider(metadataProvider);

    // Add the main io transform
    //
    BeamInputMeta beamInputMeta = new BeamInputMeta();
    beamInputMeta.setInputLocation(PipelineTestBase.INPUT_CUSTOMERS_FILE);
    beamInputMeta.setFileDefinitionName(customerFileDefinition.getName());
    TransformMeta beamInputTransformMeta = new TransformMeta(inputTransformName, beamInputMeta);
    beamInputTransformMeta.setTransformPluginId(BeamConst.STRING_BEAM_INPUT_PLUGIN_ID);
    pipelineMeta.addTransform(beamInputTransformMeta);

    TransformMeta lookupBeamInputTransformMeta = beamInputTransformMeta;

    // Add a Memory Group By transform which will
    MemoryGroupByMeta memoryGroupByMeta = new MemoryGroupByMeta();
    memoryGroupByMeta.setGroups(List.of(new GGroup("stateCode")));
    memoryGroupByMeta.setAggregates(
        List.of(
            new GAggregate(
                "rowsPerState", "stateCode", MemoryGroupByMeta.GroupType.CountAll, null)));
    TransformMeta memoryGroupByTransformMeta = new TransformMeta("rowsPerState", memoryGroupByMeta);
    pipelineMeta.addTransform(memoryGroupByTransformMeta);
    pipelineMeta.addPipelineHop(
        new PipelineHopMeta(lookupBeamInputTransformMeta, memoryGroupByTransformMeta));

    // Add a Stream Lookup transform ...
    //
    StreamLookupMeta streamLookupMeta = new StreamLookupMeta();
    streamLookupMeta.allocate(1, 1);
    streamLookupMeta.getKeystream()[0] = "stateCode";
    streamLookupMeta.getKeylookup()[0] = "stateCode";
    streamLookupMeta.getValue()[0] = "rowsPerState";
    streamLookupMeta.getValueName()[0] = "nrPerState";
    streamLookupMeta.getValueDefault()[0] = null;
    streamLookupMeta.getValueDefaultType()[0] = IValueMeta.TYPE_INTEGER;
    streamLookupMeta.setMemoryPreservationActive(false);
    streamLookupMeta
        .getTransformIOMeta()
        .getInfoStreams()
        .get(0)
        .setTransformMeta(memoryGroupByTransformMeta); // Read from Mem.GroupBy
    TransformMeta streamLookupTransformMeta = new TransformMeta("Stream Lookup", streamLookupMeta);
    pipelineMeta.addTransform(streamLookupTransformMeta);
    pipelineMeta.addPipelineHop(
        new PipelineHopMeta(beamInputTransformMeta, streamLookupTransformMeta)); // Main io
    pipelineMeta.addPipelineHop(
        new PipelineHopMeta(memoryGroupByTransformMeta, streamLookupTransformMeta)); // info stream

    // Add the output transform to write results
    //
    BeamOutputMeta beamOutputMeta = new BeamOutputMeta();
    beamOutputMeta.setOutputLocation("/tmp/customers/output/");
    beamOutputMeta.setFileDefinitionName(null);
    beamOutputMeta.setFilePrefix("stream-lookup");
    beamOutputMeta.setFileSuffix(".csv");
    beamOutputMeta.setWindowed(false); // Not yet supported
    TransformMeta beamOutputTransformMeta = new TransformMeta(outputTransformName, beamOutputMeta);
    beamOutputTransformMeta.setTransformPluginId("BeamOutput");
    pipelineMeta.addTransform(beamOutputTransformMeta);
    pipelineMeta.addPipelineHop(
        new PipelineHopMeta(streamLookupTransformMeta, beamOutputTransformMeta));

    return pipelineMeta;
  }

  public static final PipelineMeta generateMergeJoinPipelineMeta(
      String pipelineName,
      String inputTransformName,
      String outputTransformName,
      IHopMetadataProvider metadataProvider)
      throws Exception {

    IHopMetadataSerializer<FileDefinition> serializer =
        metadataProvider.getSerializer(FileDefinition.class);
    FileDefinition customerFileDefinition = createCustomersInputFileDefinition();
    serializer.save(customerFileDefinition);
    FileDefinition statePopulationFileDefinition = createStatePopulationInputFileDefinition();
    serializer.save(statePopulationFileDefinition);

    PipelineMeta pipelineMeta = new PipelineMeta();
    pipelineMeta.setName(pipelineName);
    pipelineMeta.setMetadataProvider(metadataProvider);

    // Add the left io transform
    //
    BeamInputMeta leftInputMeta = new BeamInputMeta();
    leftInputMeta.setInputLocation(PipelineTestBase.INPUT_CUSTOMERS_FILE);
    leftInputMeta.setFileDefinitionName(customerFileDefinition.getName());
    TransformMeta leftInputTransformMeta =
        new TransformMeta(inputTransformName + " Left", leftInputMeta);
    leftInputTransformMeta.setTransformPluginId(BeamConst.STRING_BEAM_INPUT_PLUGIN_ID);
    pipelineMeta.addTransform(leftInputTransformMeta);

    BeamInputMeta rightInputMeta = new BeamInputMeta();
    rightInputMeta.setInputLocation(PipelineTestBase.INPUT_STATES_FILE);
    rightInputMeta.setFileDefinitionName(statePopulationFileDefinition.getName());
    TransformMeta rightInputTransformMeta =
        new TransformMeta(inputTransformName + " Right", rightInputMeta);
    rightInputTransformMeta.setTransformPluginId(BeamConst.STRING_BEAM_INPUT_PLUGIN_ID);
    pipelineMeta.addTransform(rightInputTransformMeta);

    // Add a Merge Join transform
    //
    MergeJoinMeta mergeJoin = new MergeJoinMeta();
    mergeJoin.getKeyFields1().add("state");
    mergeJoin.getKeyFields2().add("state");
    mergeJoin.setJoinType(MergeJoinMeta.joinTypes[3]); // FULL OUTER
    mergeJoin.setLeftTransformName(leftInputTransformMeta.getName());
    mergeJoin.setRightTransformName(rightInputTransformMeta.getName());
    TransformMeta mergeJoinTransformMeta = new TransformMeta("Merge Join", mergeJoin);
    pipelineMeta.addTransform(mergeJoinTransformMeta);
    pipelineMeta.addPipelineHop(
        new PipelineHopMeta(leftInputTransformMeta, mergeJoinTransformMeta));
    pipelineMeta.addPipelineHop(
        new PipelineHopMeta(rightInputTransformMeta, mergeJoinTransformMeta));

    // Add the output transform to write results
    //
    BeamOutputMeta beamOutputMeta = new BeamOutputMeta();
    beamOutputMeta.setOutputLocation("/tmp/customers/output/");
    beamOutputMeta.setFileDefinitionName(null);
    beamOutputMeta.setFilePrefix("merge-join");
    beamOutputMeta.setFileSuffix(".csv");
    beamOutputMeta.setWindowed(false); // Not yet supported
    TransformMeta beamOutputTransformMeta = new TransformMeta(outputTransformName, beamOutputMeta);
    beamOutputTransformMeta.setTransformPluginId("BeamOutput");
    pipelineMeta.addTransform(beamOutputTransformMeta);
    pipelineMeta.addPipelineHop(
        new PipelineHopMeta(mergeJoinTransformMeta, beamOutputTransformMeta));

    mergeJoin.searchInfoAndTargetTransforms(pipelineMeta.getTransforms());
    return pipelineMeta;
  }

  public static FileDefinition createCustomersInputFileDefinition() {
    FileDefinition fileDefinition = new FileDefinition();
    fileDefinition.setName("Customers");
    fileDefinition.setDescription("File description of customers.txt");

    // id;name;firstname;zip;city;birthdate;street;housenr;stateCode;state

    fileDefinition.setSeparator(";");
    fileDefinition.setEnclosure(null); // NOT SUPPORTED YET

    List<FieldDefinition> fields = fileDefinition.getFieldDefinitions();
    fields.add(new FieldDefinition("id", "Integer", 9, 0, " #"));
    fields.add(new FieldDefinition("name", "String", 50, 0));
    fields.add(new FieldDefinition("firstname", "String", 50, 0));
    fields.add(new FieldDefinition("zip", "String", 20, 0));
    fields.add(new FieldDefinition("city", "String", 100, 0));
    fields.add(new FieldDefinition("birthdate", "Date", -1, -1, "yyyy/MM/dd"));
    fields.add(new FieldDefinition("street", "String", 100, 0));
    fields.add(new FieldDefinition("housenr", "String", 20, 0));
    fields.add(new FieldDefinition("stateCode", "String", 10, 0));
    fields.add(new FieldDefinition("state", "String", 100, 0));

    return fileDefinition;
  }

  public static FileDefinition createStatePopulationInputFileDefinition() {
    FileDefinition fileDefinition = new FileDefinition();
    fileDefinition.setName("StatePopulation");
    fileDefinition.setDescription("File description of state-data.txt");

    // state;population

    fileDefinition.setSeparator(";");
    fileDefinition.setEnclosure(null); // NOT SUPPORTED YET

    List<FieldDefinition> fields = fileDefinition.getFieldDefinitions();
    fields.add(new FieldDefinition("state", "String", 100, 0));
    fields.add(new FieldDefinition("population", "Integer", 9, 0, "#"));

    return fileDefinition;
  }
}
