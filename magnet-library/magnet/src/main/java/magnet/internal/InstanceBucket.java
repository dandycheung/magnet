/*
 * Copyright (C) 2018 Sergej Shafarenka, www.halfbit.de
 *
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

package magnet.internal;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/* Subject to change. For internal use only. */
@SuppressWarnings("unchecked")
final class InstanceBucket<T> {

    @NotNull private final OnInstanceListener listener;
    @NotNull private Instance instance;
    private final int scopeDepth;

    InstanceBucket(
        int scopeDepth,
        @Nullable InstanceFactory<T> factory,
        @NotNull Class<T> objectType,
        @NotNull T object,
        @NotNull String classifier,
        @NotNull OnInstanceListener listener
    ) {
        this.scopeDepth = scopeDepth;
        this.listener = listener;
        this.instance = createSingleInstance(factory, objectType, object, classifier);
    }

    int getScopeDepth() { return scopeDepth; }

    @NotNull
    T getSingleInstance() {
        if (instance instanceof InjectedInstance) {
            return ((InjectedInstance<T>) instance).object;
        } else if (instance instanceof BoundInstance) {
            return ((BoundInstance<T>) instance).object;
        }

        ManyValueInstance manyValueInstance = (ManyValueInstance) instance;
        throw new IllegalStateException(
            String.format(
                "Single instance requested, while many instances are stored: %s",
                manyValueInstance.instances
            )
        );
    }

    @Nullable
    T getOptional(@Nullable Class<InstanceFactory<T>> factoryType) {
        if (instance instanceof InjectedInstance) {
            InjectedInstance<T> single = (InjectedInstance<T>) instance;
            if (single.factory.getClass() == factoryType) {
                return single.object;
            }
            return null;
        } else if (instance instanceof BoundInstance) {
            BoundInstance<T> single = (BoundInstance<T>) instance;
            if (factoryType == null) {
                return single.object;
            }
            return null;
        }
        return (T) ((ManyValueInstance) instance).getOptional(factoryType);
    }

    @NotNull
    List<T> getMany() {
        if (instance instanceof InjectedInstance) {
            return Collections.singletonList(((InjectedInstance<T>) instance).object);
        } else if (instance instanceof BoundInstance) {
            return Collections.singletonList(((BoundInstance<T>) instance).object);
        }
        return ((ManyValueInstance<T>) instance).getMany();
    }

    void registerObject(
        @Nullable InstanceFactory<T> factory,
        @NotNull Class<T> objectType,
        @NotNull T object,
        @NotNull String classifier
    ) {
        if (this.instance instanceof ManyValueInstance) {
            ManyValueInstance<T> many = (ManyValueInstance<T>) this.instance;
            many.putSingle(createSingleInstance(factory, objectType, object, classifier));

        } else {
            ManyValueInstance<T> many = new ManyValueInstance<>((SingleInstance<T>) this.instance);
            many.putSingle(createSingleInstance(factory, objectType, object, classifier));
            this.instance = many;
        }
    }

    @NotNull private SingleInstance<T> createSingleInstance(
        @Nullable InstanceFactory<T> factory,
        @NotNull Class<T> objectType,
        @NotNull T object,
        @NotNull String classifier
    ) {
        SingleInstance single;
        if (factory == null) {
            single = new BoundInstance<>(objectType, object, classifier);
        } else {
            single = new InjectedInstance<>(factory, objectType, object, classifier);
        }
        listener.onInstanceCreated(single);
        return single;
    }

    interface Instance {}

    static abstract class SingleInstance<T> implements Instance {
        @NotNull final Class<T> objectType;
        @NotNull final T object;
        @NotNull final String classifier;

        SingleInstance(
            @NotNull Class<T> objectType,
            @NotNull T object,
            @NotNull String classifier
        ) {
            this.objectType = objectType;
            this.object = object;
            this.classifier = classifier;
        }
    }

    static class BoundInstance<T> extends SingleInstance<T> {
        BoundInstance(
            @NotNull Class<T> objectType,
            @NotNull T object,
            @NotNull String classifier
        ) {
            super(objectType, object, classifier);
        }
    }

    static class InjectedInstance<T> extends SingleInstance<T> {
        @NotNull final InstanceFactory<T> factory;

        InjectedInstance(
            @NotNull InstanceFactory<T> factory,
            @NotNull Class<T> objectType,
            @NotNull T object,
            @NotNull String classifier
        ) {
            super(objectType, object, classifier);
            this.factory = factory;
        }
    }

    private static class ManyValueInstance<T> implements Instance {
        @NotNull
        private final HashMap<Class<InstanceFactory<T>>, SingleInstance<T>> instances;

        ManyValueInstance(@NotNull SingleInstance<T> single) {
            instances = new HashMap<>(8);
            putSingle(single);
        }

        @NotNull
        List<T> getMany() {
            List<T> result = new ArrayList<>(this.instances.size());
            for (SingleInstance<T> single : this.instances.values()) {
                result.add(single.object);
            }
            return result;
        }

        @Nullable
        T getOptional(@Nullable Class<InstanceFactory<T>> factoryType) {
            SingleInstance<T> single = instances.get(factoryType);
            if (single == null) return null;
            return single.object;
        }

        void putSingle(@NotNull SingleInstance<T> single) {
            @Nullable final Class<InstanceFactory<T>> objectType;
            if (single instanceof InjectedInstance) {
                objectType = (Class<InstanceFactory<T>>) ((InjectedInstance) single).factory.getClass();
            } else if (single instanceof BoundInstance) {
                objectType = null;
            } else {
                throw new IllegalStateException("Unsupported SingleInstance type.");
            }
            instances.put(objectType, single);
        }
    }

    interface OnInstanceListener {
        <T> void onInstanceCreated(SingleInstance<T> instance);
    }

}