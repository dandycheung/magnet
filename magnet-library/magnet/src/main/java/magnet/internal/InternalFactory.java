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

import magnet.Scope;
import org.jetbrains.annotations.NotNull;

/* Subject to change. For internal use only. */
public final class InternalFactory {

    private static final InstanceManager INSTANCE_MANAGER = new MagnetInstanceManager();

    private InternalFactory() {}

    @NotNull
    public static Scope createRootScope() {
        return new MagnetScope(
            /* parent = */ null,
            /* instanceManager = */ INSTANCE_MANAGER
        );
    }

}
