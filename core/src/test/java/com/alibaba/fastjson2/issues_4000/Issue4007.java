package com.alibaba.fastjson2.issues_4000;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONPath;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class Issue4007 {
    @Test
    public void test() {
        String json = "{\n"
                + "    \"activityId\": \"9260304192237310917\",\n"
                + "    \"activityRuleDtos\": [\n"
                + "        {\n"
                + "            \"awardId\": \"8260304192237381811\",\n"
                + "            \"promotionRuleDtos\": [\n"
                + "                {\"field\": \"dataCenterUserTagLimit\"},\n"
                + "                {\"field\": \"tradeTime\"},\n"
                + "                {\"field\": \"skuLimit\"}\n"
                + "            ]\n"
                + "        },\n"
                + "        {\n"
                + "            \"awardId\": \"8260304192237401811\",\n"
                + "            \"promotionRuleDtos\": [\n"
                + "                {\"field\": \"tradeTime\"},\n"
                + "                {\"field\": \"skuLimit\"}\n"
                + "            ]\n"
                + "        }\n"
                + "    ]\n"
                + "}";

        String jsonPathExpr = "$.activityRuleDtos[?(@.promotionRuleDtos[?(@.field == 'dataCenterUserTagLimit')])].awardId";
        Object value = JSONPath.extract(json, jsonPathExpr);

        assertNotNull(value);
        JSONArray result = (JSONArray) value;
        assertEquals(1, result.size());
        assertEquals("8260304192237381811", result.getString(0));
    }

    @Test
    public void testNoMatch() {
        String json = "{\n"
                + "    \"activityRuleDtos\": [\n"
                + "        {\n"
                + "            \"awardId\": \"1111\",\n"
                + "            \"promotionRuleDtos\": [\n"
                + "                {\"field\": \"tradeTime\"},\n"
                + "                {\"field\": \"skuLimit\"}\n"
                + "            ]\n"
                + "        },\n"
                + "        {\n"
                + "            \"awardId\": \"2222\",\n"
                + "            \"promotionRuleDtos\": [\n"
                + "                {\"field\": \"tradeTime\"}\n"
                + "            ]\n"
                + "        }\n"
                + "    ]\n"
                + "}";

        String jsonPathExpr = "$.activityRuleDtos[?(@.promotionRuleDtos[?(@.field == 'dataCenterUserTagLimit')])].awardId";
        Object value = JSONPath.extract(json, jsonPathExpr);

        // When no elements match, result is null or empty
        if (value != null) {
            JSONArray result = (JSONArray) value;
            assertEquals(0, result.size());
        }
    }

    @Test
    public void testAllMatch() {
        String json = "{\n"
                + "    \"activityRuleDtos\": [\n"
                + "        {\n"
                + "            \"awardId\": \"1111\",\n"
                + "            \"promotionRuleDtos\": [\n"
                + "                {\"field\": \"dataCenterUserTagLimit\"},\n"
                + "                {\"field\": \"skuLimit\"}\n"
                + "            ]\n"
                + "        },\n"
                + "        {\n"
                + "            \"awardId\": \"2222\",\n"
                + "            \"promotionRuleDtos\": [\n"
                + "                {\"field\": \"dataCenterUserTagLimit\"}\n"
                + "            ]\n"
                + "        }\n"
                + "    ]\n"
                + "}";

        String jsonPathExpr = "$.activityRuleDtos[?(@.promotionRuleDtos[?(@.field == 'dataCenterUserTagLimit')])].awardId";
        Object value = JSONPath.extract(json, jsonPathExpr);

        assertNotNull(value);
        JSONArray result = (JSONArray) value;
        assertEquals(2, result.size());
        assertEquals("1111", result.getString(0));
        assertEquals("2222", result.getString(1));
    }
}
