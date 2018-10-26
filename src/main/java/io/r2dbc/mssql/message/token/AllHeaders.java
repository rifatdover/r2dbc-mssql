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

package io.r2dbc.mssql.message.token;

import io.netty.buffer.ByteBuf;
import io.r2dbc.mssql.message.TransactionDescriptor;
import io.r2dbc.mssql.message.tds.Encode;
import io.r2dbc.mssql.util.Assert;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * All Headers data structure.
 *
 * @author Mark Paluch
 */
public final class AllHeaders {

    private final List<NestedHeader> headers;

    private final int length;

    AllHeaders(List<NestedHeader> headers) {

        Objects.requireNonNull(headers, "Headers must not be null");

        this.headers = headers;

        int totalLength = 4; // DWORD

        for (NestedHeader header : headers) {
            totalLength += header.getTotalLength();
        }

        this.length = totalLength;
    }

    /**
     * Creates {@link AllHeaders} containing only a {@link TransactionDescriptor transactional} descriptor.
     *
     * @param transactionDescriptor the binary transaction descriptor.
     * @param outstandingRequests   number of outstanding requests
     * @return
     */
    public static AllHeaders transactional(byte[] transactionDescriptor, int outstandingRequests) {

        AllHeaders.TransactionDescriptorHeader txDescriptor = new AllHeaders.TransactionDescriptorHeader(transactionDescriptor, outstandingRequests
        );

        return new AllHeaders(Collections.singletonList(txDescriptor));
    }

    /**
     * Encode the header.
     *
     * @param buffer the data buffer.
     */
    public void encode(ByteBuf buffer) {

        Encode.dword(buffer, this.length);

        for (NestedHeader header : this.headers) {
            header.encode(buffer);
        }
    }

    public int getLength() {
        return this.length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AllHeaders)) {
            return false;
        }
        AllHeaders that = (AllHeaders) o;
        return length == that.length &&
            Objects.equals(headers, that.headers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(headers, length);
    }

    /**
     * Base class for nested headers.
     */
    abstract static class NestedHeader {

        private final int dataLength;

        private final int type;

        NestedHeader(int type, int dataLength) {
            this.dataLength = dataLength;
            this.type = type;
        }

        /**
         * @return the length of the header data.
         */
        int getDataLength() {
            return this.dataLength;
        }

        /**
         * @return the total length of the header including itself, data length and the data.
         */
        int getTotalLength() {
            return getDataLength() + 4 /* Length field DWORD */ + 2 /* type */;
        }

        /**
         * Encode this header.
         *
         * @param buffer the data buffer.
         */
        public void encode(ByteBuf buffer) {

            Objects.requireNonNull(buffer, "Buffer must not be null");

            Encode.dword(buffer, getTotalLength());
            Encode.uShort(buffer, this.type);
            encodeData(buffer);
        }

        /**
         * Encode this header's data.
         *
         * @param buffer the data buffer.
         */
        protected abstract void encodeData(ByteBuf buffer);
    }

    /**
     * Transaction descriptor (0x2).
     */
    static final class TransactionDescriptorHeader extends NestedHeader {

        private final int outstandingRequestCount;

        private final byte[] transactionDescriptor;

        public TransactionDescriptorHeader(byte[] transactionDescriptor, int outstandingRequestCount) {

            super(0x2, Objects.requireNonNull(transactionDescriptor, "Transaction Descriptor must not be null").length + /* outstanding request count */ 4);

            Assert.isTrue(transactionDescriptor.length == 8, "Transaction Descriptor must be exactly 8 bytes long");

            this.outstandingRequestCount = outstandingRequestCount;
            this.transactionDescriptor = transactionDescriptor;
        }

        @Override
        protected void encodeData(ByteBuf buffer) {

            buffer.writeBytes(this.transactionDescriptor);
            Encode.dword(buffer, this.outstandingRequestCount);
        }

        @Override
        int getDataLength() {
            return super.getDataLength();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof TransactionDescriptorHeader)) {
                return false;
            }
            TransactionDescriptorHeader that = (TransactionDescriptorHeader) o;
            return outstandingRequestCount == that.outstandingRequestCount &&
                Arrays.equals(transactionDescriptor, that.transactionDescriptor);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(outstandingRequestCount);
            result = 31 * result + Arrays.hashCode(transactionDescriptor);
            return result;
        }
    }
}
