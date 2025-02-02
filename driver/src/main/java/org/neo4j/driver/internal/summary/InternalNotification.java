/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.driver.internal.summary;

import static org.neo4j.driver.internal.value.NullValue.NULL;

import java.util.function.Function;
import org.neo4j.driver.Value;
import org.neo4j.driver.summary.InputPosition;
import org.neo4j.driver.summary.Notification;

public class InternalNotification implements Notification {
    public static final Function<Value, Notification> VALUE_TO_NOTIFICATION = new Function<Value, Notification>() {
        @Override
        public Notification apply(Value value) {
            String code = value.get("code").asString();
            String title = value.get("title").asString();
            String description = value.get("description").asString();
            String severity =
                    value.containsKey("severity") ? value.get("severity").asString() : "N/A";

            Value posValue = value.get("position");
            InputPosition position = null;
            if (posValue != NULL) {
                position = new InternalInputPosition(
                        posValue.get("offset").asInt(),
                        posValue.get("line").asInt(),
                        posValue.get("column").asInt());
            }

            return new InternalNotification(code, title, description, severity, position);
        }
    };

    private final String code;
    private final String title;
    private final String description;
    private final String severity;
    private final InputPosition position;

    public InternalNotification(
            String code, String title, String description, String severity, InputPosition position) {
        this.code = code;
        this.title = title;
        this.description = description;
        this.severity = severity;
        this.position = position;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String title() {
        return title;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public InputPosition position() {
        return position;
    }

    @Override
    public String severity() {
        return severity;
    }

    @Override
    public String toString() {
        String info = "code=" + code + ", title=" + title + ", description=" + description + ", severity=" + severity;
        return position == null ? info : info + ", position={" + position + "}";
    }
}
