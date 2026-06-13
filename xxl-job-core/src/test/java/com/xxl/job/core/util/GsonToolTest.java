package com.xxl.job.core.util;

import static org.junit.jupiter.api.Assertions.*;

import com.xxl.job.core.biz.model.ReturnT;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * GsonTool unit test
 *
 * @author wuwen
 */
class GsonToolTest {

    @Test
    void testToJsonWithSimpleObject() {
        TestUser user = new TestUser("John", 25);
        String json = GsonTool.toJson(user);

        assertNotNull(json);
        assertTrue(json.contains("\"name\":\"John\""));
        assertTrue(json.contains("\"age\":25"));
    }

    @Test
    void testToJsonWithNull() {
        String json = GsonTool.toJson(null);
        assertEquals("null", json);
    }

    @Test
    void testToJsonWithString() {
        String json = GsonTool.toJson("test");
        assertEquals("\"test\"", json);
    }

    @Test
    void testToJsonWithNumber() {
        String json = GsonTool.toJson(42);
        assertEquals("42", json);
    }

    @Test
    void testToJsonWithList() {
        List<String> list = new ArrayList<>();
        list.add("item1");
        list.add("item2");

        String json = GsonTool.toJson(list);
        assertNotNull(json);
        assertTrue(json.contains("item1"));
        assertTrue(json.contains("item2"));
    }

    @Test
    void testToJsonWithMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", 123);

        String json = GsonTool.toJson(map);
        assertNotNull(json);
        assertTrue(json.contains("key1"));
        assertTrue(json.contains("value1"));
    }

    @Test
    void testFromJsonWithSimpleObject() {
        String json = "{\"name\":\"John\",\"age\":25}";
        TestUser user = GsonTool.fromJson(json, TestUser.class);

        assertNotNull(user);
        assertEquals("John", user.getName());
        assertEquals(25, user.getAge());
    }

    @Test
    void testFromJsonWithNull() {
        TestUser user = GsonTool.fromJson(null, TestUser.class);
        assertNull(user);
    }

    @Test
    void testFromJsonWithString() {
        String json = "\"test\"";
        String result = GsonTool.fromJson(json, String.class);
        assertEquals("test", result);
    }

    @Test
    void testFromJsonWithNumber() {
        String json = "42";
        Integer result = GsonTool.fromJson(json, Integer.class);
        assertEquals(42, result.intValue());
    }

    @Test
    void testFromJsonWithList() {
        String json = "[\"item1\",\"item2\"]";
        ArrayList<String> list = GsonTool.fromJsonList(json, String.class);

        assertNotNull(list);
        assertEquals(2, list.size());
        assertEquals("item1", list.get(0));
        assertEquals("item2", list.get(1));
    }

    @Test
    void testFromJsonWithEmptyList() {
        String json = "[]";
        ArrayList<String> list = GsonTool.fromJsonList(json, String.class);

        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    void testFromJsonWithObjectList() {
        String json = "[{\"name\":\"John\",\"age\":25},{\"name\":\"Jane\",\"age\":30}]";
        ArrayList<TestUser> users = GsonTool.fromJsonList(json, TestUser.class);

        assertNotNull(users);
        assertEquals(2, users.size());
        assertEquals("John", users.get(0).getName());
        assertEquals("Jane", users.get(1).getName());
    }

    @Test
    void testFromReturnJson() {
        ReturnT<String> returnT = new ReturnT<>(ReturnT.SUCCESS_CODE, "success");
        returnT.setContent("result");

        String json = GsonTool.toJson(returnT);
        ReturnT<String> parsed = GsonTool.fromReturnJson(json, String.class);

        assertNotNull(parsed);
        assertEquals(ReturnT.SUCCESS_CODE, parsed.getCode());
        assertEquals("success", parsed.getMsg());
        assertEquals("result", parsed.getContent());
    }

    @Test
    void testFromReturnJsonWithNullContent() {
        ReturnT<String> returnT = new ReturnT<>(ReturnT.FAIL_CODE, "error");

        String json = GsonTool.toJson(returnT);
        ReturnT<String> parsed = GsonTool.fromReturnJson(json, String.class);

        assertNotNull(parsed);
        assertEquals(ReturnT.FAIL_CODE, parsed.getCode());
        assertEquals("error", parsed.getMsg());
    }

    @Test
    void testRoundTripSimpleObject() {
        TestUser original = new TestUser("Alice", 30);
        String json = GsonTool.toJson(original);
        TestUser parsed = GsonTool.fromJson(json, TestUser.class);

        assertEquals(original.getName(), parsed.getName());
        assertEquals(original.getAge(), parsed.getAge());
    }

    @Test
    void testRoundTripList() {
        List<String> original = new ArrayList<>();
        original.add("a");
        original.add("b");
        original.add("c");

        String json = GsonTool.toJson(original);
        ArrayList<String> parsed = GsonTool.fromJsonList(json, String.class);

        assertEquals(original.size(), parsed.size());
        for (int i = 0; i < original.size(); i++) {
            assertEquals(original.get(i), parsed.get(i));
        }
    }

    @Test
    void testDateFormat() {
        // Test that date format is yyyy-MM-dd HH:mm:ss
        TestUserWithDate user = new TestUserWithDate("Bob", DateUtil.parseDateTime("2023-11-15 10:30:45"));

        String json = GsonTool.toJson(user);
        assertNotNull(json);
        // Should contain formatted date
        assertTrue(json.contains("2023-11-15 10:30:45"));
    }

    @Test
    void testSpecialCharactersInString() {
        String text = "Hello <World> & \"Friends\"";
        String json = GsonTool.toJson(text);

        // Should not escape HTML by default (disableHtmlEscaping)
        assertNotNull(json);
    }

    @Test
    void testNestedObject() {
        NestedObject nested = new NestedObject();
        nested.setId(1);
        nested.setUser(new TestUser("Charlie", 35));

        String json = GsonTool.toJson(nested);
        assertNotNull(json);
        assertTrue(json.contains("Charlie"));

        NestedObject parsed = GsonTool.fromJson(json, NestedObject.class);
        assertNotNull(parsed);
        assertEquals(1, parsed.getId());
        assertEquals("Charlie", parsed.getUser().getName());
    }

    // -------------------------------------------------------------------------
    // Test helper classes
    // -------------------------------------------------------------------------

    static class TestUser {
        private String name;
        private int age;

        public TestUser() {}

        public TestUser(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }

    static class TestUserWithDate {
        private String name;
        private java.util.Date birthDate;

        public TestUserWithDate() {}

        public TestUserWithDate(String name, java.util.Date birthDate) {
            this.name = name;
            this.birthDate = birthDate;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public java.util.Date getBirthDate() {
            return birthDate;
        }

        public void setBirthDate(java.util.Date birthDate) {
            this.birthDate = birthDate;
        }
    }

    static class NestedObject {
        private int id;
        private TestUser user;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public TestUser getUser() {
            return user;
        }

        public void setUser(TestUser user) {
            this.user = user;
        }
    }
}
