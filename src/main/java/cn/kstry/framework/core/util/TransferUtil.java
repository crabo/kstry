/*
 *
 *  * Copyright (c) 2020-2021, Lykan (jiashuomeng@gmail.com).
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
package cn.kstry.framework.core.util;

import cn.kstry.framework.core.resource.identity.Identity;
import cn.kstry.framework.core.role.Permission;
import cn.kstry.framework.core.role.SimplePermission;

/**
 * 转换Util
 *
 * @author lykan
 */
public class TransferUtil {

    /**
     * 资源身份转化为权限
     *
     * @param identity 资源身份
     * @return 权限
     */
    public static Permission transferPermission(Identity identity) {
        AssertUtil.notNull(identity);
        return new SimplePermission(identity.getIdentityId(), identity.getIdentityType());
    }
}
