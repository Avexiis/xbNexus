package server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public final class API {
    private API() {}
    public static final int[] ConsoleTypeFlags = new int[] { 0x010B0523, 0x010B0524, 0x010C0AD8, 0x010C0AD0, 0x0304000D, 0x0304000E }; //xenon,zephyr,falcon,jasper,trinity,corona
    public static final int[] HardwareFlags = new int[]{ 0x00000207, 0x10000207, 0x20000207, 0x30000207, 0x40000207, 0x50000207 };     //^^ in order left -> right
    public static final byte[][] FuseHashes = new byte[][]{
        new byte[] {
                (byte)0xC0, (byte)0xDC, (byte)0xFE, (byte)0xF3,
                (byte)0xD7,0x3E, (byte)0xED,0x7E,0x5A, (byte)0xF8,
                (byte)0xB1,(byte)0xBB, (byte)0xB2, (byte)0xE0,0x26, (byte)0x95
        },
        new byte[] {
                (byte)0x96,0x23,0x74, (byte)0x9C, (byte)0x9E,
                (byte)0xC5,0x2B,0x30, (byte)0xC6,0x68,0x05, (byte)0x9E,
                (byte)0xAD, (byte)0x9C,0x12, (byte)0xA8
        },
        new byte[] {
                (byte)0x82, (byte)0xC1, (byte)0xF0,0x00, (byte)0x9E,0x79,
                (byte)0x97, (byte)0xF3,0x34,0x0E,0x01,0x45,0x1A, (byte)0xD0,0x32,0x57
        },
        new byte[] {
                0x3A,0x5B,0x47, (byte)0xD6, (byte)0xDD,0x5A, (byte)0xF8,0x66,
                (byte)0x93, (byte)0xED,0x05,0x47,0x25,0x66,0x15,0x69
        },
        new byte[] {
                (byte)0xDB, (byte)0xE6,0x35, (byte)0x87,0x78, (byte)0xCB,
                (byte)0xFC,0x2F,0x52, (byte)0xA3, (byte)0xBA, (byte)0xF8,
                (byte)0x92,0x45, (byte)0x8D,0x65
        },
        new byte[] { (byte)0xD1,0x32, (byte)0xFB,0x43, (byte)0x9B,0x48,0x47,
                (byte)0xE3, (byte)0x9F, (byte)0xE5,0x46,0x46, (byte)0xF0,
                (byte)0xA9, (byte)0x9E, (byte)0xB1
        }
    };
    public static final byte[][] SMCVersions = new byte[][]{
        new byte[] { 0x12, 0x12, 0x01, 0x34, 0x00 },
        new byte[] { 0x12, 0x21, 0x01, 0x09, 0x00 },
        new byte[] { 0x12, 0x31, 0x01, 0x06, 0x00 },
        new byte[] { 0x12, 0x41, 0x02, 0x03, 0x00 },
        new byte[] { 0x12, 0x51, 0x03, 0x01, 0x00 },
        new byte[] { 0x12, 0x62, 0x02, 0x05, 0x00 }
    };
    public static final long[] PCIEHardwareFlags = new long[] {
        0x1158110202000380L, 0x1158110202000380L, 0x2158023102000380L, 0x3158116002000380L, 0x4158016002000380L, 0x4158019002000380L
    };

    public static int ComputeUpdateSequence(byte[] cpuKeyHash) {
        byte[] temp = Arrays.copyOf(cpuKeyHash, 4);
        int be = ((temp[0] & 0xFF) << 24) | ((temp[1] & 0xFF) << 16) | ((temp[2] & 0xFF) << 8) | (temp[3] & 0xFF);
        return 16 | (be & ~0xFF);
    }

    public static int ComputeHVStatusFlags(boolean crl, boolean fcrt) {
        int hvStatusFlags = 0x023289D3;
        if (crl) hvStatusFlags |= 0x10000;
        if (fcrt) hvStatusFlags |= 0x1000000;
        return hvStatusFlags;
    }

    public static byte[] FindSalt(byte[] hvSalt) {
        String hex = Tools.BytesToHexString(hvSalt);
        File f = new File("data/xkec/salts/0x" + hex + ".bin");
        if (!f.exists()) return null;
        try {
            return Files.readAllBytes(f.toPath());
        } catch (IOException e) {
            return null;
        }
    }

    public static byte[] ComputeTitleDigests(byte[] kvDigest, int consoleIndex, byte[] macAddress, byte[] magic, byte[] xamAlloc) {
        try {
            byte[] titleDigest = Arrays.copyOf(kvDigest, kvDigest.length);
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            sha.reset();
            sha.update(Globals.XamHeader);
            sha.update(titleDigest, 0, 0x14);
            sha.update(new byte[0x10]);
            titleDigest = sha.digest();
            sha.reset();
            sha.update(Globals.KernelHeader);
            sha.update(titleDigest, 0, 0x14);
            sha.update(macAddress, 0, 6);
            titleDigest = sha.digest();
            sha.reset();
            sha.update(Globals.TitleHeader);
            sha.update(titleDigest, 0, 0x14);
            sha.update(SMCVersions[consoleIndex], 0, 5);
            titleDigest = sha.digest();
            if (Globals.XOSCHeader != null) {
                if (Globals.XOSCHeader.length >= 0x655D + 0x10) {
                    System.arraycopy(magic, 0, Globals.XOSCHeader, 0x655D, 0x10);
                }
                if (Globals.XOSCHeader.length >= 0x6EC1 + 8) {
                    System.arraycopy(xamAlloc, 0, Globals.XOSCHeader, 0x6EC1, 8);
                }
            }
            sha.reset();
            sha.update(Globals.XOSCHeader);
            sha.update(titleDigest, 0, 0x14);
            titleDigest = sha.digest();
            titleDigest[0] = 7;
            return titleDigest;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}