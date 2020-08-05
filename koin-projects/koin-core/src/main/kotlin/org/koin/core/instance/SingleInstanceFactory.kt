/*
 * Copyright 2017-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.koin.core.instance

import org.koin.core.Koin
import org.koin.core.definition.BeanDefinition

/**
 * Single instance holder
 * @author Arnaud Giuliani
 */
class SingleInstanceFactory<T>(koin: Koin, beanDefinition: BeanDefinition<T>) :
    InstanceFactory<T>(koin, beanDefinition) {

    @Volatile
    private var value: T? = null

    override fun isCreated(): Boolean = (value != null)

    @Synchronized
    override fun drop() {
        beanDefinition.callbacks.onClose?.invoke(value)
        value = null
    }

    @Synchronized
    override fun create(context: InstanceContext): T {
        return value ?: super.create(context).apply { value = this }
    }

    override fun get(context: InstanceContext): T {
        return value ?: create(context)
    }
}