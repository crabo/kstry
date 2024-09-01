/*
 *
 *  * Copyright (c) 2020-2024, Lykan (jiashuomeng@gmail.com).
 *  * <p>
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  * <p>
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  * <p>
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */
package cn.kstry.framework.core.exception;

import java.util.List;

/**
 * @author lykan
 */
public class ViolationException extends KstryException {

    private Object invalidValue;

    private String fieldName;

    private List<String> fieldPath;

    /**
     * 受限的Bean
     */
    private Object leafBean;

    public ViolationException(String code, String desc, Throwable cause) {
        super(code, desc, cause);
    }

    public Object getInvalidValue() {
        return invalidValue;
    }

    public void setInvalidValue(Object invalidValue) {
        this.invalidValue = invalidValue;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public List<String> getFieldPath() {
        return fieldPath;
    }

    public void setFieldPath(List<String> fieldPath) {
        this.fieldPath = fieldPath;
    }

    public Object getLeafBean() {
        return leafBean;
    }

    public void setLeafBean(Object leafBean) {
        this.leafBean = leafBean;
    }
}
