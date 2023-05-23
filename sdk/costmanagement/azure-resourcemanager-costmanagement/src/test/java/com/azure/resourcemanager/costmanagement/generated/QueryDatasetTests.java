// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
// Code generated by Microsoft (R) AutoRest Code Generator.

package com.azure.resourcemanager.costmanagement.generated;

import com.azure.core.util.BinaryData;
import com.azure.resourcemanager.costmanagement.models.FunctionType;
import com.azure.resourcemanager.costmanagement.models.GranularityType;
import com.azure.resourcemanager.costmanagement.models.QueryAggregation;
import com.azure.resourcemanager.costmanagement.models.QueryColumnType;
import com.azure.resourcemanager.costmanagement.models.QueryComparisonExpression;
import com.azure.resourcemanager.costmanagement.models.QueryDataset;
import com.azure.resourcemanager.costmanagement.models.QueryDatasetConfiguration;
import com.azure.resourcemanager.costmanagement.models.QueryFilter;
import com.azure.resourcemanager.costmanagement.models.QueryGrouping;
import com.azure.resourcemanager.costmanagement.models.QueryOperatorType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;

public final class QueryDatasetTests {
    @org.junit.jupiter.api.Test
    public void testDeserialize() throws Exception {
        QueryDataset model =
            BinaryData
                .fromString(
                    "{\"granularity\":\"Daily\",\"configuration\":{\"columns\":[\"fvhqc\",\"a\",\"lvpnpp\",\"uflrwd\"]},\"aggregation\":{\"agafcnihgwqap\":{\"name\":\"lxyjr\",\"function\":\"Sum\"},\"keqdcvdrhvoods\":{\"name\":\"edgfbcvkcvq\",\"function\":\"Sum\"}},\"grouping\":[{\"type\":\"Dimension\",\"name\":\"bzdopcj\"},{\"type\":\"Dimension\",\"name\":\"nhdldwmgxcx\"},{\"type\":\"Dimension\",\"name\":\"lpmutwuoegrpkhj\"},{\"type\":\"TagKey\",\"name\":\"iyq\"}],\"filter\":{\"and\":[{\"and\":[],\"or\":[]}],\"or\":[{\"and\":[],\"or\":[]},{\"and\":[],\"or\":[]},{\"and\":[],\"or\":[]},{\"and\":[],\"or\":[]}],\"dimensions\":{\"name\":\"fy\",\"operator\":\"In\",\"values\":[\"pfvmwyhrfou\",\"ft\"]},\"tags\":{\"name\":\"kcpwiy\",\"operator\":\"In\",\"values\":[\"tmnubexkpzksmon\",\"jmquxvypomgk\",\"pkwhojvpa\"]}}}")
                .toObject(QueryDataset.class);
        Assertions.assertEquals(GranularityType.DAILY, model.granularity());
        Assertions.assertEquals("fvhqc", model.configuration().columns().get(0));
        Assertions.assertEquals("lxyjr", model.aggregation().get("agafcnihgwqap").name());
        Assertions.assertEquals(FunctionType.SUM, model.aggregation().get("agafcnihgwqap").function());
        Assertions.assertEquals(QueryColumnType.DIMENSION, model.grouping().get(0).type());
        Assertions.assertEquals("bzdopcj", model.grouping().get(0).name());
        Assertions.assertEquals("fy", model.filter().dimensions().name());
        Assertions.assertEquals(QueryOperatorType.IN, model.filter().dimensions().operator());
        Assertions.assertEquals("pfvmwyhrfou", model.filter().dimensions().values().get(0));
        Assertions.assertEquals("kcpwiy", model.filter().tags().name());
        Assertions.assertEquals(QueryOperatorType.IN, model.filter().tags().operator());
        Assertions.assertEquals("tmnubexkpzksmon", model.filter().tags().values().get(0));
    }

    @org.junit.jupiter.api.Test
    public void testSerialize() throws Exception {
        QueryDataset model =
            new QueryDataset()
                .withGranularity(GranularityType.DAILY)
                .withConfiguration(
                    new QueryDatasetConfiguration().withColumns(Arrays.asList("fvhqc", "a", "lvpnpp", "uflrwd")))
                .withAggregation(
                    mapOf(
                        "agafcnihgwqap",
                        new QueryAggregation().withName("lxyjr").withFunction(FunctionType.SUM),
                        "keqdcvdrhvoods",
                        new QueryAggregation().withName("edgfbcvkcvq").withFunction(FunctionType.SUM)))
                .withGrouping(
                    Arrays
                        .asList(
                            new QueryGrouping().withType(QueryColumnType.DIMENSION).withName("bzdopcj"),
                            new QueryGrouping().withType(QueryColumnType.DIMENSION).withName("nhdldwmgxcx"),
                            new QueryGrouping().withType(QueryColumnType.DIMENSION).withName("lpmutwuoegrpkhj"),
                            new QueryGrouping().withType(QueryColumnType.TAG_KEY).withName("iyq")))
                .withFilter(
                    new QueryFilter()
                        .withAnd(Arrays.asList(new QueryFilter().withAnd(Arrays.asList()).withOr(Arrays.asList())))
                        .withOr(
                            Arrays
                                .asList(
                                    new QueryFilter().withAnd(Arrays.asList()).withOr(Arrays.asList()),
                                    new QueryFilter().withAnd(Arrays.asList()).withOr(Arrays.asList()),
                                    new QueryFilter().withAnd(Arrays.asList()).withOr(Arrays.asList()),
                                    new QueryFilter().withAnd(Arrays.asList()).withOr(Arrays.asList())))
                        .withDimensions(
                            new QueryComparisonExpression()
                                .withName("fy")
                                .withOperator(QueryOperatorType.IN)
                                .withValues(Arrays.asList("pfvmwyhrfou", "ft")))
                        .withTags(
                            new QueryComparisonExpression()
                                .withName("kcpwiy")
                                .withOperator(QueryOperatorType.IN)
                                .withValues(Arrays.asList("tmnubexkpzksmon", "jmquxvypomgk", "pkwhojvpa"))));
        model = BinaryData.fromObject(model).toObject(QueryDataset.class);
        Assertions.assertEquals(GranularityType.DAILY, model.granularity());
        Assertions.assertEquals("fvhqc", model.configuration().columns().get(0));
        Assertions.assertEquals("lxyjr", model.aggregation().get("agafcnihgwqap").name());
        Assertions.assertEquals(FunctionType.SUM, model.aggregation().get("agafcnihgwqap").function());
        Assertions.assertEquals(QueryColumnType.DIMENSION, model.grouping().get(0).type());
        Assertions.assertEquals("bzdopcj", model.grouping().get(0).name());
        Assertions.assertEquals("fy", model.filter().dimensions().name());
        Assertions.assertEquals(QueryOperatorType.IN, model.filter().dimensions().operator());
        Assertions.assertEquals("pfvmwyhrfou", model.filter().dimensions().values().get(0));
        Assertions.assertEquals("kcpwiy", model.filter().tags().name());
        Assertions.assertEquals(QueryOperatorType.IN, model.filter().tags().operator());
        Assertions.assertEquals("tmnubexkpzksmon", model.filter().tags().values().get(0));
    }

    @SuppressWarnings("unchecked")
    private static <T> Map<String, T> mapOf(Object... inputs) {
        Map<String, T> map = new HashMap<>();
        for (int i = 0; i < inputs.length; i += 2) {
            String key = (String) inputs[i];
            T value = (T) inputs[i + 1];
            map.put(key, value);
        }
        return map;
    }
}