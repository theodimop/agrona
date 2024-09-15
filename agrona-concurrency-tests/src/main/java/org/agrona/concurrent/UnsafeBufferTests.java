/*
 * Copyright 2014-2024 Real Logic Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.agrona.concurrent;

import org.agrona.BufferUtil;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.III_Result;
import org.openjdk.jcstress.infra.results.I_Result;
import org.openjdk.jcstress.infra.results.JJJ_Result;
import org.openjdk.jcstress.infra.results.J_Result;

import static java.nio.ByteBuffer.allocateDirect;

/**
 * Set of concurrency tests for {@link UnsafeBuffer}.
 */
public class UnsafeBufferTests
{
    UnsafeBufferTests()
    {
    }

    /**
     * Test that verifies the atomicity of the {@link UnsafeBuffer#putLongVolatile(int, long)},
     * {@link UnsafeBuffer#putLongOrdered(int, long)} and {@link UnsafeBuffer#getLongVolatile(int)}.
     */
    @JCStressTest
    @Outcome(id = "0", expect = Expect.ACCEPTABLE, desc = "read before writes")
    @Outcome(id = "-1", expect = Expect.ACCEPTABLE, desc = "putLongVolatile before read")
    @Outcome(id = "9223372036854775806", expect = Expect.ACCEPTABLE, desc = "putLongOrdered before read")
    @State
    public static class DirectBufferLong
    {
        private static final int WRITE_INDEX = 0;
        private final UnsafeBuffer buffer = new UnsafeBuffer(BufferUtil.allocateDirectAligned(8, 8));

        DirectBufferLong()
        {
        }

        /**
         * Writer thread.
         */
        @Actor
        public void putLongVolatile()
        {
            buffer.putLongVolatile(WRITE_INDEX, -1);
        }

        /**
         * Writer thread.
         */
        @Actor
        public void putLongOrdered()
        {
            buffer.putLongOrdered(WRITE_INDEX, Long.MAX_VALUE - 1);
        }

        /**
         * Reader thread.
         *
         * @param result object.
         */
        @Actor
        public void actor2(final J_Result result)
        {
            result.r1 = buffer.getLongVolatile(WRITE_INDEX);
        }
    }

    /**
     * Test that verifies the atomicity of the {@link UnsafeBuffer#putIntVolatile(int, int)},
     * {@link UnsafeBuffer#putIntOrdered(int, int)} and {@link UnsafeBuffer#getIntVolatile(int)}.
     */
    @JCStressTest
    @Outcome(id = "0", expect = Expect.ACCEPTABLE, desc = "read before writes")
    @Outcome(id = "-1", expect = Expect.ACCEPTABLE, desc = "putIntVolatile before read")
    @Outcome(id = "222222222", expect = Expect.ACCEPTABLE, desc = "putIntOrdered before read")
    @State
    public static class DirectBufferInt
    {
        private static final int WRITE_INDEX = 4;
        private final UnsafeBuffer buffer = new UnsafeBuffer(BufferUtil.allocateDirectAligned(8, 8));

        DirectBufferInt()
        {
        }

        /**
         * Writer thread.
         */
        @Actor
        public void putIntVolatile()
        {
            buffer.putIntVolatile(WRITE_INDEX, -1);
        }

        /**
         * Writer thread.
         */
        @Actor
        public void putIntOrdered()
        {
            buffer.putIntOrdered(WRITE_INDEX, 222222222);
        }

        /**
         * Reader thread.
         *
         * @param result object.
         */
        @Actor
        public void actor2(final I_Result result)
        {
            result.r1 = buffer.getIntVolatile(WRITE_INDEX);
        }
    }

    /**
     * Test that verifies the atomicity of the {@link UnsafeBuffer#getAndAddLong(int, long)} method.
     */
    @JCStressTest
    @Outcome(id = "5, 6, 9", expect = Expect.ACCEPTABLE, desc = "actor1 before actor2")
    @Outcome(id = "8, 5, 9", expect = Expect.ACCEPTABLE, desc = "actor2 before actor1")
    @State
    public static class GetAndAddLong
    {
        private static final int WRITE_INDEX = 8;
        private final UnsafeBuffer buffer = new UnsafeBuffer(new byte[16]);

        GetAndAddLong()
        {
            buffer.putLong(0, -1);
            buffer.putLong(WRITE_INDEX, 5);
        }

        /**
         * First writer.
         *
         * @param result to capture return value.
         */
        @Actor
        public void actor1(final JJJ_Result result)
        {
            result.r1 = buffer.getAndAddLong(WRITE_INDEX, 1);
        }

        /**
         * Second writer.
         *
         * @param result to capture return value.
         */
        @Actor
        public void actor2(final JJJ_Result result)
        {
            result.r2 = buffer.getAndAddLong(WRITE_INDEX, 3);
        }

        /**
         * Arbiter to verify final value.
         *
         * @param result object.
         */
        @Arbiter
        public void arbiter(final JJJ_Result result)
        {
            result.r3 = buffer.getLong(WRITE_INDEX);
            if (buffer.getLong(0) != -1)
            {
                throw new IllegalStateException("buffer overwritten");
            }
        }
    }

    /**
     * Test that verifies the atomicity of the {@link UnsafeBuffer#getAndAddInt(int, int)} method.
     */
    @JCStressTest
    @Outcome(id = "4, 5, 13", expect = Expect.ACCEPTABLE, desc = "actor1 before actor2")
    @Outcome(id = "12, 4, 13", expect = Expect.ACCEPTABLE, desc = "actor2 before actor1")
    @State
    public static class GetAndAddInt
    {
        private static final int WRITE_INDEX = 12;
        private final UnsafeBuffer buffer = new UnsafeBuffer(allocateDirect(16));

        GetAndAddInt()
        {
            buffer.setMemory(0, buffer.capacity(), (byte)-1);
            buffer.putInt(WRITE_INDEX, 4);
        }

        /**
         * First writer.
         *
         * @param result to capture return value.
         */
        @Actor
        public void actor1(final III_Result result)
        {
            result.r1 = buffer.getAndAddInt(WRITE_INDEX, 1);
        }

        /**
         * Second writer.
         *
         * @param result to capture return value.
         */
        @Actor
        public void actor2(final III_Result result)
        {
            result.r2 = buffer.getAndAddInt(WRITE_INDEX, 8);
        }

        /**
         * Arbiter to verify final value.
         *
         * @param result object.
         */
        @Arbiter
        public void arbiter(final III_Result result)
        {
            result.r3 = buffer.getInt(WRITE_INDEX);
            if (buffer.getByte(0) != -1)
            {
                throw new IllegalStateException("buffer overwritten");
            }
        }
    }

    /**
     * Test that verifies the atomicity of the {@link UnsafeBuffer#getAndSetLong(int, long)} method.
     */
    @JCStressTest
    @Outcome(id = "42, 19, 7", expect = Expect.ACCEPTABLE, desc = "actor1 before actor2")
    @Outcome(id = "7, 42, 19", expect = Expect.ACCEPTABLE, desc = "actor2 before actor1")
    @State
    public static class GetAndSetLong
    {
        private static final int WRITE_INDEX = 16;
        private final UnsafeBuffer buffer = new UnsafeBuffer(allocateDirect(32));

        GetAndSetLong()
        {
            buffer.setMemory(0, buffer.capacity(), (byte)111);
            buffer.putLong(WRITE_INDEX, 42);
        }

        /**
         * First writer.
         *
         * @param result to capture return value.
         */
        @Actor
        public void actor1(final JJJ_Result result)
        {
            result.r1 = buffer.getAndSetLong(WRITE_INDEX, 19);
        }

        /**
         * Second writer.
         *
         * @param result to capture return value.
         */
        @Actor
        public void actor2(final JJJ_Result result)
        {
            result.r2 = buffer.getAndSetLong(WRITE_INDEX, 7);
        }

        /**
         * Arbiter to verify final value.
         *
         * @param result object.
         */
        @Arbiter
        public void arbiter(final JJJ_Result result)
        {
            result.r3 = buffer.getLong(WRITE_INDEX);
            if (buffer.getByte(buffer.capacity() - 1) != 111)
            {
                throw new IllegalStateException("buffer overwritten");
            }
        }
    }

    /**
     * Test that verifies the atomicity of the {@link UnsafeBuffer#getAndSetInt(int, int)} method.
     */
    @JCStressTest
    @Outcome(id = "-1, 8, 11", expect = Expect.ACCEPTABLE, desc = "actor1 before actor2")
    @Outcome(id = "11, -1, 8", expect = Expect.ACCEPTABLE, desc = "actor2 before actor1")
    @State
    public static class GetAndSetInt
    {
        private static final int WRITE_INDEX = 4;
        private final UnsafeBuffer buffer = new UnsafeBuffer(new byte[16]);

        GetAndSetInt()
        {
            buffer.setMemory(0, buffer.capacity(), (byte)5);
            buffer.putInt(WRITE_INDEX, -1);
        }

        /**
         * First writer.
         *
         * @param result to capture return value.
         */
        @Actor
        public void actor1(final III_Result result)
        {
            result.r1 = buffer.getAndSetInt(WRITE_INDEX, 8);
        }

        /**
         * Second writer.
         *
         * @param result to capture return value.
         */
        @Actor
        public void actor2(final III_Result result)
        {
            result.r2 = buffer.getAndSetInt(WRITE_INDEX, 11);
        }

        /**
         * Arbiter to verify final value.
         *
         * @param result object.
         */
        @Arbiter
        public void arbiter(final III_Result result)
        {
            result.r3 = buffer.getInt(WRITE_INDEX);
            if (buffer.getByte(buffer.capacity() - 1) != 5)
            {
                throw new IllegalStateException("buffer overwritten");
            }
        }
    }
}
