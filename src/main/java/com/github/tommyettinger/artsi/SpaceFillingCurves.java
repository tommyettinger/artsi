package com.github.tommyettinger.artsi;

/**
 * Utility class for working with space filling curves. All implementations are for 16-bit integers
 * <p>
 * Much of the code is from <a href="https://github.com/rawrunprotected/hilbert_curves">rawrunprotected's repo</a>.
 * <p>
 * Decoding the curves can either be performed using the {@link #decodeHilbert} or {@link #decodePackedHilbert} methods. The advantage
 * of the former is that it avoids having to create an array, instead packing the value in an int. This can then be
 * extracted using the {@link #getX} and {@link #getY} methods.
 */
public final class SpaceFillingCurves {
    private SpaceFillingCurves() {

    }

    /**
     * @param decoded the decoded value
     * @return the left-hand part of an int
     */
    public static int getX(int decoded) {
        return decoded >>> 16;
    }

    /**
     * @param decoded the decoded value
     * @return the right-hand part of an int
     */
    public static int getY(int decoded) {
        return decoded & 0xFFFF;
    }

    /**
     * Decode the encoded value to a packed int (where the left-hand bits are the x
     * component and the right-hand bits are the y component
     *
     * @param encoded the encoded value
     * @return a packed int containing the decoded values
     */
    public static int decodePackedHilbert(int encoded) {

        int i0 = deinterleave(encoded);
        int i1 = deinterleave(encoded >> 1);

        int t0 = (i0 | i1) ^ 0xFFFF;
        int t1 = i0 & i1;

        int prefixT0 = prefixScan(t0);
        int prefixT1 = prefixScan(t1);

        int a = (((i0 ^ 0xFFFF) & prefixT1) | (i0 & prefixT0));

        int x = (a ^ i1);
        x <<= 16;
        x |= (a ^ i0 ^ i1);
        return x;
    }

    /**
     * Decode a Hilbert index to an x and y coordinate
     *
     * @param encoded the Hilbert index
     * @return a two-element int array containing [0] the x component and [1] the y component
     */
    public static int[] decodeHilbert(int encoded) {
        int i0 = deinterleave(encoded);
        int i1 = deinterleave(encoded >> 1);

        int t0 = (i0 | i1) ^ 0xFFFF;
        int t1 = i0 & i1;

        int prefixT0 = prefixScan(t0);
        int prefixT1 = prefixScan(t1);

        int a = (((i0 ^ 0xFFFF) & prefixT1) | (i0 & prefixT0));

        return new int[]{(a ^ i1), (a ^ i0 ^ i1)};
    }

    /**
     * Encode an xy coordinate to a hilbert code
     *
     * @param x the x component
     * @param y the y component
     * @return the encoded value
     */
    public static int encodeHilbert(int x, int y) {
        int a = x ^ y;
        int b = 0xFFFF ^ a;
        int c = 0xFFFF ^ (x | y);
        int d = x & (y ^ 0xFFFF);

        int A = a | (b >> 1);
        int B = (a >> 1) ^ a;
        int C = ((c >> 1) ^ (b & (d >> 1))) ^ c;
        int D = ((a & (c >> 1)) ^ (d >> 1)) ^ d;

        a = A;
        b = B;
        c = C;
        d = D;
        A = ((a & (a >> 2)) ^ (b & (b >> 2)));
        B = ((a & (b >> 2)) ^ (b & ((a ^ b) >> 2)));
        C ^= ((a & (c >> 2)) ^ (b & (d >> 2)));
        D ^= ((b & (c >> 2)) ^ ((a ^ b) & (d >> 2)));

        a = A;
        b = B;
        c = C;
        d = D;
        A = ((a & (a >> 4)) ^ (b & (b >> 4)));
        B = ((a & (b >> 4)) ^ (b & ((a ^ b) >> 4)));
        C ^= ((a & (c >> 4)) ^ (b & (d >> 4)));
        D ^= ((b & (c >> 4)) ^ ((a ^ b) & (d >> 4)));

        a = A;
        b = B;
        c = C;
        d = D;
        C ^= ((a & (c >> 8)) ^ (b & (d >> 8)));
        D ^= ((b & (c >> 8)) ^ ((a ^ b) & (d >> 8)));

        a = C ^ (C >> 1);
        b = D ^ (D >> 1);

        int i0 = x ^ y;
        int i1 = b | (0xFFFF ^ (i0 | a));

        return ((interleave(i1) << 1) | interleave(i0));
    }

    /**
     * Encode an X Y coordinate to its Morton/Z-order index
     *
     * @param x the x value
     * @param y the y value
     * @return the associated Morton index
     */
    public static int encodeMorton(int x, int y) {
        return interleave(x) | (interleave(y) << 1);
    }

    /**
     * 16-bit interleaving
     *
     * @param x the value to interleave
     * @return the interleaved value
     */
    static int interleave(int x) {
        x = (x | (x << 8)) & 0x00FF00FF;
        x = (x | (x << 4)) & 0x0F0F0F0F;
        x = (x | (x << 2)) & 0x33333333;
        x = (x | (x << 1)) & 0x55555555;
        return x;
    }

    /**
     * 16-bit de-interleave
     *
     * @param x the value to de-interleave
     * @return the 16-bit deinterleave
     */
    static int deinterleave(int x) {
        x = x & 0x55555555;
        x = (x | (x >> 1)) & 0x33333333;
        x = (x | (x >> 2)) & 0x0F0F0F0F;
        x = (x | (x >> 4)) & 0x00FF00FF;
        x = (x | (x >> 8)) & 0x0000FFFF;
        return x;
    }

    static int prefixScan(int x) {
        x = (x >> 8) ^ x;
        x = (x >> 4) ^ x;
        x = (x >> 2) ^ x;
        x = (x >> 1) ^ x;
        return x;
    }

    static int descan(int x) {
        return x ^ (x >> 1);
    }

    /**
     * Decode a Morton-encoded index to a packed int (left hand bits are x, right hand bits are x)
     *
     * @param encoded the morton-encoded index
     * @return the x,y coordinate as a packed int
     */
    public static int decodePackedMorton(int encoded) {
        int i = deinterleave(encoded);
        i <<= 16;
        i |= deinterleave(encoded >> 1);
        return i;
    }

    /**
     * Decode a Morton encoded index to an x and y coordinate
     *
     * @param encoded the Morton-encoded index
     * @return a two-element int array containing [0] the x component and [1] the y component
     */
    public static int[] decodeMorton(int encoded) {
        return new int[]{deinterleave(encoded), deinterleave(encoded >> 1)};
    }

}
