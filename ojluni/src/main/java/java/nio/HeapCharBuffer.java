/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

// -- This file was mechanically generated: Do not edit! -- //
// Android-note: This file is generated by ojluni/src/tools/gensrc_android.sh.

package java.nio;

import java.util.Objects;
import libcore.io.Memory;

/**

 * A read/write HeapCharBuffer.






 */
// Android-changed: Make it final as no subclasses exist.
final class HeapCharBuffer
    extends CharBuffer
{
    // Android-removed: Removed unused constants.
    /*
    // Cached array base offset
    private static final long ARRAY_BASE_OFFSET = UNSAFE.arrayBaseOffset(char[].class);

    // Cached array index scale
    private static final long ARRAY_INDEX_SCALE = UNSAFE.arrayIndexScale(char[].class);
    */

    // For speed these fields are actually declared in X-Buffer;
    // these declarations are here as documentation
    /*

    protected final char[] hb;
    protected final int offset;

    */
    // Android-removed: Removed MemorySegmentProxy to be supported yet.
    HeapCharBuffer(int cap, int lim) {            // package-private

        // Android-changed: Merge the Read-only buffer class with this Read-Write buffer class.
        // super(-1, 0, lim, cap, new char[cap], 0);
        this(cap, lim, false);
        /*
        hb = new char[cap];
        offset = 0;
        */
        // Android-removed: buffer.address is only used by Direct*Buffer.
        // this.address = ARRAY_BASE_OFFSET;




    }


   // Android-added: Merge the Read-only buffer class with this Read-Write buffer class.
    private HeapCharBuffer(int cap, int lim, boolean isReadOnly) {
        super(-1, 0, lim, cap, new char[cap], 0);
        this.isReadOnly = isReadOnly;
    }


    // Android-removed: Removed MemorySegmentProxy to be supported yet.
    HeapCharBuffer(char[] buf, int off, int len) { // package-private

        // Android-changed: Merge the Read-only buffer class with this Read-Write buffer class.
        // super(-1, off, off + len, buf.length, buf, 0);
        this(buf, off, len, false);
        /*
        hb = buf;
        offset = 0;
        */
        // Android-removed: buffer.address is only used by Direct*Buffer.
        // this.address = ARRAY_BASE_OFFSET;




    }


   // Android-added: Merge the Read-only buffer class with this Read-Write buffer class.
    private HeapCharBuffer(char[] buf, int off, int len, boolean isReadOnly) {
        super(-1, off, off + len, buf.length, buf, 0);
        this.isReadOnly = isReadOnly;
    }


    // Android-changed: Merge the Read-only buffer class with this Read-Write buffer class.
    // Android-changed: Make the method private.
    // Android-removed: Removed MemorySegmentProxy to be supported yet.
    private HeapCharBuffer(char[] buf,
                                   int mark, int pos, int lim, int cap,
                                   int off, boolean isReadOnly)
    {

        super(mark, pos, lim, cap, buf, off);
        // Android-changed: Merge the Read-only buffer class with this Read-Write buffer class.
        this.isReadOnly = isReadOnly;
        /*
        hb = buf;
        offset = off;
        */
        // Android-removed: buffer.address is only used by Direct*Buffer.
        // this.address = ARRAY_BASE_OFFSET + off * ARRAY_INDEX_SCALE;




    }

    public CharBuffer slice() {
        int pos = this.position();
        int lim = this.limit();
        int rem = (pos <= lim ? lim - pos : 0);
        return new HeapCharBuffer(hb,
                -1,
                0,
                rem,
                rem,
        // Android-removed: Removed MemorySegmentProxy not supported yet.
                pos + offset,
        // Android-changed: Merge the Read-only buffer class with this Read-Write buffer class.
                isReadOnly);
    }

    @Override
    public CharBuffer slice(int index, int length) {
        Objects.checkFromIndexSize(index, length, limit());
        return new HeapCharBuffer(hb,
                -1,
                0,
                length,
                length,
        // Android-removed: Removed MemorySegmentProxy not supported yet.
                index + offset,
        // Android-changed: Merge the Read-only buffer class with this Read-Write buffer class.
                isReadOnly);
    }

    public CharBuffer duplicate() {
        return new HeapCharBuffer(hb,
                this.markValue(),
                this.position(),
                this.limit(),
                this.capacity(),
        // Android-removed: Removed MemorySegmentProxy not supported yet.
                offset,
        // Android-changed: Merge the Read-only buffer class with this Read-Write buffer class.
                isReadOnly);
    }

    public CharBuffer asReadOnlyBuffer() {

        // Android-removed: Removed MemorySegmentProxy not supported yet.
        // Android-changed: Merge the Read-only buffer class with this Read-Write buffer class.
        /*
        return new HeapCharBufferR(hb,
                                     this.markValue(),
                                     this.position(),
                                     this.limit(),
                                     this.capacity(),
                                     offset, segment);
        */
        return new HeapCharBuffer(hb,
                this.markValue(),
                this.position(),
                this.limit(),
                this.capacity(),
                offset,
                true /* isReadOnly */);



    }



    // Android-changed:  Make it private as no subclasses exist.
    private int ix(int i) {
        return i + offset;
    }







    @Override
    public char get() {
        return hb[ix(nextGetIndex())];
    }

    @Override
    public char get(int i) {
        return hb[ix(checkIndex(i))];
    }


    @Override
    char getUnchecked(int i) {
    return hb[ix(i)];
    }


    @Override
    public CharBuffer get(char[] dst, int offset, int length) {
        checkScope();
        Objects.checkFromIndexSize(offset, length, dst.length);
        int pos = position();
        if (length > limit() - pos)
            throw new BufferUnderflowException();
        System.arraycopy(hb, ix(pos), dst, offset, length);
        position(pos + length);
        return this;
    }

    @Override
    public CharBuffer get(int index, char[] dst, int offset, int length) {
        checkScope();
        Objects.checkFromIndexSize(index, length, limit());
        Objects.checkFromIndexSize(offset, length, dst.length);
        System.arraycopy(hb, ix(index), dst, offset, length);
        return this;
    }

    public boolean isDirect() {
        return false;
    }



    @Override
    public boolean isReadOnly() {
        // Android-changed: Merge the Read-only buffer class with this Read-Write buffer class.
        return isReadOnly;
    }

    @Override
    public CharBuffer put(char x) {

        // Android-added: Merge the Read-only buffer class with this Read-Write buffer class.
        throwIfReadOnly();
        hb[ix(nextPutIndex())] = x;
        return this;



    }

    @Override
    public CharBuffer put(int i, char x) {

        // Android-added: Merge the Read-only buffer class with this Read-Write buffer class.
        throwIfReadOnly();
        hb[ix(checkIndex(i))] = x;
        return this;



    }

    @Override
    public CharBuffer put(char[] src, int offset, int length) {

        // Android-added: Merge the Read-only buffer class with this Read-Write buffer class.
        throwIfReadOnly();
        checkScope();
        Objects.checkFromIndexSize(offset, length, src.length);
        int pos = position();
        if (length > limit() - pos)
            throw new BufferOverflowException();
        System.arraycopy(src, offset, hb, ix(pos), length);
        position(pos + length);
        return this;



    }

    @Override
    public CharBuffer put(CharBuffer src) {

        checkScope();




        // Android-changed: Speed-up this operation if the src is a heap or direct buffer.
        // super.put(src);
        if (src == this) {
            throw createSameBufferException();
        }
        // Android-added: Merge the Read-only buffer class with this Read-Write buffer class.
        throwIfReadOnly();
        if (src instanceof HeapCharBuffer sb) {
            int n = sb.remaining();
            if (n > remaining())
                throw new BufferOverflowException();
            System.arraycopy(sb.hb, sb.ix(sb.position()),
                    hb, ix(position()), n);
            sb.position(sb.position() + n);
            position(position() + n);
        } else if (src.isDirect()) {
            int n = src.remaining();
            if (n > remaining())
                throw new BufferOverflowException();
            src.get(hb, ix(position()), n);
            position(position() + n);
        } else {
            super.put(src);
        }

        return this;



    }

    @Override
    public CharBuffer put(int index, CharBuffer src, int offset, int length) {

        checkScope();
        super.put(index, src, offset, length);
        return this;



    }

    @Override
    public CharBuffer put(int index, char[] src, int offset, int length) {

        checkScope();
        Objects.checkFromIndexSize(index, length, limit());
        Objects.checkFromIndexSize(offset, length, src.length);
        // Android-added: Merge the Read-only buffer class with this Read-Write buffer class.
        throwIfReadOnly();
        System.arraycopy(src, offset, hb, ix(index), length);
        return this;



    }



    @Override
    public CharBuffer put(String src, int start, int end) {

        checkScope();
        int length = end - start;
        Objects.checkFromIndexSize(start, length, src.length());
        // Android-added: Merge the Read-only buffer class with this Read-Write buffer class.
        throwIfReadOnly();
        int pos = position();
        int lim = limit();
        int rem = (pos <= lim) ? lim - pos : 0;
        if (length > rem)
            throw new BufferOverflowException();
        src.getChars(start, end, hb, ix(pos));
        position(pos + length);
        return this;



    }



    @Override
    public CharBuffer compact() {

        // Android-added: Merge the Read-only buffer class with this Read-Write buffer class.
        throwIfReadOnly();
        int pos = position();
        int lim = limit();
        assert (pos <= lim);
        int rem = (pos <= lim ? lim - pos : 0);
        System.arraycopy(hb, ix(pos), hb, ix(0), rem);
        position(rem);
        limit(capacity());
        discardMark();
        return this;



    }






















































































































































































































































































































































































































































































































































































































































































































































































    String toString(int start, int end) {               // package-private
        try {
            return new String(hb, start + offset, end - start);
        } catch (StringIndexOutOfBoundsException x) {
            throw new IndexOutOfBoundsException();
        }
    }


    // --- Methods to support CharSequence ---

    public CharBuffer subSequence(int start, int end) {
        int pos = position();
        Objects.checkFromToIndex(start, end, limit() - pos);
        return new HeapCharBuffer(hb,
                -1,
                pos + start,
                pos + end,
                capacity(),
        // Android-removed: Removed MemorySegmentProxy not supported yet.
                offset,
        // Android-changed: Merge the Read-only buffer class with this Read-Write buffer class.
                isReadOnly);
    }






    public ByteOrder order() {
        return ByteOrder.nativeOrder();
    }



    ByteOrder charRegionOrder() {
        return order();
    }


    // Android-added: Merge the Read-only buffer class with this Read-Write buffer class.
    private void throwIfReadOnly() {
        if (isReadOnly) {
            throw new ReadOnlyBufferException();
        }
    }
}
