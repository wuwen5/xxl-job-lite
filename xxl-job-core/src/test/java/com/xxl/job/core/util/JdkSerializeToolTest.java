package com.xxl.job.core.util;

import com.xxl.job.core.biz.model.HandleCallbackParam;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author wuwen
 */
class JdkSerializeToolTest {

    @Test
    void serialize() {
        List<HandleCallbackParam> list = new ArrayList<>();
        list.add(new HandleCallbackParam());
        byte[] serialize = JdkSerializeTool.serialize(list);

        Object deserialize = JdkSerializeTool.deserialize(serialize, List.class);
        
        assertEquals(list.getClass(), deserialize.getClass());
    }
}