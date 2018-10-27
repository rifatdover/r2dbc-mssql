/*
 * Copyright 2018 the original author or authors.
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

package io.r2dbc.mssql.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.r2dbc.mssql.message.type.TypeInformation;
import io.r2dbc.mssql.message.type.TypeInformation.SqlServerType;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Codec for date types that are represented as {@link LocalDate}.
 *
 * <ul>
 * <li>Server types: {@link SqlServerType#DATE}</li>
 * <li>Java type: {@link LocalDate}</li>
 * <li>Downcast: none</li>
 * </ul>
 *
 * @author Mark Paluch
 */
final class LocalDateCodec extends AbstractCodec<LocalDate> {

    /**
     * Singleton instance.
     */
    public static final LocalDateCodec INSTANCE = new LocalDateCodec();

    /**
     * Date base date: 0001-01-01.
     */
    private static final LocalDate DATE_ZERO = LocalDate.of(1, 1, 1);

    private LocalDateCodec() {
        super(LocalDate.class);
    }

    @Override
    boolean doCanDecode(TypeInformation typeInformation) {
        return typeInformation.getServerType() == SqlServerType.DATE;
    }

    @Override
    LocalDate doDecode(ByteBuf buffer, LengthDescriptor length, TypeInformation type, Class<? extends LocalDate> valueType) {

        int days = (buffer.readByte() & 0xFF) | (buffer.readByte() & 0xFF) << 8 | (buffer.readByte() & 0xFF) << 16;

        return DATE_ZERO.plusDays(days);
    }

    @Override
    Encoded doEncode(ByteBufAllocator allocator, TypeInformation type, LocalDate value) {

        ByteBuf buffer = allocator.buffer(3);

        doEncode(buffer, value);

        return Encoded.of(buffer);
    }

    /**
     * Write the {@link LocalDate} value to the {@link ByteBuf data buffer}.
     *
     * @param buffer
     * @param value
     */
    void doEncode(ByteBuf buffer, LocalDate value) {

        long days = ChronoUnit.DAYS.between(DATE_ZERO, value);

        buffer.writeByte((byte) days & 0xFF);
        buffer.writeByte((byte) (days >> 8) & 0xFF);
        buffer.writeByte((byte) (days >> 16) & 0xFF);
    }
}
