/*
 * Copyright 2018 astonbitecode
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.astonbitecode.j4rs.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.astonbitecode.j4rs.api.services.json.Codec;
import org.astonbitecode.j4rs.api.services.json.exceptions.JsonCodecException;
import org.astonbitecode.j4rs.utils.Utils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

public class JacksonCodec implements Codec {
    private static final String RUST_FIELD = "Rust";
    private static final String JSON_FIELD = "json";
    private static final String CLASS_NAME_FIELD = "class_name";
    private ObjectMapper mapper = new ObjectMapper();
    TypeReference<Map<String, Object>[]> typeRef
            = new TypeReference<Map<String, Object>[]>() {
    };

    @Override
    @SuppressWarnings("unchecked")
    public <T> T decode(String json, String className) throws JsonCodecException {
        try {
            Class<T> clazz = null;
            clazz = (Class<T>) Utils.forNameEnhanced(className);
            T obj = mapper.readValue(json, clazz);
            return obj;
        } catch (ClassNotFoundException | JsonProcessingException error) {
            throw new JsonCodecException(error);
        }
    }

    @Override
    public String encode(Object obj) throws JsonCodecException {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException error) {
            throw new JsonCodecException(error);
        }
    }

    @Override
    public Object[] decodeArrayContents(String json) throws JsonCodecException {
        try {
            Map<String, Object>[] array = mapper.readValue(json, typeRef);

            return Arrays.stream(array)
                    .map(elem -> {
                        try {
                            return retrieveFromMap(elem);
                        } catch (Exception error) {
                            throw new JsonCodecException("Error while retrieving Array", error);
                        }
                    }).toArray();
        } catch (JsonProcessingException error) {
            throw new JsonCodecException(error);
        }
    }

    /**
     * [
     * {"Rust":{"json":"\"arg1\"","class_name":"java.lang.String","arg_from":"rust"}},
     * {"Rust":{"json":"\"arg2\"","class_name":"java.lang.String","arg_from":"rust"}},
     * {"Rust":{"json":"\"arg3\"","class_name":"java.lang.String","arg_from":"rust"}}
     * ]
     */
    private <U> U retrieveFromMap(Map<String, Object> map) throws ClassNotFoundException, IOException {
        Map<String, String> innerMap = (Map<String, String>) map.get(RUST_FIELD);
        if (innerMap == null) {
            throw new JsonCodecException("Cannot create InvocationArg object form Map '" + map + "'");
        }
        String retrievedClassName = innerMap.get(CLASS_NAME_FIELD);
        String retrievedJson = innerMap.get(JSON_FIELD);
        if (retrievedClassName == null || retrievedJson == null) {
            throw new JsonCodecException("Cannot create InvocationArg object form the JSON '" + retrievedJson + "'");
        }
        return decode(retrievedJson, retrievedClassName);
    }
}
